package Pollers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;

import Constants.Keys;
import Constants.SportType;
import Models.ScoreUpdate;

public class EspnScorePoller implements Poller<List<ScoreUpdate>>{

  public List<ScoreUpdate> doPoll() {
    List<ScoreUpdate> updates = new ArrayList<>();
    for (SportType type : SportType.values()) {
      updates.addAll(doOneSportPoll(type));
    }
    return updates;
  }

  public List<ScoreUpdate> doOneSportPoll(SportType type) {
    try {
      URL url = new URL(String.format(Keys.ESPNFormat, type.name().toLowerCase()));
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");
      conn.setRequestProperty("Accept", "application/json");
      if (conn.getResponseCode() != 200) {
        throw new RuntimeException("Invalid response, error code: " + conn.getResponseCode());
      }
      BufferedReader reader =  new BufferedReader(new InputStreamReader(conn.getInputStream()));
      String response = reader.readLine();
      return filterResponse(response, type);
    } catch (MalformedURLException e) {
      System.out.println("Invalid URL for requesting ESPN: " + type.name());
      e.printStackTrace();
      return new ArrayList<>();
    } catch (IOException e) {
      System.out.println("Could not receive response");
      e.printStackTrace();
      return new ArrayList<>();
    }
  }


  public List<ScoreUpdate> filterResponse(String response, SportType type) {
    String splitter = type.name().toLowerCase() + "_s";
    String[] responseArray = response.split(splitter);
    List<ScoreUpdate> output = new ArrayList<>();
    for (String s : responseArray) {
      if (s.startsWith("_left")) {

        String spaced =
            s.replace("(", "")
                .replace(")", "")
                .replace("&", "")
                .replace("=", " ")
                .replaceAll("\\s+", " ");

        List<String> lineList = Lists.newArrayList(spaced.split(" "));
        lineList.remove(0);
        String httpLine = lineList.get(0);
        httpLine = httpLine.replaceAll("%20", " ");
        String[] scoreArray = httpLine.replaceAll("(?<=[a-zA-Z]) +(?=[a-zA-Z])", "-").replaceAll("\\s+", " ").split(" ");
        System.out.println(Arrays.toString(scoreArray));
        ScoreUpdate newScoreUpdate = makeScoreUpdate(Lists.newArrayList(scoreArray), type);
        if (newScoreUpdate != null) {
          output.add(newScoreUpdate);
        }
      }
    }
    return output;
  }

  public ScoreUpdate makeScoreUpdate(List<String> scores, SportType type) {

    String awayTeam = scores.get(0);
    int awayTeamScore = Integer.valueOf(scores.get(1));
    String homeTeam = scores.get(2).replaceAll("-", " ");
    int homeTeamScore = Integer.valueOf(scores.get(3));
    String time = scores.get(4);
    if (time.toLowerCase().contains("end") || time.toLowerCase().contains("final")) {
      return null;
    }
    String period = scores.get(6);
    int periodNumber = Integer.parseInt(period.replaceAll("\\D", ""));
    return new ScoreUpdate.ScoreUpdateBuilder(homeTeam, awayTeam, homeTeamScore, awayTeamScore, period, time)
        .gameTitle(awayTeam + "@" + homeTeam)
        .sportType(type)
        .overtime(type.isOvertime(periodNumber))
        .build();
  }



}
