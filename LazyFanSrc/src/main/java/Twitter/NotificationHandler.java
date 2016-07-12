package Twitter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import Constants.Messages;
import Constants.NotificationType;
import Models.Event;
import Models.ScoreUpdate;
import twitter4j.DirectMessage;
import twitter4j.Paging;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

import static Constants.Keys.accessToken;
import static Constants.Keys.accessTokenSecret;
import static Constants.Keys.consumerKey;
import static Constants.Keys.consumerSecret;

public class NotificationHandler {
  private Connection conn;
  private Twitter twitter;

  public NotificationHandler() throws SQLException, ClassNotFoundException{
    String url = "";
    String username = "";
    String password = "";
    String dbName = "";
    String driver = "";
    Class.forName(driver);
    conn = DriverManager.getConnection(url + dbName, username, password);

    Twitter twitterInst = new TwitterFactory().getInstance();
    twitterInst.setOAuthConsumer(consumerKey, consumerSecret);
    AccessToken access = new AccessToken(accessToken, accessTokenSecret);
    twitterInst.setOAuthAccessToken(access);
    twitter = twitterInst;
  }

  private List<Long> getUsersWithKeyword(String keyword) {
    String query = "SELECT DISTINCT userID FROM preferences WHERE team = ?";
    List<Long> users = new LinkedList<>();
    try (PreparedStatement prep = conn.prepareStatement(query)) {
      prep.setString(1, keyword);
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

  private void updateStatus(ScoreUpdate scoreUpdate) {
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
            scoreUpdate.getTimeLeft(),
            scoreUpdate.getCurrentPeriod());  // awayName, awayScore, homeName, homeScore, timeLeft, quarter
        break;
      case TIED_GAME:
        message = String.format(Messages.TIED_GAME_FORMAT,
            scoreUpdate.getAwayName(),
            scoreUpdate.getHomeName(),
            scoreUpdate.getAwayScore(),
            scoreUpdate.getTimeLeft(),
            scoreUpdate.getCurrentPeriod());
        break;
      case OVERTIME:
        message = String.format(Messages.OVERTIME,
            scoreUpdate.getCurrentPeriod(),
            scoreUpdate.getAwayName(),
            scoreUpdate.getHomeName(),
            scoreUpdate.getAwayScore(),
            scoreUpdate.getHomeScore());
        break;
    }
    return message;
  }

  private void sendUserMessage(long user, ScoreUpdate scoreUpdate) {
    try {
      twitter.sendDirectMessage(user, formatMessage(scoreUpdate));
    } catch (TwitterException e) {
      System.out.println("Could not send DM, twitter service error");
    }
  }
}
