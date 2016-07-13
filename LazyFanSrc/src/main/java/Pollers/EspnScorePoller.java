package Pollers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import com.google.common.collect.Lists;

import Constants.Keys;
import Constants.NotificationType;
import Constants.SportType;
import Constants.Times;
import Models.ScoreUpdate;
import Twitter.NotificationHandler;

/**
 * Runs every 3 minutes to sync scores
 */
public class EspnScorePoller implements Poller<List<ScoreUpdate>>, ScheduledExecutorService {
  private Connection conn;
  private NotificationHandler notifier;

  public EspnScorePoller(Connection conn, NotificationHandler not) {
    this.conn = conn;
    this.notifier = not;
  }

  public List<ScoreUpdate> doPoll() {
    List<ScoreUpdate> updates = new ArrayList<>();
    for (SportType type : SportType.values()) {
      updates.addAll(doOneSportPoll(type, type.espnFormat()));
    }
    List<ScoreUpdate> updatesToNotify = filterUpdates(updates);

    // update users on games that are interesting based on their preferences
    for (ScoreUpdate update : updatesToNotify) {
      notifier.updateStatus(update);
      notifier.dmUsersOnGame(update);
    }
    syncScoreUpdates(updates);
    cleanScoreUpdatesTable();
    return updates;
  }

  /**
   * Method that goes through games in the scoreUpdates table and deletes all finished ones
   */
  public void cleanScoreUpdatesTable() {
    String query = "DELETE FROM scoreUpdates WHERE LOWER(updateType)='end'";
    try (PreparedStatement prep = conn.prepareStatement(query)) {
      prep.execute();
    } catch (SQLException e) {
      System.out.println("Could not delete old scoreUpdates from table");
    }
  }

  /**
   * Removes updates where game status hasn't changed (don't want to notify users multiple times of same game)
   * Also removes entries in database where the game has finished
   * @param updates List of updates
   */
  public List<ScoreUpdate> filterUpdates(List<ScoreUpdate> updates) {
    String query = "SELECT updateType from scoreUpdates WHERE id=?";
    List<ScoreUpdate> output = new LinkedList<>();
    for (Iterator<ScoreUpdate> i = updates.iterator(); i.hasNext();) {
      ScoreUpdate update = i.next();
      try (PreparedStatement prep = conn.prepareStatement(query)) {
        prep.setInt(1, update.getId());
        try (ResultSet rs = prep.executeQuery()) {
          if (rs.next()) {
            String updateType = rs.getString(1);
            switch (update.getNotificationType()) {

              // if the game is currently tied, only notify if the previous state was none
              case TIED_GAME:
                if (updateType.equals(NotificationType.NONE.name().toLowerCase())) {
                  output.add(update);
                }
                break;

              // if the game is currently close, only notify if previous state was None
              case CLOSE_GAME:
                if (updateType.equals(NotificationType.NONE.name().toLowerCase())) {
                  output.add(update);
                }
                break;

              // if game is currently overtime, notify everytime unless last time was overtime
              case OVERTIME:
                if (!updateType.equals(NotificationType.OVERTIME.name().toLowerCase())) {
                  output.add(update);
                }
                break;
              default:
                break;
            }
          }
        }
      } catch (SQLException e) {
        System.out.println("Could not access past game state in scoreUpdates table");
      }
    }
    return output;
  }

  public void syncScoreUpdates(List<ScoreUpdate> updates) {
    String query = "INSERT INTO scoreUpdates VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
        "homeScore = ?, awayScore = ?, overtime = ?, currentPeriod = ?, timeLeft = ?, updateType = ?";
    try (PreparedStatement prep = conn.prepareStatement(query)) {

      for (ScoreUpdate update : updates) {
        prep.setInt(1, update.getId());
        prep.setString(2, update.getGameTitle());
        prep.setString(3, update.getHomeName());
        prep.setString(4, update.getAwayName());
        prep.setInt(5, update.getHomeScore());
        prep.setInt(6, update.getAwayScore());
        prep.setBoolean(7, update.isOvertime());
        prep.setString(8, update.getCurrentPeriod());
        prep.setInt(9, update.getTimeLeft());
        prep.setString(10, update.getNotificationType().name());
        prep.setInt(11, update.getHomeScore());
        prep.setInt(12, update.getAwayScore());
        prep.setBoolean(13, update.isOvertime());
        prep.setString(14, update.getCurrentPeriod());
        prep.setInt(15, update.getTimeLeft());
        prep.setString(16, update.getNotificationType().name());
        prep.addBatch();
      }
      prep.executeBatch();
    } catch (SQLException e) {
      System.out.println("Could not update Scores in database");
    }
  }

