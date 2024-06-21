package io.icker.factions.util;

import java.util.HashMap;
import java.util.Map;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.Marker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import de.bluecolored.bluemap.api.markers.ExtrudeMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.ClaimEvents;
import io.icker.factions.api.events.FactionEvents;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Home;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;

public class BlueMapWrapper {
    private HashMap<String, MarkerSet> markerSets = new HashMap<>();
    private BlueMapAPI api;
    private boolean loadWhenReady = false;

    public BlueMapWrapper() {
        BlueMapAPI.onEnable((api) -> {
            this.api = api;
            generateMarkers();
        });

        ClaimEvents.ADD.register(this::addClaim);
        ClaimEvents.REMOVE.register(this::removeClaim);

        WorldUtils.ON_READY.register(() -> {
            if (loadWhenReady) {
                loadWhenReady = false;

                generateMarkers();
            }
        });

        FactionEvents.SET_HOME.register(this::setHome);
        FactionEvents.MODIFY.register(faction -> updateFaction(faction));
        FactionEvents.MEMBER_JOIN.register((faction, user) -> updateFaction(faction));
        FactionEvents.MEMBER_LEAVE.register((faction, user) -> updateFaction(faction));
        FactionEvents.POWER_CHANGE.register((faction, oldPower) -> updateFaction(faction));
    }

    private void generateMarkers() {
        if (!WorldUtils.isReady()) {
            loadWhenReady = true;
            FactionsMod.LOGGER.info("Server hasn't loaded, postponing dynmap marker loading");
            return;
        }

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
        MarkerSet markerSet = markerSets.get(claim.level);

        if (markerSet == null) {
            ServerWorld world = WorldUtils.getWorld(claim.level);
            markerSet = new MarkerSet("factions-"+claim.level);

            for (BlueMapMap map : api.getWorld(world).get().getMaps()) {
                map.getMarkerSets().put("factions-"+claim.level, markerSet);
            }

            markerSets.put(claim.level, markerSet);
        }

        ExtrudeMarker marker = ExtrudeMarker.builder()
            .position((double) pos.getCenterX(), 320, (double) pos.getCenterZ())
            .shape(Shape.createRect(pos.getStartX(), pos.getStartZ(), pos.getEndX(), pos.getEndZ()), -64, 320)
            .fillColor(new Color(faction.getColor().getColorValue() | 0x40000000))
            .lineColor(new Color(faction.getColor().getColorValue() | 0xFF000000))
            .label(faction.getName())
            .detail(factionInfo)
            .build();

        String areaMarkerId = String.format("%s-%d-%d", claim.level, claim.x, claim.z);
        markerSet.put(areaMarkerId, marker);
    }

    private void addClaim(Claim claim) {
        addClaim(claim, getInfo(claim.getFaction()));
    }

    private void removeClaim(int x, int z, String level, Faction faction) {
        MarkerSet markerSet = markerSets.get(level);
        if (markerSet != null) {
            String areaMarkerId = String.format("%s-%d-%d", level, x, z);
            markerSet.remove(areaMarkerId);
        }
    }

    private void updateFaction(Faction faction) {
        String info = getInfo(faction);

        for (Claim claim : faction.getClaims()) {
            MarkerSet markerSet = markerSets.get(claim.level);

            if (markerSet == null) {
                continue;
            }

            String areaMarkerId = String.format("%s-%d-%d", claim.level, claim.x, claim.z);
            ExtrudeMarker marker = (ExtrudeMarker) markerSet.get(areaMarkerId);

            marker.setFillColor(new Color(faction.getColor().getColorValue() | 0x40000000));
            marker.setLineColor(new Color(faction.getColor().getColorValue() | 0xFF000000));
            marker.setLabel(faction.getName()); 
            marker.setDetail(info); 
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
            markerSet = new MarkerSet("factions-"+home.level);

            for (BlueMapMap map : api.getWorld(world).get().getMaps()) {
                map.getMarkerSets().put("factions-"+home.level, markerSet);
            }

            markerSets.put(home.level, markerSet);
        }

        for (Map.Entry<String, MarkerSet> entry : markerSets.entrySet()) {
            if (entry.getKey() == home.level) {
                continue;
            }

            entry.getValue().remove(faction.getID().toString() + "-home");
        }

        Marker marker = markerSet.get(faction.getID().toString() + "-home");

        if (marker == null) {
            POIMarker homeMarker = POIMarker.builder().position(home.x, home.y, home.z).detail(getInfo(faction)).label(faction.getName() + "'s Home").build();
            markerSet.put(faction.getID().toString() + "-home", homeMarker);
        } else {
            ((POIMarker) marker).setPosition(home.x, home.y, home.z);
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
