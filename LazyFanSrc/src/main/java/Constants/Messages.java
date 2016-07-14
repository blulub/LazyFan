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

  public static final String INVALID_KEYWORDS = "Could not determine any keywords from your message";
  public static final String SUCCESSFUL_SET = "Success!\nYou will be alerted for: ";
  public static final String SUCCESSFUL_RESET = "You have no more preferences";
  public static final String JOKE_RESPONSE =
  ":::::::::::::/”\\" + "\n" + "::::::::::::|\\:/|\n::::::::::::|:::|\n::::::::::::|:~|\n" +
      "::::::::::::|:::|\n:::::::::/’\\|:::|/’\\::\n:::::/”\\|:::|:::|:::|:\\" +
      "\n::::|:::[#]:::|:::|::\\\n::::|:::|:::|:::|:::|:::\\\n::::|:~:~::~::~:|::::)" +
      "\n::::|:::::::::::::::::::/\n:::::\\:::::::::::::::::/\n::::::\\:::::::::::::::/";

  public static final String NEW_FOLLOW = "Hello!\n" +
      "Please enter your keywords to follow in the form:\n<word>, <word2>\n" +
      "Or please say:\nRESET\nTo clear all preferences";



}
