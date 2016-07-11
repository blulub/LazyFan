import com.google.gson.Gson;

import Constants.Keys;
import Constants.SportType;
import Pollers.SeatGeekPoller;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

public class Main {
  private static String consumerKey = Keys.consumerKey;
  private static String consumerSecret = Keys.consumerSecret;
  private static String accessToken = Keys.accessToken;
  private static String accessTokenSecret = Keys.accessTokenSecret;

  private String[] args;
  public Twitter twitter;
  private Gson gson = new Gson();
  private SeatGeekPoller schedulePoller = new SeatGeekPoller(gson);

  private Main(String[] args) {
    this.args = args;
  }


  public static void main(String[] args) {
    new Main(args).run();
  }

  private void run() {
    System.out.println(schedulePoller.getAllSportEvents());

    try {
      Twitter twitterInst = new TwitterFactory().getInstance();
      twitterInst.setOAuthConsumer(consumerKey, consumerSecret);
      AccessToken access = new AccessToken(accessToken, accessTokenSecret);
      twitterInst.setOAuthAccessToken(access);
      twitter = twitterInst;
      System.out.println(twitter.getScreenName());
      twitter.updateStatus("test");

      // every 24 hours get a cached version of the daily schedule
      // go through the schedule and schedule events to take place near the end of games on the schedule
      // the events will keep polling the game and once it gets close, tweet out specifics about the game


    } catch (TwitterException te) {
      te.printStackTrace();
      System.out.println("could not access twitter");
    }
  }
}