  /**
   * Polls one sport for live scores
   * @param type the type of the sport
   * @return List of score updates for each game going on right now
   */
  public List<ScoreUpdate> doOneSportPoll(SportType type, String urlForm) {
    try {
      URL url = new URL(String.format(Keys.ESPNFormat, urlForm.toLowerCase()));
      System.out.println(url.toString());
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");
      conn.setRequestProperty("Accept", "application/json");
      if (conn.getResponseCode() != 200) {
        throw new RuntimeException("Invalid response, error code: " + conn.getResponseCode());
      }
      BufferedReader reader =  new BufferedReader(new InputStreamReader(conn.getInputStream()));
      String response = reader.readLine();
      return filterResponse(response, type);
    } catch (MalformedURLException e) {
      System.out.println("Invalid URL for requesting ESPN: " + urlForm);
      e.printStackTrace();
      return new ArrayList<>();
    } catch (IOException e) {
      System.out.println("Could not receive response");
      e.printStackTrace();
      return new ArrayList<>();
    }
  }

  /**
   * Filters given the http response, transforms it into a list of scoreupdates
   * @param response String, response from ESPN
   * @param type  the sport type
   * @return a list of scoreupdates
   */
  public List<ScoreUpdate> filterResponse(String response, SportType type) {
    String splitter = type.name().toLowerCase() + "_s";
    String[] responseArray = response.split(splitter);
    List<ScoreUpdate> output = new ArrayList<>();
    for (String s : responseArray) {
      if (s.startsWith("_left")) {

        String spaced =
            s.replace("(", "")
                .replace(")", "")
                .replace("&", "")
                .replace("=", " ")
                .replaceAll("\\s+", " ");

        List<String> lineList = Lists.newArrayList(spaced.split(" "));
        lineList.remove(0);
        String httpLine = lineList.get(0);
        httpLine = httpLine.replaceAll("%20", " ");
        String[] scoreArray = httpLine.replaceAll("(?<=[a-zA-Z]) +(?=[a-zA-Z])", "-").replaceAll("\\s+", " ").split(" ");
        ScoreUpdate newScoreUpdate = makeScoreUpdate(Lists.newArrayList(scoreArray), type);
        if (newScoreUpdate != null) {
          output.add(newScoreUpdate);
        }
      }
    }
    return output;
  }

  /**
   * Makes a ScoreUpdate object given a line of updates
   * @param scores A broken up string of updates
   * @param type Sport type
   * @return ScoreUpdate object
   */
  public ScoreUpdate makeScoreUpdate(List<String> scores, SportType type) {

    String awayTeam = scores.get(0).replaceAll("-", " ");
    int awayTeamScore = Integer.valueOf(scores.get(1));
    String homeTeam = scores.get(2).replaceAll("-", " ");
    int homeTeamScore = Integer.valueOf(scores.get(3));
    String time = scores.get(4);
    List<String> fullTeamNames = getFullTeamNames(homeTeam, awayTeam);
    homeTeam = fullTeamNames.get(0);
    awayTeam = fullTeamNames.get(1);
    if (time.toLowerCase().contains("end") || time.toLowerCase().contains("final")) {
      time = "0:00";
      int seconds = Times.stringMinutesToIntSeconds(time);
      return new ScoreUpdate.ScoreUpdateBuilder(homeTeam, awayTeam, homeTeamScore, awayTeamScore, "END", seconds)
          .gameTitle(awayTeam + " @ " + homeTeam)
          .sportType(type)
          .overtime(false)
          .build();
    }
    int seconds = Times.stringMinutesToIntSeconds(time);
    String period = scores.get(6);
    int periodNumber = Integer.parseInt(period.replaceAll("\\D", ""));
    return new ScoreUpdate.ScoreUpdateBuilder(homeTeam, awayTeam, homeTeamScore, awayTeamScore, period, seconds)
        .gameTitle(awayTeam + " @ " + homeTeam)
        .sportType(type)
        .overtime(type.isOvertime(periodNumber))
        .build();
  }

  private List<String> getFullTeamNames(String home, String away) {
    List<String> output = new LinkedList<>();
    String homeTeamQuery = "SELECT homeTeamName FROM schedule WHERE LOWER(homeTeamName) LIKE '%?%' LIMIT 1";
    try (PreparedStatement prep = conn.prepareStatement(homeTeamQuery)) {
      prep.setString(1, home.toLowerCase());
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          output.add(rs.getString(1));
        } else {
          output.add(home);
        }
      }
    } catch (SQLException e) {
      output.add(home);
    }

    String awayTeamQuery = "SELECT awayTeamName FROM schedule WHERE LOWER(awayTeamName) LIKE '%?%' LIMIT 1";
    try (PreparedStatement prep = conn.prepareStatement(awayTeamQuery)) {
      prep.setString(1, away.toLowerCase());
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          output.add(rs.getString(1));
        } else {
          output.add(away);
        }
      }
    } catch (SQLException e) {
      output.add(away);
    }

    return output;
  }


}
