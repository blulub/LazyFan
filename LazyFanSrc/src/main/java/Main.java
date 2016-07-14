import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;


import Constants.Keys;
import Pollers.EspnScorePoller;
import Pollers.MessageStatusUpdater;
import Pollers.SeatGeekPoller;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

import Twitter.NotificationHandler;
import Twitter.DMHandler;

public class Main {
  private String[] args;
  public Twitter twitter;
  private SeatGeekPoller schedulePoller = null;
  private EspnScorePoller scorePoller = null;
  private DMHandler dmHandler = null;
  private MessageStatusUpdater statusUpdater = null;
  private NotificationHandler notifier = null;
  private Connection conn;

  private Main(String[] args) {
    this.args = args;
    Twitter twitterInst = new TwitterFactory().getInstance();
    twitterInst.setOAuthConsumer(Keys.consumerKey, Keys.consumerSecret);
    AccessToken access = new AccessToken(Keys.accessToken, Keys.accessTokenSecret);
    twitterInst.setOAuthAccessToken(access);
    this.twitter = twitterInst;

    try {
      String driver = "com.mysql.jdbc.Driver";
      Class.forName(driver);
      this.conn =
          DriverManager.getConnection(
              Keys.MYSQL_URL + Keys.DB_NAME,
              Keys.MYSQL_USERNAME,
              Keys.MYSQL_PASSWORD);
      this.schedulePoller = new SeatGeekPoller(this.conn);
      this.notifier = new NotificationHandler(this.conn, this.twitter);
      this.dmHandler = new DMHandler(this.conn, this.twitter);
      this.scorePoller = new EspnScorePoller(this.conn, this.notifier);
      this.statusUpdater = new MessageStatusUpdater(this.conn);
    } catch (SQLException e) {
      e.printStackTrace();
      System.out.println("could not instantiate database connection");
    } catch (ClassNotFoundException e) {
      System.out.println("Could not find db driver class");
    }
  }


  public static void main(String[] args) {
    new Main(args).run();
  }


  private void run() {
    if (conn == null) {
      System.out.println("Could not run bot, connection not made to database");
      return;
    }

    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.HOUR_OF_DAY, 4);
    cal.set(Calendar.MINUTE, 37);
    Date time = cal.getTime();

    Timer timer = new Timer();
    timer.schedule(new MessageStatusUpdater(this.conn), time, TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS));

    this.schedulePoller.doPoll();
    this.dmHandler.run();
    this.scorePoller.doPoll();

  }
}
