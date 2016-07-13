package Models;

import Constants.NotificationType;
import Constants.SportType;

public class ScoreUpdate {
  private String gameTitle;
  private String homeName;
  private String awayName;
  private final SportType type;
  private int homeScore;
  private int awayScore;
  private boolean overtime;
  private String currentPeriod;
  private int timeLeft;
  private NotificationType notificationType;
  private int id;

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
    this.id = builder.gameTitle.hashCode();

    try {
      int period = Integer.valueOf(currentPeriod);
      if (period == type.getLastPeriod()) {
        if (homeScore == awayScore) {
          this.notificationType = NotificationType.TIED_GAME;
        }
        if (Math.abs(homeScore - awayScore) <= type.getScoreThreshold()) {
          this.notificationType = NotificationType.CLOSE_GAME;
        }
      } else if (isOvertime()) {
        this.notificationType = NotificationType.OVERTIME;
      } else {
        this.notificationType = NotificationType.NONE;
      }
    } catch (Exception e) {
      this.notificationType = NotificationType.GAMEOVER;
    }


  }

  public void update(int homeScore, int awayScore, boolean overtime, String currentPeriod, int timeLeft, NotificationType type) {
    this.homeScore = homeScore;
    this.awayScore = awayScore;
    this.overtime = overtime;
    this.currentPeriod = currentPeriod;
    this.timeLeft = timeLeft;
    this.notificationType = type;
  }

  public int getId() {
    return this.id;
  }

  public void setNames(String gametitle, String homeName, String awayName) {
    this.gameTitle = gametitle;
    this.homeName = homeName;
    this.awayName = awayName;
  }

  @Override
  public String toString() {
    return gameTitle + ": " + awayScore + " to " + homeScore + " at " + timeLeft + " in the " + currentPeriod;
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

  public String getCurrentPeriod() {
    return currentPeriod;
  }

  public int getTimeLeft() {
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
    private String currentPeriod;
    private int timeLeft;

    public ScoreUpdateBuilder(String homeName, String awayName, int homeScore, int awayScore, String currentPeriod, int timeLeft) {
      this.homeName = homeName;
      this.awayName = awayName;
      this.homeScore = homeScore;
      this.awayScore = awayScore;
      this.currentPeriod = currentPeriod;
      this.timeLeft = timeLeft;
    }

    public ScoreUpdateBuilder gameTitle(String gameTitle) {
      this.gameTitle = gameTitle; return this;
    }

    public ScoreUpdateBuilder sportType(SportType type) {
      this.type = type; return this;
    }

    public ScoreUpdateBuilder overtime(boolean overtime) {
      this.overtime = overtime; return this;
    }

    public ScoreUpdate build() {
      return new ScoreUpdate(this);
    }
  }

}
