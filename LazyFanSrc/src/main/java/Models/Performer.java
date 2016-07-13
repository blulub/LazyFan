package Models;

public class Performer {
  private boolean away_team;
  private String name;
  private String type;
  private String short_name;

  public boolean isHomeTeam() {
    return !away_team;
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public String getShort_name() {
    return short_name;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof Performer) {
      Performer otherPerformer = (Performer) other;
      return (otherPerformer.away_team == this.away_team) &&
          (otherPerformer.name.equals(this.name)) &&
          (otherPerformer.short_name.equals((this.short_name))) &&
          (otherPerformer.type.equals(this.type));
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    int result = (away_team ? 1 : 0);
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (type != null ? type.hashCode() : 0);
    result = 31 * result + (short_name != null ? short_name.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return this.short_name;
  }
}
