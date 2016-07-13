package Constants;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Times {
  public static final int GAME_LENGTH = 4; // hours
  public static final int LIVESCORE_INTERVAL = 5; // minutes
  public static final int SCHEDULE_INTERVAL = 24; // hours
  public static final int CLEAR_SCHEDULE_INTERVAL = 8; // hours

  public static int stringMinutesToIntSeconds(String minutes) {
    SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
    try {
      Date date = sdf.parse(minutes);
      Calendar cal = Calendar.getInstance();
      cal.setTime(date);
      return cal.get(Calendar.MINUTE) * 60 + cal.get(Calendar.SECOND);
    } catch (ParseException e) {
      return -1;
    }

  }

  public static void main (String[] args) {
    System.out.println(stringMinutesToIntSeconds("05:45"));
    System.out.println(stringMinutesToIntSeconds("5:45"));
  }
}
