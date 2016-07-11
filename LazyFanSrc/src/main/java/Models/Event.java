package Models;

import java.time.*;

import Constants.SportType;

public class Event {
  private String eventName;
  private String homeTeam;
  private String awayTeam;
  private LocalDateTime start;
  private LocalDateTime end;
  private boolean ended;
  private SportType type;


  public Event(String name, String homeTeam, String awayTeam, LocalDateTime time, SportType type, boolean ended) {
    this.eventName = name;
    this.homeTeam = homeTeam;
    this.awayTeam = awayTeam;
    this.start = time;
    this.end = time.plusHours(4);
    this.type = type;
    this.ended = ended;
  }

  public void update(LocalDateTime time) {
    this.start = time;
  }

  public void update(boolean ended) {
    this.ended = ended;
  }

  public Boolean happeningNow() {
    return LocalDateTime.now().isAfter(start) && LocalDateTime.now().isBefore(end);
  }
}
