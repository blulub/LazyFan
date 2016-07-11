package Models;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import Constants.Times;

public class SeatGeekEvents {
  private Set<Event> events = new HashSet<>();

  public Set<Event> getEvents() {
    return events;
  }

  /**
   * Get the events happening now.
   * @return a set of events happening live
   */
  public Set<Event> getLive() {
    Set<Event> live = new HashSet<>();
    for (Event e : events) {
      if (e.happeningNow()) {
        live.add(e);
      }
    }
    return live;
  }

  // delete method to delete from database when events are over

  /**
   * Get all events happening in this sports day (now until 24 hours from now)
   * @return set of events happening this sports day
   */
  public Set<Event> getDailyEvents() {
    Set<Event> output = new HashSet<>();
    for (Event e : events) {
      if (e.getStart().isAfter(LocalDateTime.now()) &&
          e.getStart().isBefore(LocalDateTime.now().plusDays(1))) {
        output.add(e);
      }
    }
    return output;
  }

  public void join(SeatGeekEvents other) {
    this.events.addAll(other.getEvents());
  }

  /**
   * Method that filters out preseason and summer league games
   */
  public void filterGoodGames() {
    for (Iterator<Event> i = events.iterator(); i.hasNext();) {
      Event game = i.next();
      if (!game.isRegularGame()) {
        i.remove();
      }
    }
  }

  /**
   * Removes all games that have finished from the schedules
   */
  public void filterOldGames() {
    for (Iterator<Event> i = events.iterator(); i.hasNext();) {
      Event game = i.next();
      if (game.getStart().plusHours(Times.GAME_LENGTH).isBefore(LocalDateTime.now())) {
        i.remove();
      }
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SeatGeekEvents that = (SeatGeekEvents) o;

    return events != null ? events.equals(that.events) : that.events == null;

  }

  @Override
  public int hashCode() {
    return events != null ? events.hashCode() : 0;
  }

  @Override
  public String toString() {
    StringBuilder output = new StringBuilder("[");
    for (Event e : events) {
      output.append(e.toString() + ", ");
    }
    output.append("]");
    return output.toString();
  }
}
