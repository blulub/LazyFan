package Pollers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import Constants.Keys;
import Constants.SportType;
import Constants.Times;
import Models.Event;
import Models.SeatGeekEvents;

/**
 * Runs every 12 hours to get information on sport schedules
 */
public class SeatGeekPoller {
  private final String CLIENT_ID = Keys.seatGeakClientId;
  private final String URL = "https://api.seatgeek.com/2/events?per_page=25&taxonomies.name=";
  private final Gson gson;
  private Connection conn;

  private SeatGeekEvents cached;

  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);


  public SeatGeekPoller(Connection conn) throws SQLException, ClassNotFoundException{
    this.conn = conn;
    this.gson = new Gson();
  }

  public SeatGeekEvents getCached() {
    return cached;
  }

  public void doPoll() {
    final ScheduledFuture<?> handler = scheduler.scheduleWithFixedDelay(
        new Runnable() {
          @Override
          public void run() {
            System.out.println("Starting poll for sports events");
            SeatGeekEvents allEvents = new SeatGeekEvents();
            for (SportType sport : SportType.values()) {
              SeatGeekEvents sportEvents = getSportEvents(sport);
              allEvents.join(sportEvents);
            }
            cached = allEvents;
            syncSportsEvents(cached);
            cleanSportsEventsTable();
            System.out.println("Finished polling and updating sports events");
          }
        }, 0, 12, TimeUnit.HOURS);
  }

  private void cleanSportsEventsTable() {
    System.out.println("Cleaning sports events data..........");
    String query = "DELETE FROM schedule WHERE DATE_ADD(date, INTERVAL %s HOUR) < NOW() ";
    String deleteStatement = String.format(query, Times.CLEAR_SCHEDULE_INTERVAL);
    try (PreparedStatement prep = conn.prepareStatement(deleteStatement)) {
      prep.execute();
    } catch (SQLException e) {
      System.out.println("Couldn't clear schedule table of old events");
    }
  }

  private SeatGeekEvents getSportEvents(SportType sport) {
    try {
      URL url = new URL(URL + sport.name().toLowerCase() + "&client_id=" + CLIENT_ID);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");
      conn.setRequestProperty("Accept", "application/json");
      if (conn.getResponseCode() != 200) {
        throw new RuntimeException("Invalid response, error code: " + conn.getResponseCode());
      }

      BufferedReader reader =  new BufferedReader(new InputStreamReader(conn.getInputStream()));
      String response = reader.readLine();
      JsonObject json = new JsonParser().parse(response).getAsJsonObject();
      SeatGeekEvents schedule =  gson.fromJson(json, SeatGeekEvents.class);
      // filter out preseason and summer games
      schedule.filterGoodGames();
      return schedule;
    } catch (MalformedURLException e) {
      System.out.println("Invalid URL for requesting SeakGeekEvents");
      e.printStackTrace();
      return null;
    } catch (IOException e) {
      System.out.println("Could not getResponse response");
      e.printStackTrace();
      return null;
    }
  }

  private void syncSportsEvents(SeatGeekEvents events) {
    String query = "INSERT INTO schedule VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE date = ?";
    try (PreparedStatement prep = conn.prepareStatement(query)) {
      for (Event event : events.getEvents()) {
        prep.setLong(1, event.getID());
        prep.setString(2, event.getTitle());
        prep.setString(3, event.getType());
        prep.setString(4, event.getHomeTeam().getName());
        prep.setString(5, event.getAwayTeam().getName());
        prep.setDate(6, Date.valueOf(event.getStart().toLocalDate()));
        prep.setDate(7, Date.valueOf(event.getStart().toLocalDate()));
        prep.addBatch();
      }
      prep.executeBatch();
    } catch (SQLException e) {
      e.printStackTrace();
      System.out.println("Could not reach database to update event schedule");
    }
  }

}
