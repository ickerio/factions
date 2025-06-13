package io.icker.factions.util;

import com.flowpowered.math.vector.Vector2i;

import io.icker.factions.api.events.ClaimEvents;
import io.icker.factions.api.events.FactionEvents;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Home;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

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

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class SquareMapWrapper {
    private HashMap<String, SimpleLayerProvider> layers = new HashMap<>();
    private Squaremap api;

    public SquareMapWrapper() {
        ClaimEvents.ADD.register(
                (Claim claim) -> {
                    generateMarkers();
                });
        ClaimEvents.REMOVE.register(
                (x, z, level, faction) -> {
                    generateMarkers();
                });

        ServerLifecycleEvents.SERVER_STARTED.register(
                (server) -> {
                    this.api = SquaremapProvider.get();

                    generateMarkers();
                });

        FactionEvents.SET_HOME.register(this::setHome);
        FactionEvents.MODIFY.register(faction -> generateMarkers());
        FactionEvents.MEMBER_JOIN.register((faction, user) -> generateMarkers());
        FactionEvents.MEMBER_LEAVE.register((faction, user) -> generateMarkers());
        FactionEvents.POWER_CHANGE.register((faction, oldPower) -> generateMarkers());
        FactionEvents.DISBAND.register((faction) -> generateMarkers());
    }

    private void generateMarkers() {
        for (SimpleLayerProvider layer : layers.values()) {
            for (Key id : layer.registeredMarkers().keySet()) {
                layer.removeMarker(id);
            }
        }

        for (Faction faction : Faction.all()) {
            Home home = faction.getHome();
            if (home != null) {
                setHome(faction, home);
            }

            String info = getInfo(faction);
            for (Map.Entry<String, Set<Vector2i>> entry :
                    ClaimGrouper.separateClaimsByLevel(faction).entrySet()) {
                String level = entry.getKey();
                for (Map<Vector2i, Vector2i[]> group :
                        ClaimGrouper.convertClaimsToLineSegmentGroups(entry.getValue())) {
                    List<List<Vector2i>> outlines =
                            ClaimGrouper.convertLineSegmentsToOutlines(group);
                    List<List<Point>> points =
                            outlines.stream()
                                    .map(
                                            (hole) ->
                                                    hole.stream()
                                                            .map(
                                                                    (point) ->
                                                                            Point.of(
                                                                                    point.getX(),
                                                                                    point.getY()))
                                                            .collect(Collectors.toList()))
                                    .collect(Collectors.toList());

                    SimpleLayerProvider layer = layers.get(level);
                    if (layer == null) {
                        layer =
                                SimpleLayerProvider.builder("factions-" + level)
                                        .showControls(true)
                                        .build();

                        MapWorld world =
                                api.getWorldIfEnabled(WorldIdentifier.parse(level)).orElse(null);
                        if (world != null) {
                            world.layerRegistry()
                                    .register(Key.of("factions-" + level.replace(':', '-')), layer);
                        }

                        layers.put(level, layer);
                    }

                    Marker marker =
                            Marker.polygon(points.removeFirst(), points)
                                    .markerOptions(
                                            MarkerOptions.builder()
                                                    .fillColor(
                                                            new Color(
                                                                    faction.getColor()
                                                                            .getColorValue()))
                                                    .strokeColor(
                                                            new Color(
                                                                    faction.getColor()
                                                                            .getColorValue()))
                                                    .hoverTooltip(faction.getName())
                                                    .clickTooltip(info));

                    layer.addMarker(Key.of(UUID.randomUUID().toString()), marker);
                }
            }
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
            layer =
                    SimpleLayerProvider.builder("factions-" + home.level)
                            .showControls(true)
                            .build();

            MapWorld world = api.getWorldIfEnabled(WorldIdentifier.parse(home.level)).orElse(null);
            if (world != null) {
                world.layerRegistry()
                        .register(Key.of("factions-" + home.level.replace(':', '-')), layer);
            } else {
            }

            layers.put(home.level, layer);
        }

        for (Map.Entry<String, SimpleLayerProvider> entry : layers.entrySet()) {
            if (entry.getKey().equals(home.level)) {
                continue;
            }

            entry.getValue().removeMarker(Key.of(faction.getID().toString() + "-home"));
        }

        Marker marker = layer.registeredMarkers().get(Key.of(faction.getID().toString() + "-home"));

        if (marker == null) {
            Marker homeMarker =
                    Marker.icon(Point.of(home.x, home.z), Key.of("squaremap-spawn_icon"), 16)
                            .markerOptions(
                                    MarkerOptions.builder()
                                            .clickTooltip(getInfo(faction))
                                            .hoverTooltip(faction.getName() + "'s Home"));
            layer.addMarker(Key.of(faction.getID().toString() + "-home"), homeMarker);
        } else {
            ((Icon) marker).point(Point.of(home.x, home.z));
        }
    }

    private String getInfo(Faction faction) {
        return "Name: "
                + faction.getName()
                + "<br>"
                + "Description: "
                + faction.getDescription()
                + "<br>"
                + "Power: "
                + faction.getPower()
                + "<br>"
                + "Number of members: "
                + faction.getUsers().size(); // + "<br>"
        // + "Allies: " + Ally.getAllies(faction.getName).stream().map(ally ->
        // ally.target).collect(Collectors.joining(", "));
    }
}
