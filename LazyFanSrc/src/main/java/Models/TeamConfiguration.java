package Models;

import Constants.Times;

public class TeamConfiguration {
  public final String team;
  public final int scoreDifferential;
  public final int secondsleft;
  public final int quarter;
  public final boolean alreadySent;

  public TeamConfiguration(String team, int scoreDifferential, int secondsleft, int quarter) {
    this.team = team;
    this.scoreDifferential = scoreDifferential;
    this.secondsleft = secondsleft;
    this.quarter = quarter;
    this.alreadySent = false;
  }

  @Override
  public String toString() {
    if (scoreDifferential == -1 && secondsleft == -1 && quarter == -1) {
      return team + "\n" +
          "  Default";
    }
    return team + "\n" + "  If score difference <= " + scoreDifferential + "\n" +
        "  If " + Times.intSecondsToStringMinutes(secondsleft) + " left\n" +
        "  If in the " + periodName(quarter) + " period";
  }

  private String periodName(int period) {
    switch (period) {
      case 1:
        return "1st";
      case 2:
        return "2nd";
      case 3:
        return "3rd";
      default:
        return period + "th";
    }
  }
}
