package io.icker.factions.util;

import com.flowpowered.math.vector.Vector2d;
import com.flowpowered.math.vector.Vector2i;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.ExtrudeMarker;
import de.bluecolored.bluemap.api.markers.Marker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.ClaimEvents;
import io.icker.factions.api.events.FactionEvents;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Home;

import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class BlueMapWrapper {
    private HashMap<String, MarkerSet> markerSets = new HashMap<>();
    private BlueMapAPI api;
    private boolean loadWhenReady = false;

    public BlueMapWrapper() {
        BlueMapAPI.onEnable(
                (api) -> {
                    markerSets.clear();
                    this.api = api;
                    generateMarkers();
                });

        ClaimEvents.ADD.register(
                (Claim claim) -> {
                    generateMarkers();
                });
        ClaimEvents.REMOVE.register(
                (x, z, level, faction) -> {
                    generateMarkers();
                });

        WorldUtils.ON_READY.register(
                () -> {
                    if (loadWhenReady) {
                        loadWhenReady = false;

                        generateMarkers();
                    }
                });

        FactionEvents.SET_HOME.register(this::setHome);
        FactionEvents.MODIFY.register(faction -> generateMarkers());
        FactionEvents.MEMBER_JOIN.register((faction, user) -> generateMarkers());
        FactionEvents.MEMBER_LEAVE.register((faction, user) -> generateMarkers());
        FactionEvents.POWER_CHANGE.register((faction, oldPower) -> generateMarkers());
        FactionEvents.DISBAND.register((faction) -> generateMarkers());
    }

    private void generateMarkers() {
        if (!WorldUtils.isReady()) {
            loadWhenReady = true;
            FactionsMod.LOGGER.info("Server hasn't loaded, postponing dynmap marker loading");
            return;
        }

        for (MarkerSet set : markerSets.values()) {
            for (String id : set.getMarkers().keySet()) {
                set.remove(id);
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
                    List<Shape> shapes =
                            outlines.stream()
                                    .map(
                                            (hole) ->
                                                    new Shape(
                                                            hole.stream()
                                                                    .map(
                                                                            (point) ->
                                                                                    new Vector2d(
                                                                                            point
                                                                                                    .getX(),
                                                                                            point
                                                                                                    .getY()))
                                                                    .collect(Collectors.toList())))
                                    .collect(Collectors.toList());

                    MarkerSet markerSet = markerSets.get(level);

                    if (markerSet == null) {
                        ServerWorld world = WorldUtils.getWorld(level);
                        markerSet = new MarkerSet("factions-" + level);

                        for (BlueMapMap map : api.getWorld(world).get().getMaps()) {
                            map.getMarkerSets().put("factions-" + level, markerSet);
                        }

                        markerSets.put(level, markerSet);
                    }

                    ExtrudeMarker marker =
                            ExtrudeMarker.builder()
                                    .position(
                                            (double) outlines.get(0).get(0).getX(),
                                            320,
                                            (double) outlines.get(0).get(0).getY())
                                    .shape(shapes.removeFirst(), -64, 320)
                                    .holes(shapes.toArray(new Shape[0]))
                                    .fillColor(
                                            new Color(
                                                    faction.getColor().getColorValue()
                                                            | 0x40000000))
                                    .lineColor(
                                            new Color(
                                                    faction.getColor().getColorValue()
                                                            | 0xFF000000))
                                    .label(faction.getName())
                                    .detail(info)
                                    .build();

                    markerSet.put(UUID.randomUUID().toString(), marker);
                }
            }
        }
    }

    private void setHome(Faction faction, Home home) {
        if (home == null) {
            for (Map.Entry<String, MarkerSet> entry : markerSets.entrySet()) {
                entry.getValue().remove(faction.getID().toString() + "-home");
            }
            return;
        }

        MarkerSet markerSet = markerSets.get(home.level);

        if (markerSet == null) {
            ServerWorld world = WorldUtils.getWorld(home.level);
            markerSet = new MarkerSet("factions-" + home.level);

            for (BlueMapMap map : api.getWorld(world).get().getMaps()) {
                map.getMarkerSets().put("factions-" + home.level, markerSet);
            }

            markerSets.put(home.level, markerSet);
        }

        for (Map.Entry<String, MarkerSet> entry : markerSets.entrySet()) {
            if (entry.getKey().equals(home.level)) {
                continue;
            }

            entry.getValue().remove(faction.getID().toString() + "-home");
        }

        Marker marker = markerSet.get(faction.getID().toString() + "-home");

        if (marker == null) {
            POIMarker homeMarker =
                    POIMarker.builder()
                            .position(home.x, home.y, home.z)
                            .detail(getInfo(faction))
                            .label(faction.getName() + "'s Home")
                            .build();
            markerSet.put(faction.getID().toString() + "-home", homeMarker);
        } else {
            ((POIMarker) marker).setPosition(home.x, home.y, home.z);
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
