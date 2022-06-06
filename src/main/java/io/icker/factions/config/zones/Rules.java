package io.icker.factions.config.zones;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

public class Rules {
    @SerializedName("pvp")
    @JsonAdapter(RuleDeserializer.class)
    public Rule PVP;

    @SerializedName("claim")
    @JsonAdapter(RuleDeserializer.class)
    public Rule CLAIM;

    @SerializedName("edit")
    @JsonAdapter(RuleDeserializer.class)
    public Rule EDIT;

    @SerializedName("useItem")
    @JsonAdapter(RuleDeserializer.class)
    public Rule USE_ITEM;

    @SerializedName("mobSpawn")
    @JsonAdapter(RuleDeserializer.class)
    public Rule MOB_SPAWN;

    public static class Rule {
        private String message;

        public Rule(String message) {
            this.message = message;
        }

        public boolean isEnabled() {
            return message == null;
        }

        public String getMessage() {
            return message;
        }
    }

    private static class RuleDeserializer implements JsonDeserializer<Rule> {
        public Rule deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            return new Rule(json.getAsString());
        }
    }
}
