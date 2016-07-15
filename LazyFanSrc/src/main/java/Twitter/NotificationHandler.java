package Twitter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import Constants.Messages;
import Constants.Times;
import Models.ScoreUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;

/**
 * Handles status updates and messaging users with game information.
 * Runs every 3 minutes to look for exciting games
 */
public class NotificationHandler {
  private Connection conn;
  private Twitter twitter;

  public NotificationHandler(Connection conn, Twitter twitter) {
    this.conn = conn;
    this.twitter = twitter;
  }

  public void dmUsersOnGame(ScoreUpdate update) {
    Map<Long, String> messageHeaders = getUsersWithInterest(update);
    for (Long userID : messageHeaders.keySet()) {
      sendUserMessage(userID, update);
    }
    updateSent(messageHeaders);
  }

  public void messageDefaults(ScoreUpdate update) {
    Map<Long, String> messageHeaders = getDefaultUsers(update);
    for (Long userID : messageHeaders.keySet()) {
      sendUserMessage(userID, update);
    }
    updateSent(messageHeaders);
  }

  public Map<Long, String> getDefaultUsers(ScoreUpdate update) {
    int scoreDiff = Math.abs(update.getAwayScore() - update.getHomeScore());
    int period = Integer.parseInt(update.getCurrentPeriod().replaceAll("\\D", ""));
    StringBuilder query =
        new StringBuilder("SELECT DISTINCT userID, team FROM preferences WHERE (team = ? ");
    for (String keyword : update.getGameTitle().split(" ")) {
      query.append("OR team=? ");
    }
    for (String keyword : update.getHomeName().split(" ")) {
      query.append("OR team=? ");
    }
    for (String keyword: update.getAwayName().split(" ")) {
      query.append("OR team=? ");
    }
    query.append(") AND scoreDiff == -1 AND timeLeft == -1 AND period == -1 AND alreadySent = false");

    Map<Long, String> output = new HashMap<>();
    int queryIndex = 2;

    try (PreparedStatement prep = conn.prepareStatement(query.toString())) {
      prep.setString(1, update.getGameTitle());
      for (String keyword : update.getGameTitle().split(" ")) {
        prep.setString(queryIndex, keyword);
        queryIndex++;
      }
      for (String keyword : update.getHomeName().split(" ")) {
        prep.setString(queryIndex, keyword);
        queryIndex++;
      }
      for (String keyword: update.getAwayName().split(" ")) {
        prep.setString(queryIndex, keyword);
        queryIndex++;
      }
      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          output.put(rs.getLong(1), rs.getString(2));
        }
      }
    } catch (SQLException e) {
      System.out.println("Could not query database for users with default configs matching teams");
    }
    return output;
  }

  public Map<Long, String> getUsersWithInterest(ScoreUpdate update) {
    int scoreDiff = Math.abs(update.getAwayScore() - update.getHomeScore());
    int period = Integer.parseInt(update.getCurrentPeriod().replaceAll("\\D", ""));
    StringBuilder query =
        new StringBuilder("SELECT DISTINCT userID, team FROM preferences WHERE (team = ? ");
    for (String keyword : update.getGameTitle().split(" ")) {
      query.append("OR team=? ");
    }
    for (String keyword : update.getHomeName().split(" ")) {
      query.append("OR team=? ");
    }
    for (String keyword: update.getAwayName().split(" ")) {
      query.append("OR team=? ");
    }
    query.append(") AND scoreDiff <= ? AND timeLeft <= ? AND period >= ? AND alreadySent = false");

    Map<Long, String> output = new HashMap<>();
    int queryIndex = 2;

    try (PreparedStatement prep = conn.prepareStatement(query.toString())) {
      prep.setString(1, update.getGameTitle());
      for (String keyword : update.getGameTitle().split(" ")) {
        prep.setString(queryIndex, keyword);
        queryIndex++;
      }
      for (String keyword : update.getHomeName().split(" ")) {
        prep.setString(queryIndex, keyword);
        queryIndex++;
      }
      for (String keyword: update.getAwayName().split(" ")) {
        prep.setString(queryIndex, keyword);
        queryIndex++;
      }
      prep.setInt(queryIndex++, scoreDiff);
      prep.setInt(queryIndex++, update.getTimeLeft());
      prep.setInt(queryIndex, period);
      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          output.put(rs.getLong(1), rs.getString(2));
        }
      }
    } catch (SQLException e) {
      System.out.println("Could not query database for users matching teams");
    }
    return output;
  }

  private void updateSent(Map<Long, String> updates) {
    String query = "UPDATE preferences SET alreadySent = true WHERE userID = ? AND team = ?";
    try (PreparedStatement prep = conn.prepareStatement(query)) {
      for (Long key : updates.keySet()) {
        prep.setLong(1, key);
        prep.setString(2, updates.get(key));
        prep.addBatch();
      }
      prep.executeBatch();
    } catch (SQLException e) {
      System.out.println("Could not set update as sent");
    }
  }

  public void updateStatus(ScoreUpdate scoreUpdate) {
    try {
      twitter.updateStatus(formatMessage(scoreUpdate));
    } catch (TwitterException e) {
      System.out.println("Could not update status to say: " + formatMessage(scoreUpdate));
    }
  }

  private String formatMessage(ScoreUpdate scoreUpdate) {
    String message = null;
    switch (scoreUpdate.getNotificationType()) {
      case CLOSE_GAME:
        message = String.format(Messages.CLOSE_GAME_FORMAT,
            scoreUpdate.getAwayName(),
            scoreUpdate.getAwayScore(),
            scoreUpdate.getHomeName(),
            scoreUpdate.getHomeScore(),
            Times.intSecondsToStringMinutes(scoreUpdate.getTimeLeft()),
            scoreUpdate.getCurrentPeriod());
        break;
      case TIED_GAME:
        message = String.format(Messages.TIED_GAME_FORMAT,
            scoreUpdate.getAwayName(),
            scoreUpdate.getHomeName(),
            scoreUpdate.getAwayScore(),
            Times.intSecondsToStringMinutes(scoreUpdate.getTimeLeft()),
            scoreUpdate.getCurrentPeriod());
        break;
      case OVERTIME:
        message = String.format(Messages.OVERTIME,
            scoreUpdate.getCurrentPeriod(),
            scoreUpdate.getAwayName(),
            scoreUpdate.getAwayScore(),
            scoreUpdate.getHomeName(),
            scoreUpdate.getHomeScore());
        break;
      default:
        message = String.format(Messages.OTHER_CONFIG,
            scoreUpdate.getAwayName(),
            scoreUpdate.getAwayScore(),
            scoreUpdate.getHomeName(),
            scoreUpdate.getHomeName(),
            Times.intSecondsToStringMinutes(scoreUpdate.getTimeLeft()),
            scoreUpdate.getCurrentPeriod());
        break;
    }
    return message;
  }

  public void sendUserMessage(long user, ScoreUpdate scoreUpdate) {
    try {
      twitter.sendDirectMessage(user, formatMessage(scoreUpdate));
    } catch (TwitterException e) {
      System.out.println("Could not send DM, twitter service error");
    }
  }

}
