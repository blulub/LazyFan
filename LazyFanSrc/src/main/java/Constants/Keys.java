package Constants;

// public keys for now, will put in server as variables later
public class Keys {
  public static final String consumerKey = System.getenv("CONSUMER_KEY");
  public static final String consumerSecret = System.getenv("CONSUMER_SECRET");
  public static final String accessToken = System.getenv("ACCESS_TOKEN");
  public static final String accessTokenSecret = System.getenv("ACCESS_TOKEN_SECRET");
  public static final String seatGeakClientId = System.getenv("SEAT_GEEK_CLIENT_ID");
  public static final String ESPNFormat = "http://espn.go.com/%s/bottomline/scores";

  public static final String MYSQL_URL = System.getenv("MYSQL_URL");
  public static final String MYSQL_USERNAME = System.getenv("MYSQL_USERNAME");
  public static final String MYSQL_PASSWORD = System.getenv("MYSQL_PASSWORD");
  public static final String DB_NAME = System.getenv("DB_NAME");

}
