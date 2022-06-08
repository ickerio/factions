package io.icker.factions.util;

import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapCommonAPIListener;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

import io.icker.factions.api.events.ClaimEvents;
import io.icker.factions.api.events.FactionEvents;
import io.icker.factions.api.events.HomeEvents;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Home;
import net.minecraft.util.math.ChunkPos;

public class DynmapWrapper {
    private DynmapCommonAPI api;
    private MarkerAPI markerApi;
    private MarkerSet markerSet;

    public DynmapWrapper() {
        DynmapCommonAPIListener.register(new DynmapCommonAPIListener() {
            @Override
            public void apiEnabled(DynmapCommonAPI dCAPI) {
                api = dCAPI;
                markerApi = api.getMarkerAPI();
                markerSet = markerApi.getMarkerSet("dynmap-factions");
                if (markerSet == null) {
                    markerSet = markerApi.createMarkerSet("dynmap-factions", "The Dynmap Factions integration", null, true);
                }
                generateMarkers();
            }
        });

        ClaimEvents.ADD.register(this::addClaim);
        ClaimEvents.REMOVE.register(this::removeClaim);
        HomeEvents.SET.register(this::setHome);

        FactionEvents.MODIFY.register((faction) -> updateFaction(faction));
        FactionEvents.MEMBER_JOIN.register((faction, user) -> updateFaction(faction));
        FactionEvents.MEMBER_LEAVE.register((faction, user) -> updateFaction(faction));
        FactionEvents.POWER_CHANGE.register((faction, oldPower) -> updateFaction(faction));
    }

    private void generateMarkers() {
        for (Faction faction : Faction.all()) {
            Home home = faction.getHome();
            if (home != null) {
                setHome(home);
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

        AreaMarker marker = markerSet.createAreaMarker(
            claim.getKey(), factionInfo, 
            true, dimensionTagToID(claim.level), 
            new double[]{pos.getStartX(), pos.getEndX() + 1}, 
            new double[]{pos.getStartZ(), pos.getEndZ() + 1},
            true
        );
        if (marker != null) {
            marker.setFillStyle(marker.getFillOpacity(), faction.getColor().getColorValue());
            marker.setLineStyle(marker.getLineWeight(), marker.getLineOpacity(), faction.getColor().getColorValue());
        }
    }

    private void addClaim(Claim claim) {
        addClaim(claim, getInfo(claim.getFaction()));
    }

    private void removeClaim(int x, int z, String level, Faction faction) {
        String areaMarkerId = String.format("%s-%d-%d", level, x, z);
        markerSet.findAreaMarker(areaMarkerId).deleteMarker();
    }

    private void updateFaction(Faction faction) {
        String info = getInfo(faction);

        for (Claim claim : faction.getClaims()) {
            AreaMarker marker = markerSet.findAreaMarker(claim.getKey());

            marker.setFillStyle(marker.getFillOpacity(), faction.getColor().getColorValue());
            marker.setLineStyle(marker.getLineWeight(), marker.getLineOpacity(), faction.getColor().getColorValue());
            marker.setDescription(info);
        }
    }

    private void setHome(Home home) {
        Faction faction = home.getFaction();
        Marker marker = markerSet.findMarker(faction.getID().toString() + "-home");
        if (marker == null) {
            markerSet.createMarker("home", faction.getName() + "'s Home", dimensionTagToID(home.level), home.x, home.y, home.z, null, true);
        } else {
            marker.setLocation(dimensionTagToID(home.level), home.x, home.y, home.z);
        }
    }

    private String dimensionTagToID(String level) { // TODO: allow custom dimensions
        if (level.equals("minecraft:overworld")) return "world";
        if (level.equals("minecraft:the_nether")) return "DIM-1";
        if (level.equals("minecraft:the_end")) return "DIM1";
        return level;
    }

    private String getInfo(Faction faction) {
        return "Name: " + faction.getName() + "<br>"
            + "Description: " + faction.getDescription() + "<br>"
            + "Power: " + faction.getPower() + "<br>"
            + "Number of members: " + faction.getUsers().size();// + "<br>"
            //+ "Allies: " + Ally.getAllies(faction.getName).stream().map(ally -> ally.target).collect(Collectors.joining(", "));
    }

    public void reloadAll() {
        markerSet.deleteMarkerSet();
        markerSet = markerApi.createMarkerSet("dynmap-factions", "The Dynmap Factions integration", null, true);
        generateMarkers();
    }
}
