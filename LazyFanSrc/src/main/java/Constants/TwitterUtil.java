package Constants;

import java.util.LinkedList;
import java.util.List;

import twitter4j.IDs;
import twitter4j.Twitter;
import twitter4j.TwitterException;

public class TwitterUtil {

  public static List<Long> getFollowersSince(Twitter twitter, long userID, Long lastID) {
    try {
      if (lastID == null) {
        return getAllFollowers(twitter, userID);
      } else {
        List<Long> output = new LinkedList<>();
        IDs first = twitter.getFollowersIDs(userID, -1);
        for (long id : first.getIDs()) {
          if (id != lastID) {
            output.add(id);
          } else {
            return output;
          }
        }
        while (first.hasNext()) {
          first = twitter.getFollowersIDs(userID, first.getNextCursor());
          for (long id : first.getIDs()) {
            if (id != lastID) {
              output.add(id);
            } else {
              return output;
            }
          }
        }
        return output;
      }
    } catch (TwitterException e) {
      e.printStackTrace();
      System.out.println("Twitter exception while getting followers");
      return new LinkedList<>();
    }
  }

  public static List<Long> getAllFollowers(Twitter twitter, long userID) {
    List<Long> output = new LinkedList<>();
    try {
      IDs first = twitter.getFollowersIDs(userID, -1);
      for (long id : first.getIDs()) {
        output.add(id);
      }
      while (first.hasNext()) {
        first = twitter.getFollowersIDs(userID, first.getNextCursor());
        for (long id : first.getIDs()) {
          output.add(id);
        }
      }
    } catch (TwitterException e) {
      e.printStackTrace();
    }
    return output;
  }

  public static String wrapInPercent(String str) {
    return "%" + str.replaceAll(" ", "%") + "%";
  }
}
