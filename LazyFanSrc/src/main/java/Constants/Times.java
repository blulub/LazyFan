package Constants;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Times {
  public static final int GAME_LENGTH = 4; // hours
  public static final int LIVESCORE_INTERVAL = 3; // minutes
  public static final int SCHEDULE_INTERVAL = 12; // hours
  public static final int CLEAR_SCHEDULE_INTERVAL = 8; // hours

  public static final int INNING_BOTTOM = -2000;
  public static final int INNING_TOP = -1000;

  public static int stringMinutesToIntSeconds(String minutes) {
    SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
    String lower = minutes.toLowerCase().replaceAll(" ", "");
    if (lower.equals("top") || lower.equals("t")) {
      return Times.INNING_TOP;
    } else if (lower.contains("bot") || lower.equals("b")) {
      return Times.INNING_BOTTOM;
    }
    try {
      Date date = sdf.parse(minutes);
      Calendar cal = Calendar.getInstance();
      cal.setTime(date);
      return cal.get(Calendar.MINUTE) * 60 + cal.get(Calendar.SECOND);
    } catch (ParseException e) {
      try {
        return Integer.valueOf(minutes) * 60;
      } catch (Exception p) {
        return -1;
      }
    }
  }

  public static String intSecondsToStringMinutes(int seconds) {
    if (seconds == INNING_TOP) {
      return "TOP";
    }
    if (seconds == INNING_BOTTOM) {
      return "BOTTOM";
    }
    if (seconds < 0) {
      return "0:00";
    }
    String outputFormat = "%d:%02d";
    int min = seconds / 60;
    int sec = seconds - (min * 60);
    return String.format(outputFormat, min, sec);
  }
}
