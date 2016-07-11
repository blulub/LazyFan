package Constants;

public class Messages {
  public static final String TIED_GAME_FORMAT =
      "Tied game alert! %s @ %s tied with %d with %s to go in %s";
      // awayName, homeName, score, timeLeft, quarter
  public static final String CLOSE_GAME_FORMAT =
      "Close game alert! %s with %d @ %s with %d. %s left in %s";
      // awayName, awayScore, homeName, homeScore, timeLeft, quarter
  public static final String OVERTIME =
          "Overtime number %d! %s @ %s, %d to %d";
      // overTimeNumber, awayTeam, homeTeam, awayScore, homeScore
}
