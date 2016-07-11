package Pollers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import Constants.Keys;
import Constants.SportType;
import Models.SeatGeekEvents;

public class SeatGeekPoller {
  private final String CLIENT_ID = Keys.seatGeakClientId;
  private final String URL = "https://api.seatgeek.com/2/events?per_page=25&taxonomies.name=";
  private final Gson gson;

  public SeatGeekPoller(Gson gson) {
    this.gson = gson;
  }

  public SeatGeekEvents getAllSportEvents() {
    SeatGeekEvents allEvents = new SeatGeekEvents();
    for (SportType sport : SportType.values()) {
      SeatGeekEvents sportEvents = getSportEvents(sport);
      allEvents.join(sportEvents);
    }
    return allEvents;
  }

  public SeatGeekEvents getSportEvents(SportType sport) {
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

}
