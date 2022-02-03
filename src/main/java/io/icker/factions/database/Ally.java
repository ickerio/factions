package io.icker.factions.database;

import java.util.UUID;

public class Ally {
  public String target;
  private String source;

  public static Ally get(String target, String source) {
    Query query = new Query("SELECT * FROM Ally WHERE source = ? AND target = ?;")
        .set(source, target)
        .executeQuery();

    if (!query.success)
      return null;
    return new Ally(query.getString("source"), query.getString("target"));
  }

  public static Ally add(String source, String target) {
    Query query = new Query("INSERT INTO Allies (source, target) VALUES (?, ?);")
        .set(source, target)
        .executeUpdate();

    if (!query.success)
      return null;
    return new Ally(source, target);
  }

  public Ally(String source, String target) {
    this.source = source;
    this.target = target;
  }

  public void remove() {
    Query query = new Query("DELETE FROM Allies WHERE source = ? AND target = ?;")
        .set(this.source, this.target)
        .executeUpdate();
  }
}