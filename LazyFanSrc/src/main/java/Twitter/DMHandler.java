package Twitter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import Constants.Messages;
import Constants.Times;
import Constants.TwitterUtil;
import Models.TeamConfiguration;
import twitter4j.DirectMessage;
import twitter4j.Paging;
import twitter4j.Twitter;
import twitter4j.TwitterException;

/**
 * Handles registration through DM, runs continuously
 */
public class DMHandler {
  private Twitter twitter;
  private Connection conn;
  private Long lastDMid;
  private Long lastFollowerID;

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
            for (long newFollower : getFollowersSince()) {
              sendMessage(newFollower, Messages.NEW_FOLLOW, Sets.newHashSet());
            }
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

  private void getLastFollower() {
    try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM lastFollower")) {
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          lastFollowerID = rs.getLong(1);
        } else {
          lastFollowerID = null;
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

  private List<Long> getFollowersSince() {
    getLastFollower();
    try {
      if (lastFollowerID == null) {
        List<Long> result = TwitterUtil.getAllFollowers(twitter, twitter.getId());
        filterFollowersForActivity(result);
        System.out.println("Number of followers: " + result.size());
        setLastFollower(result);
        return result;
      } else {
        List<Long> result =  TwitterUtil.getFollowersSince(twitter, twitter.getId(), lastFollowerID);
        filterFollowersForActivity(result);
        System.out.println("Number of new followers: " + result.size());
        setLastFollower(result);
        return result;
      }
    } catch (TwitterException e) {
      System.out.println("Twitter services are down, couldn't retrieve DMs");
      return new ArrayList<>();
    }
  }

  /**
   * Function that filters followers and removes them from the "to be notified" list if they
   * have already given their preferences.
   * @param followers, a list of longs
   */
  private void filterFollowersForActivity(List<Long> followers) {
    String query = "SELECT DISTINCT userID FROM preferences";
    Set<Long> users = new HashSet<>();
    try (PreparedStatement prep = conn.prepareStatement(query)) {
      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          users.add(rs.getLong(1));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    for (Iterator<Long> i = followers.iterator(); i.hasNext();) {
      long followerID = i.next();
      if (users.contains(followerID)) {
        i.remove();
      }
    }
  }

  private void setLastFollower(List<Long> followers) {
    if (followers.size() > 0) {
      Long last = followers.get(0);
      if (lastFollowerID == null) {
        try (PreparedStatement prep = conn.prepareStatement("INSERT INTO lastFollower VALUE (?)")) {
          prep.setLong(1, last);
          prep.execute();
        } catch (SQLException e) {
          e.printStackTrace();
          System.out.println("Could not insert initial lastMessageID");
        }
      } else {
        try (PreparedStatement prep = conn.prepareStatement("UPDATE lastFollower SET lastID = ? WHERE lastID = ?")) {
          prep.setLong(1, last);
          prep.setLong(2, lastFollowerID);
          prep.execute();
        } catch (SQLException e) {
          e.printStackTrace();
          System.out.println("Could not set last DM id in database table lastMessage");
        }
        lastFollowerID = last;
      }
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
    String query = "INSERT IGNORE INTO preferences VALUES (?, ?, ?, ?, ?, false)";
    try (PreparedStatement prep = conn.prepareStatement(query)) {

      Map<String, Set<Long>> seenUsers = ImmutableMap.of(
          Messages.JOKE_RESPONSE, Sets.newHashSet(),
          Messages.NEW_FOLLOW, Sets.newHashSet(),
          Messages.INVALID_KEYWORDS, Sets.newHashSet());

      for (DirectMessage DM : messages) {
        long senderID = DM.getSenderId();
        if (isResetRequest(DM)) {
          dropUserRecords(DM);
        } else if (isJokeRequest(DM)) {
          sendMessage(DM.getSenderId(), Messages.JOKE_RESPONSE, seenUsers.get(Messages.JOKE_RESPONSE));
        } else if (isHelpRequest(DM)) {
          sendMessage(DM.getSenderId(), Messages.NEW_FOLLOW, seenUsers.get(Messages.NEW_FOLLOW));
        } else {
          AbstractMap.SimpleImmutableEntry<List<TeamConfiguration>, List<String>> parsed = parseMessage(DM);

          // cache the userID so that we don't resend the same error message multiple times
          if (parsed.getKey().isEmpty()) {
            StringBuilder errors = new StringBuilder();
            for (String message : parsed.getValue()) {
              errors.append(message + "\n");
            }
            sendMessage(DM.getSenderId(), Messages.INVALID_KEYWORDS + errors.toString(), seenUsers.get(Messages.INVALID_KEYWORDS));
          } else {
            for (TeamConfiguration config : parsed.getKey()) {
              prep.setLong(1, DM.getSenderId());
              prep.setString(2, config.team.toLowerCase());
              prep.setInt(3, config.scoreDifferential);
              prep.setInt(4, config.secondsleft);
              prep.setInt(5, config.quarter);
              prep.addBatch();
            }
            sendSuccess(senderID, parsed.getKey());
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

  private List<TeamConfiguration> getUserPreferences(Long userID) {
    List<TeamConfiguration> preferences = new LinkedList<>();
    String query = "SELECT * FROM preferences WHERE userID = ?";
    try (PreparedStatement prep = conn.prepareStatement(query)) {
      prep.setLong(1, userID);
      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          long user = rs.getLong(1);
          String team = rs.getString(2);
          int scoreDiff = rs.getInt(3);
          int timeLeft = rs.getInt(4);
          int period = rs.getInt(4);
          preferences.add(new TeamConfiguration(team, scoreDiff, timeLeft, period));
        }
      }
    } catch (SQLException e) {
      System.out.println("Could not access all of " + userID + "'s preferences");
    }
    return preferences;
  }

  private void sendSuccess(long senderID, List<TeamConfiguration> configs) {
    StringBuilder message = new StringBuilder(Messages.SUCCESSFUL_SET);
    List<TeamConfiguration> all = getUserPreferences(senderID);
    all.addAll(configs);
    for (TeamConfiguration team : all) {
      message.append("\n" + team);
    }
    try {
      twitter.sendDirectMessage(senderID, message.toString());
    } catch (TwitterException e) {
      e.printStackTrace();
      System.out.println("Twitter service down, could not send success message");
    }
  }

  private void sendMessage(long senderID, String message, Set<Long> users) {
    try {
      if (!users.contains(senderID)) {
        twitter.sendDirectMessage(senderID, message);
        users.add(senderID);
      }
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

  private AbstractMap.SimpleImmutableEntry<List<TeamConfiguration>, List<String>>
  parseMessage(DirectMessage message) {
    String noWhitespace = message.getText().replaceAll("(?<=>).(?=<)", "");
    List<TeamConfiguration> configs = new LinkedList<>();
    List<String> errors = new LinkedList<>();
    if (!(noWhitespace.contains("<") && noWhitespace.contains(">"))) {
      return new AbstractMap.SimpleImmutableEntry<>(configs, errors);
    }
    String[] tokened = noWhitespace.replaceAll("<", "").split(">");

    // <teamName, scoreDiff, timeLeft, quarter>  // use defaults if anything but teamName isn't provided
    for (String config : tokened) {
      String[] specs = config.split(",");
      if (specs.length != 1 && specs.length != 4) {
        errors.add("<" + config + ">");
      } else {
        if (specs.length == 1) {
          TeamConfiguration newConfig = new TeamConfiguration(specs[0].toLowerCase(), -1, -1, -1);  // add some default
          configs.add(newConfig);
        } else {
          int timeLeft = Times.stringMinutesToIntSeconds(specs[2].replaceAll(" ", ""));
          if (timeLeft == -1) {
            errors.add("<" + config + ">");
            continue;
          }

          try {
            TeamConfiguration newConfig = new TeamConfiguration(
                specs[0].toLowerCase(),
                Integer.valueOf(specs[1].replaceAll(" ", "")),
                timeLeft,
                Integer.parseInt(specs[3].replaceAll("\\D", ""))
            );

            configs.add(newConfig);
          } catch (Exception e) {
            errors.add("<" + config + ">");
          }
        }
      }
    }
    return new AbstractMap.SimpleImmutableEntry<>(configs, errors);
  }

  private boolean isResetRequest(DirectMessage message) {
    return message.getText().replaceAll(" ", "").toLowerCase().startsWith("reset") &&
        !message.getText().contains(",");
  }

  private boolean isJokeRequest(DirectMessage message) {
    return message.getText().toLowerCase().contains("drop") &&
        !message.getText().toLowerCase().contains(",");
  }

  private boolean isHelpRequest(DirectMessage message) {
    return message.getText().toLowerCase().replaceAll("\\s+", "").equals("help");
  }

  private void dropUserRecords(DirectMessage dm) {
    try (PreparedStatement prep = conn.prepareStatement("DELETE FROM preferences WHERE userID = ?")) {
      prep.setLong(1, dm.getSenderId());
      prep.execute();
      System.out.println("Deleted user preferences for user: " + dm.getSenderId());

      sendMessage(dm.getSenderId(), Messages.SUCCESSFUL_RESET, Sets.newHashSet());
    } catch (SQLException e) {
      e.printStackTrace();
      System.out.println("Could not delete user records");
    }
  }

}
