package Constants;

public class Messages {
  public static final String TIED_GAME_FORMAT =
      "Tied game alert!\n%s @ %s tied with %d.\n%s to go in %s";
      // awayName, homeName, score, timeLeft, quarter
  public static final String CLOSE_GAME_FORMAT =
      "Close game alert!\n%s (%d) @ %s (%d).\n%s left in %s";
      // awayName, awayScore, homeName, homeScore, timeLeft, quarter
  public static final String OVERTIME =
          "Overtime number %d! %s (%d) @ %s (%d)";
      // overTimeNumber, awayTeam, homeTeam, awayScore, homeScore
  public static final String OTHER_CONFIG =
          "%s (%d) at %s (%d)\n%s in %s";

  public static final String INVALID_KEYWORDS = "Errors determining teams\n";
  public static final String SUCCESSFUL_SET = "Success!\nYou will be alerted for:\n";
  public static final String SUCCESSFUL_RESET = "You have no more preferences";
  public static final String JOKE_RESPONSE =
  ":::::::::::::/”\\" + "\n" + "::::::::::::|\\:/|\n::::::::::::|:::|\n::::::::::::|:~|\n" +
      "::::::::::::|:::|\n:::::::::/’\\|:::|/’\\::\n:::::/”\\|:::|:::|:::|:\\" +
      "\n::::|:::[#]:::|:::|::\\\n::::|:::|:::|:::|:::|:::\\\n::::|:~:~::~::~:|::::)" +
      "\n::::|:::::::::::::::::::/\n:::::\\:::::::::::::::::/\n::::::\\:::::::::::::::/";

  public static final String NEW_FOLLOW = "Hello!\n" +
      "Please enter your teams in the format:\n\n" +
      "<name, score, time, period><.....>\n\n" +
      "  - name = any keyword\n" +
      "  - score = threshold for alerts\n" +
      "  - time = min:seconds left in a period (3:00)\n" +
      "  - period = period of play (ex: 3)\n\n" +
      "To clear all preferences, send:\n\n" +
      "RESET\n\n";



}
