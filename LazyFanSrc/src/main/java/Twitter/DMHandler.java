package Twitter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.sun.corba.se.impl.protocol.giopmsgheaders.Message;
import com.sun.javafx.event.DirectEvent;

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
            setTags(getDMsSinceLast());
            System.out.println("Finished responding to DMs.......");
          }
        }, 0, 1, TimeUnit.MINUTES);
  }

  private void getLastDM() {
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
  }

  /**
   * Returns a list of all direct messages since the last one we looked at.
   * @return List of DirectMessage since lastDM
   */
  private List<DirectMessage> getDMsSinceLast() {
    getLastDM();
    try {
      if (lastDMid == null) {
        List<DirectMessage> result = twitter.getDirectMessages();
        System.out.println("Number of messages: " + result.size());
        setLastDM(result);
        return result;
      } else {
        List<DirectMessage> result =  twitter.getDirectMessages(new Paging(lastDMid));

        System.out.println("Number of new messages: " + result.size());
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
      DirectMessage last = newMessagesSinceLast.get(0);
      if (lastDMid == null) {
        try (PreparedStatement prep = conn.prepareStatement("INSERT INTO lastMessage VALUE (?)")) {
          prep.setLong(1, last.getId());
          prep.execute();
        } catch (SQLException e) {
          e.printStackTrace();
          System.out.println("Could not insert initial lastMessageID");
        }
      } else {
        try (PreparedStatement prep = conn.prepareStatement("UPDATE lastMessage SET lastID = ? WHERE lastID = ?")) {
          prep.setLong(1, last.getId());
          prep.setLong(2, lastDMid);
          prep.execute();
        } catch (SQLException e) {
          e.printStackTrace();
          System.out.println("Could not set last DM id in database table lastMessage");
        }
        lastDMid = last.getId();
      }
    }
  }


  private void setTags(List<DirectMessage> messages) {
    String query = "INSERT IGNORE INTO preferences VALUES (?, ?)";
    try (PreparedStatement prep = conn.prepareStatement(query)) {

      Set<Long> badUsers = new HashSet<>();

      for (DirectMessage DM : messages) {
        long senderID = DM.getSenderId();
        if (isResetRequest(DM)) {
          dropUserRecords(DM);
        } else if (isJokeRequest(DM)) {
          sendMessage(DM.getSenderId(), Messages.JOKE_RESPONSE);
        } else {
          List<String> keywords = parseKeyword(DM);
          if (keywords.isEmpty() && !badUsers.contains(DM.getId())) {
            try {
              twitter.sendDirectMessage(senderID, Messages.INVALID_KEYWORDS);
              badUsers.add(DM.getId());
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
        destroyDM(DM.getId());
      }

      prep.executeBatch();
    } catch (SQLException e) {
      e.printStackTrace();
      System.out.println("Could not insert tags into database");
    }
  }

  private void destroyDM(long id) {
    try {
      twitter.destroyDirectMessage(id);
    } catch (TwitterException e) {
      System.out.println("Could not destroy already seen DM");
      e.printStackTrace();
    }
  }

  private void sendSuccess(long senderID, List<String> keywords) {
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

  private void sendMessage(long senderID, String message) {
    try {
      twitter.sendDirectMessage(senderID, message);
    } catch (TwitterException e) {
      e.printStackTrace();
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

  private boolean isJokeRequest(DirectMessage message) {
    return message.getText().toLowerCase().contains("drop") &&
        !message.getText().toLowerCase().contains(",");
  }

  private void dropUserRecords(DirectMessage dm) {
    try (PreparedStatement prep = conn.prepareStatement("DELETE FROM preferences WHERE userID = ?")) {
      prep.setLong(1, dm.getSenderId());
      prep.execute();
      System.out.println("Deleted user preferences for user: " + dm.getSenderId());

      sendMessage(dm.getSenderId(), Messages.SUCCESSFUL_RESET);
    } catch (SQLException e) {
      e.printStackTrace();
      System.out.println("Could not delete user records");
    }
  }

}
