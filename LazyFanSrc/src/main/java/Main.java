import java.io.IOException;

import com.google.gson.Gson;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import Constants.Keys;
import Pollers.SeatGeekPoller;
import twitter4j.DirectMessage;
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
//    System.out.println(schedulePoller.doPoll().getDailyEvents());
//    try {
//      String html = Jsoup.connect("http://sports.espn.go.com/mlb/boxscore?gameId=360710126&mlb_s_count=15&mlb_s_loaded=true").get().html();
//      Document doc = Jsoup.parse(html);
//      System.out.println(doc.select("#matchup-mlb-360710126-awayScore").first().text());  // selects away team score
//      System.out.println(doc.select("#matchup-mlb-360710126-homeScore").first().text());  // selects home team score
//    } catch (IOException e) {
//      System.out.println("could not parse");
//    }0

    try {
      Twitter twitterInst = new TwitterFactory().getInstance();
      twitterInst.setOAuthConsumer(consumerKey, consumerSecret);
      AccessToken access = new AccessToken(accessToken, accessTokenSecret);
      twitterInst.setOAuthAccessToken(access);
      twitter = twitterInst;
      for (DirectMessage dm : twitter.getDirectMessages()) {
        System.out.println(dm.getText());
      }

      // every 24 hours get a cached version of the daily schedule
      // go through the schedule and schedule events to take place near the end of games on the schedule
      // the events will keep polling the game and once it gets close, tweet out specifics about the game


    } catch (TwitterException te) {
      te.printStackTrace();
      System.out.println("could not access twitter");
    }
  }
}
