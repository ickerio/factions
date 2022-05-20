package io.icker.factions.database;

@Name("Faction")
public class TestPersistent extends Persistent {
    TestPersistent() { }

    @Field("name")
    String name;

    @Field("number")
    int number;

    public TestPersistent(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getNumber() {
        return number;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getKey() {
        return name;
    }
}
