package Pollers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.TimerTask;

public class MessageStatusUpdater extends TimerTask {
  private Connection conn;

  public MessageStatusUpdater(Connection conn) {
    this.conn = conn;
  }

  public void run() {
    resetPreferencesToNotSent();
  }

  private void resetPreferencesToNotSent() {
    String query = "UPDATE preferences SET alreadySent = false";
    try (PreparedStatement ps = conn.prepareStatement(query)) {
      ps.execute();
      System.out.println("Set all preferences to alreadySent = false");
    } catch (SQLException e) {
      System.out.println("Could not reset all alreadySent fields to false");
    }
  }


}
