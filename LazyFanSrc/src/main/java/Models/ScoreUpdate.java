package Models;

import Constants.NotificationType;
import Constants.SportType;

public class ScoreUpdate {
  private final String gameTitle;
  private final String homeName;
  private final String awayName;
  private final SportType type;
  private int homeScore;
  private int awayScore;
  private boolean overtime;
  private int currentPeriod;
  private String timeLeft;
  private NotificationType notificationType;

  private ScoreUpdate(ScoreUpdateBuilder builder) {
    this.gameTitle = builder.gameTitle;
    this.homeName = builder.homeName;
    this.awayName = builder.awayName;
    this.type = builder.type;
    this.homeScore = builder.homeScore;
    this.awayScore = builder.awayScore;
    this.overtime = builder.overtime;
    this.currentPeriod = builder.currentPeriod;
    this.timeLeft = builder.timeLeft;
  }

  public void update(int homeScore, int awayScore, boolean overtime, int currentPeriod, String timeLeft, NotificationType type) {
    this.homeScore = homeScore;
    this.awayScore = awayScore;
    this.overtime = overtime;
    this.currentPeriod = currentPeriod;
    this.timeLeft = timeLeft;
    this.notificationType = type;
  }

  public NotificationType getNotificationType() {
    return this.notificationType;
  }

  public String getGameTitle() {
    return gameTitle;
  }

  public String getHomeName() {
    return homeName;
  }

  public String getAwayName() {
    return awayName;
  }

  public SportType getType() {
    return type;
  }

  public int getHomeScore() {
    return homeScore;
  }

  public int getAwayScore() {
    return awayScore;
  }

  public boolean isOvertime() {
    return overtime;
  }

  public int getCurrentPeriod() {
    return currentPeriod;
  }

  public String getTimeLeft() {
    return timeLeft;
  }

  public static class ScoreUpdateBuilder {
    private String gameTitle = null;
    private String homeName;
    private String awayName;
    private SportType type = null;
    private int homeScore;
    private int awayScore;
    private boolean overtime = false;
    private int currentPeriod;
    private String timeLeft;

    public ScoreUpdateBuilder(String homeName, String awayName, int homeScore, int awayScore, int currentPeriod, String timeLeft) {
      this.homeName = homeName;
      this.awayName = awayName;
      this.homeScore = homeScore;
      this.awayScore = awayScore;
      this.currentPeriod = currentPeriod;
      this.timeLeft = timeLeft;
    }

    public void gameTitle(String gameTitle) {
      this.gameTitle = gameTitle;
    }

    public void sportType(SportType type) {
      this.type = type;
    }

    public void overtime(boolean overtime) {
      this.overtime = overtime;
    }

    public ScoreUpdate build() {
      return new ScoreUpdate(this);
    }
  }

}
