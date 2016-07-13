package Models;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

import Constants.SportType;
import Constants.Times;

public class Event {
  private long id;
  private String title;
  private String type;
  private List<Performer> performers;
  private String short_title;
  private String datetime_utc;

  public void update(LocalDateTime time) {
    this.datetime_utc = time.toString();
  }

  public boolean hasEnded() {
    return LocalDateTime.parse(this.datetime_utc, DateTimeFormatter.ISO_DATE_TIME).plusHours(4)
        .isBefore(LocalDateTime.now());
  }

  public long getID() {
    return this.id;
  }

  public String getTitle() {
    return this.title;
  }

  public String getType() {
    return this.type;
  }

  /**
   * Determines if the game is a regular season / post season game.
   * @return true if the game is in regular season or post season. False if all star game or
   *         preseason
   */
  public boolean isRegularGame() {
    return title.toLowerCase().contains(" at ") &&
        !title.toLowerCase().contains("preseason: ") &&
        !title.toLowerCase().contains("summer") &&
        performers.size() == 2;
  }

  public LocalDateTime getStart() {
    return LocalDateTime.parse(datetime_utc, DateTimeFormatter.ISO_DATE_TIME);
  }

  public LocalDateTime getEnd() {
    return LocalDateTime.parse(datetime_utc, DateTimeFormatter.ISO_DATE_TIME).plusHours(Times.GAME_LENGTH);
  }

  public boolean happeningNow() {
    return LocalDateTime.now().isAfter(getStart()) && LocalDateTime.now().isBefore(getEnd());
  }

  public Performer getHomeTeam() {
    if (performers.size() != 2) {

      return null;
    }
    if (performers.get(0).isHomeTeam()) {
      return performers.get(0);
    } else {
      return performers.get(1);
    }
  }

  public Performer getAwayTeam() {
    if (performers.size() != 2) {
      return null;
    }
    if (performers.get(0).equals(getHomeTeam())) {
      return performers.get(1);
    }
    return performers.get(0);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Event event = (Event) o;

    if (id != event.id) return false;
    if (title != null ? !title.equals(event.title) : event.title != null) return false;
    if (type != null ? !type.equals(event.type) : event.type != null) return false;
    if (performers != null ? !performers.equals(event.performers) : event.performers != null)
      return false;
    if (short_title != null ? !short_title.equals(event.short_title) : event.short_title != null)
      return false;
    return datetime_utc != null ? datetime_utc.equals(event.datetime_utc) : event.datetime_utc == null;

  }

  @Override
  public int hashCode() {
    int result = (int) (id ^ (id >>> 32));
    result = 31 * result + (title != null ? title.hashCode() : 0);
    result = 31 * result + (type != null ? type.hashCode() : 0);
    result = 31 * result + (short_title != null ? short_title.hashCode() : 0);
    result = 31 * result + (datetime_utc != null ? datetime_utc.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return getAwayTeam().toString() + " @ " + getHomeTeam().toString() + " on " + datetime_utc;
  }
}
