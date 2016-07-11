package Models;

import java.util.ArrayList;
import java.util.List;

public class Schedule {
  private List<Event> schedule;

  public Schedule(List<Event> eventList) {
    this.schedule = eventList;
  }

  public List<Event> getLive() {
    List<Event> live = new ArrayList<>();
    for (Event e : schedule) {
      if (e.happeningNow()) {
        live.add(e);
      }
    }
    return live;
  }
}
