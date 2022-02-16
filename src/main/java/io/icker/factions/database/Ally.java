package io.icker.factions.database;

import java.util.ArrayList;
public class Ally {
  public String target;
  public String source;

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
    new Query("DELETE FROM Allies WHERE source = ? AND target = ?;")
        .set(this.source, this.target)
        .executeUpdate();
  }

  public static void remove(String source, String target) {
    new Query("DELETE FROM Allies WHERE source = ? AND target = ?;")
        .set(source, target)
        .executeUpdate();
  }

  public static boolean checkIfAlly(String source, String target) {
    Query query = new Query("SELECT EXISTS(SELECT * FROM Allies WHERE source = ? AND target = ?);")
        .set(source, target)
        .executeQuery();

    return query.exists();
  }

  public static ArrayList<Ally> getAllies(String source) {
    Query query = new Query("SELECT * FROM Allies WHERE source = ?;")
        .set(source)
        .executeQuery();

    ArrayList<Ally> allies = new ArrayList<Ally>();
    if (!query.success)
      return allies;

    while (query.next()) {
      allies.add(new Ally(query.getString("source"), query.getString("target")));
    }
    return allies;
  }
}