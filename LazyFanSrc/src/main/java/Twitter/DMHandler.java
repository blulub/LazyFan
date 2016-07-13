package Twitter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;

import Constants.Messages;
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

/**
 * Handles registration through DM, runs continuously
 */
public class DMHandler {
  private Twitter twitter;
  private Connection conn;
  private Long lastDMid;

  public DMHandler(Connection conn, Twitter twitter) {
    this.conn = conn;
    this.twitter = twitter;
  }

  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  public void run() {
    final ScheduledFuture<?> handler = scheduler.scheduleWithFixedDelay(
        new Runnable() {
          @Override
          public void run() {
            System.out.println("Starting response to DMs.......");
            System.out.println(getLastDM());
            setTags(getDMsSinceLast());
            System.out.println("Finished responding to DMs.......");
          }
        }, 0, 1, TimeUnit.MINUTES);
  }

  private long getLastDM() {
    try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM lastMessage")) {
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          lastDMid = rs.getLong(1);
        } else {
          lastDMid = null;
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return lastDMid;
  }

  /**
   * Returns a list of all direct messages since the last one we looked at.
   * @return List of DirectMessage since lastDM
   */
  private List<DirectMessage> getDMsSinceLast() {
    lastDMid = getLastDM();

    System.out.println("Starting getDMsSinceLast");
    try {
      if (lastDMid == null) {
        List<DirectMessage> result = twitter.getDirectMessages();
        System.out.println(result.size() + "all messages");
        setLastDM(result);
        return result;
      } else {
        List<DirectMessage> result =  twitter.getDirectMessages(new Paging(lastDMid));
        System.out.println(result.size() + "messages after last");
        setLastDM(result);
        return result;
      }
    } catch (TwitterException e) {
      System.out.println("Twitter services are down, couldn't retrieve DMs");
      return new ArrayList<>();
    }
  }

  // sets the last DM to be the top message returned since
  private void setLastDM(List<DirectMessage> newMessagesSinceLast) {
    if (newMessagesSinceLast.size() > 0) {
      System.out.println("Got messages and setting last message to the last one");
      DirectMessage last = newMessagesSinceLast.get(0);
      try (PreparedStatement prep = conn.prepareStatement("UPDATE lastMessage SET lastID = ? WHERE lastID = ?")) {
        prep.setLong(1, last.getId());
        prep.setLong(2, lastDMid);
      } catch (SQLException e) {
        e.printStackTrace();
        System.out.println("Could not set last DM id in database table lastMessage");
      }
      lastDMid = last.getId();
    }

    System.out.println("new lastDMID is: " + lastDMid);
  }


  private void setTags(List<DirectMessage> messages) {
    String query = "INSERT IGNORE INTO preferences VALUES (?, ?)";
    try (PreparedStatement prep = conn.prepareStatement(query)) {

      for (DirectMessage DM : messages) {
        System.out.println("Found DM: " + DM.getText());
        long senderID = DM.getSenderId();
        if (isResetRequest(DM)) {
          dropUserRecords(DM);
        } else {
          List<String> keywords = parseKeyword(DM);
          if (keywords.isEmpty()) {
            try {
              twitter.sendDirectMessage(senderID, Messages.INVALID_KEYWORDS);
            } catch (TwitterException e) {
              System.out.println("Could not send invalid keyword warning, twitter down");
            }
          } else {
            for (String keyword : keywords) {
              prep.setLong(1, DM.getSenderId());
              prep.setString(2, keyword.toLowerCase());
              prep.addBatch();
            }
            sendSuccess(senderID, keywords);
          }
        }
      }

      prep.executeBatch();
    } catch (SQLException e) {
      e.printStackTrace();
      System.out.println("Could not insert tags into database");
    }
  }

  public void sendSuccess(long senderID, List<String> keywords) {
    StringBuilder message = new StringBuilder(Messages.SUCCESSFUL_SET);
    for (String team : keywords) {
      message.append("\n" + team);
    }
    try {
      twitter.sendDirectMessage(senderID, message.toString());
    } catch (TwitterException e) {
      e.printStackTrace();
      System.out.println("Twitter service down, could not send success message");
    }
  }

  private List<String> parseKeyword(DirectMessage message) {
    String noWhitespace = message.getText().replaceAll(" ", "");
    if (!noWhitespace.contains(",")) {
      return Lists.newArrayList();
    }
    return Lists.newArrayList(noWhitespace.split(","));
  }

  private boolean isResetRequest(DirectMessage message) {
    return message.getText().replaceAll(" ", "").startsWith("RESET");
  }

  private void dropUserRecords(DirectMessage dm) {
    try (PreparedStatement prep = conn.prepareStatement("DELETE FROM preferences WHERE userID = ?")) {
      prep.setLong(1, dm.getSenderId());
      prep.execute();
    } catch (SQLException e) {
      e.printStackTrace();
      System.out.println("Could not delete user records");
    }
  }

}
