package io.icker.factions.util;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.ClaimEvents;
import io.icker.factions.api.events.FactionEvents;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Home;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.util.math.ChunkPos;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.Point;
import xyz.jpenilla.squaremap.api.SimpleLayerProvider;
import xyz.jpenilla.squaremap.api.Squaremap;
import xyz.jpenilla.squaremap.api.SquaremapProvider;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.api.marker.Icon;
import xyz.jpenilla.squaremap.api.marker.Marker;
import xyz.jpenilla.squaremap.api.marker.MarkerOptions;

public class SquareMapWrapper {
    private HashMap<String, SimpleLayerProvider> layers = new HashMap<>();
    private Squaremap api;

    public SquareMapWrapper() {
        ClaimEvents.ADD.register(this::addClaim);
        ClaimEvents.REMOVE.register(this::removeClaim);

        ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
            this.api = SquaremapProvider.get();

            generateMarkers();
        });

        FactionEvents.SET_HOME.register(this::setHome);
        FactionEvents.MODIFY.register(faction -> updateFaction(faction));
        FactionEvents.MEMBER_JOIN.register((faction, user) -> updateFaction(faction));
        FactionEvents.MEMBER_LEAVE.register((faction, user) -> updateFaction(faction));
        FactionEvents.POWER_CHANGE.register((faction, oldPower) -> updateFaction(faction));
    }

    private void generateMarkers() {
        for (Faction faction : Faction.all()) {
            Home home = faction.getHome();
            if (home != null) {
                setHome(faction, home);
            }

            String info = getInfo(faction);
            for (Claim claim : faction.getClaims()) {
                addClaim(claim, info);
            }
        }
    }

    private void addClaim(Claim claim, String factionInfo) {
        Faction faction = claim.getFaction();
        ChunkPos pos = new ChunkPos(claim.x, claim.z);
        SimpleLayerProvider layer = layers.get(claim.level);

        if (layer == null) {
            layer = SimpleLayerProvider.builder("factions-"+claim.level).showControls(true).build();

            MapWorld world = api.getWorldIfEnabled(WorldIdentifier.parse(claim.level)).orElse(null);
            if (world != null) {
                world.layerRegistry().register(Key.of("factions-"+claim.level.replace(':', '-')), layer);
            }
        
            layers.put(claim.level, layer);
        }

        Marker marker = Marker.rectangle(Point.of(pos.getStartX(), pos.getStartZ()), Point.of(pos.getEndX(), pos.getEndZ()))
            .markerOptions(
                MarkerOptions.builder()
                    .fillColor(new Color(faction.getColor().getColorValue()))
                    .strokeColor(new Color(faction.getColor().getColorValue()))
                    .hoverTooltip(faction.getName())
                    .clickTooltip(factionInfo)
            );

        String areaMarkerId = String.format("%s-%d-%d", claim.level.replace(':', '-'), claim.x, claim.z);
        layer.addMarker(Key.of(areaMarkerId), marker);
    }

    private void addClaim(Claim claim) {
        addClaim(claim, getInfo(claim.getFaction()));
    }

    private void removeClaim(int x, int z, String level, Faction faction) {
        SimpleLayerProvider layer = layers.get(level);
        if (layer != null) {
            String areaMarkerId = String.format("%s-%d-%d", level.replace(':', '-'), x, z);
            layer.removeMarker(Key.of(areaMarkerId));
        }
    }

    private void updateFaction(Faction faction) {
        String info = getInfo(faction);

        for (Claim claim : faction.getClaims()) {
            SimpleLayerProvider layer = layers.get(claim.level);

            if (layer == null) {
                continue;
            }

            String areaMarkerId = String.format("%s-%d-%d", claim.level.replace(':', '-'), claim.x, claim.z);
            Marker marker = layer.registeredMarkers().get(Key.of(areaMarkerId));

            marker.markerOptions(
                MarkerOptions.builder()
                    .fillColor(new Color(faction.getColor().getColorValue()))
                    .strokeColor(new Color(faction.getColor().getColorValue()))
                    .hoverTooltip(faction.getName())
                    .clickTooltip(info)
            );
        }
    }

    private void setHome(Faction faction, Home home) {
        if (home == null) {
            for (Map.Entry<String, SimpleLayerProvider> entry : layers.entrySet()) {
                entry.getValue().removeMarker(Key.of(faction.getID().toString() + "-home"));
            }
            return;
        }

        SimpleLayerProvider layer = layers.get(home.level);

        if (layer == null) {
            FactionsMod.LOGGER.info("No layer yet");
            layer = SimpleLayerProvider.builder("factions-"+home.level).showControls(true).build();

            MapWorld world = api.getWorldIfEnabled(WorldIdentifier.parse(home.level)).orElse(null);
            if (world != null) {
                world.layerRegistry().register(Key.of("factions-"+home.level.replace(':', '-')), layer);
            } else {
                FactionsMod.LOGGER.info("No world yet");
            }

            FactionsMod.LOGGER.info(api.mapWorlds().size());
        
            layers.put(home.level, layer);
        }

        for (Map.Entry<String, SimpleLayerProvider> entry : layers.entrySet()) {
            if (entry.getKey() == home.level) {
                continue;
            }

            entry.getValue().removeMarker(Key.of(faction.getID().toString() + "-home"));
        }

        Marker marker = layer.registeredMarkers().get(Key.of(faction.getID().toString() + "-home"));

        if (marker == null) {
            Marker homeMarker = Marker.icon(Point.of(home.x, home.z), Key.of("squaremap-spawn_icon"), 16).markerOptions(MarkerOptions.builder().clickTooltip(getInfo(faction)).hoverTooltip(faction.getName() + "'s Home"));
            layer.addMarker(Key.of(faction.getID().toString() + "-home"), homeMarker);
        } else {
            ((Icon) marker).point(Point.of(home.x, home.z));
        }
    }

    private String getInfo(Faction faction) {
        return "Name: " + faction.getName() + "<br>" + "Description: " + faction.getDescription()
                + "<br>" + "Power: " + faction.getPower() + "<br>" + "Number of members: "
                + faction.getUsers().size();// + "<br>"
        // + "Allies: " + Ally.getAllies(faction.getName).stream().map(ally ->
        // ally.target).collect(Collectors.joining(", "));
    }
}
