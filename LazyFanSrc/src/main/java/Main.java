import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

public class Main {
  private static String consumerKey = "j1cAeRMFWAez8FAb5ZJVbZV5S";
  private static String consumerSecret = "dbeocgH55EOETEMIXTZN1oAbtT9mzH4PbT1buViu1PjKvWCSRd";
  private static String accessToken = "752518914391810048-kk8gLnQ8QMC4rN0P2cs756945iMsQfg";
  private static String accessTokenSecret = "0QyxHEqo5nr1XOc8V2Zxl2KyAghuzJABKmPl396M7yPVo";
  private String[] args;
  public Twitter twitter;

  private Main(String[] args) {
    this.args = args;
  }


  public static void main(String[] args) {
    new Main(args).run();
  }

  private void run() {
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
