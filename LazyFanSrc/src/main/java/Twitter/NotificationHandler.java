package Twitter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

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
    for (Long userID : getUsersWithInterest(update)) {
      sendUserMessage(userID, update);
    }
  }

  public List<Long> getUsersWithInterest(ScoreUpdate update) {
    StringBuilder query =
        new StringBuilder("SELECT DISTINCT userID FROM preferences WHERE team = ? ");
    for (String keyword : update.getGameTitle().split(" ")) {
      query.append("OR team=? ");
    }
    for (String keyword : update.getHomeName().split(" ")) {
      query.append("OR team=? ");
    }
    for (String keyword: update.getAwayName().split(" ")) {
      query.append("OR team=? ");
    }

    List<Long> users = new LinkedList<>();
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
          users.add(rs.getLong(1));
        }
      }
    } catch (SQLException e) {
      System.out.println("Could not query database for users matching teams");
    }
    return users;
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
