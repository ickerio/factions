package io.icker.factions.util;

import io.icker.factions.database.Home;
import org.dynmap.DynmapCommonAPIListener;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;
import org.dynmap.markers.AreaMarker;

import io.icker.factions.FactionsMod;
import io.icker.factions.database.Ally;

import io.icker.factions.database.Faction;
import io.icker.factions.database.Claim;

import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.util.math.ChunkPos;

public class DynmapWrapper {
  public DynmapCommonAPI dynmapCommonAPI;
  public MarkerAPI markerApi;
  public MarkerSet markerSet;

  public DynmapWrapper() {
    DynmapCommonAPIListener.register(new DynmapCommonAPIListener() {
      @Override
      public void apiEnabled(DynmapCommonAPI dCAPI) {
        dynmapCommonAPI = dCAPI;
        markerApi = dynmapCommonAPI.getMarkerAPI();
        markerSet = markerApi.createMarkerSet("dynmap-factions", "The Dynmap Factions integration", null, true);
        if (markerSet == null) {
          markerSet = markerApi.getMarkerSet("dynmap-factions");
        }
        generateMarkers();
      }
    });
  }

  private void generateMarkers() {
    ArrayList<Faction> factions = Faction.all();
    for (Faction faction : factions) {
      ArrayList<Claim> claims = faction.getClaims();

      if (faction.getHome() != null) {
        FactionsMod.LOGGER.info("home found");
        Home home = faction.getHome();
        String markerId = faction.name + "-home";
        markerSet.createMarker(markerId, faction.name + "'s Home", dimensionTagToID(home.level), home.x, home.y, home.z, null, true);
      }

      for (Claim claim : claims) {
        ChunkPos pos = new ChunkPos(claim.x, claim.z);

        String areaMarkerId = faction.name + "-" + claim.x + claim.z;
        AreaMarker marker = markerSet.createAreaMarker(areaMarkerId, getInfo(faction), true, dimensionTagToID(claim.level), new double[]{pos.getStartX(), pos.getEndX() + 1}, new double[]{pos.getStartZ(), pos.getEndZ() + 1}, true);
        if (marker != null) {
          marker.setFillStyle(marker.getFillOpacity(), faction.color.getColorValue());
          marker.setLineStyle(marker.getLineWeight(), marker.getLineOpacity(), faction.color.getColorValue());
        }
      }
    }
  }

  private String dimensionTagToID(String level) { // FIXME: allow custom dimensions
    if (level.equals("minecraft:overworld")) {
      return "world";
    }
    if (level.equals("minecraft:the_nether")) {
      return "DIM-1";
    }
    if (level.equals("minecraft:the_end")) {
      return "DIM1";
    }
    return level;
  }

  private String getInfo(Faction faction) {
    return "Name: " + faction.name + "<br>"
            + "Description: " + faction.description + "<br>"
            + "Power: " + faction.power + "<br>"
            + "Number of members: " + faction.getMembers().size() + "<br>"
            + "Allies: " + Ally.getAllies(faction.name).stream().map(ally -> ally.target).collect(Collectors.joining(", "));
  }

  public void addClaim(Claim claim) {
    Faction faction = claim.getFaction();
    ChunkPos pos = new ChunkPos(claim.x, claim.z);

    FactionsMod.LOGGER.info(claim.level);

    String areaMarkerId = faction.name + "-" + claim.x + claim.z;
    AreaMarker marker = markerSet.createAreaMarker(areaMarkerId, getInfo(faction), true, dimensionTagToID(claim.level), new double[]{pos.getStartX(), pos.getEndX()+1}, new double[]{pos.getStartZ(), pos.getEndZ()+1}, true);
    if (marker != null) {
      marker.setFillStyle(marker.getFillOpacity(), faction.color.getColorValue());
      marker.setLineStyle(marker.getLineWeight(), marker.getLineOpacity(), faction.color.getColorValue());
    }
  }

  public void removeClaim(Claim claim) {
    Faction faction = claim.getFaction();
    String areaMarkerId = faction.name + "-" + claim.x + claim.z;

    AreaMarker marker = markerSet.findAreaMarker(areaMarkerId);
    marker.deleteMarker();
  }

  public void updateFaction(Faction oldFaction, Faction newFaction) {
    ArrayList<Claim> claims = newFaction.getClaims();

    for (Claim claim : claims) {
      String areaMarkerId = newFaction.name + "-" + claim.x + claim.z;
      AreaMarker marker = markerSet.findAreaMarker(areaMarkerId);

      marker.setFillStyle(marker.getFillOpacity(), newFaction.color.getColorValue());
      marker.setLineStyle(marker.getLineWeight(), marker.getLineOpacity(), newFaction.color.getColorValue());
      marker.setDescription(getInfo(newFaction));
    }
  }

  public void removeAll(Faction faction) {
    ArrayList<Claim> claims = faction.getClaims();

    for (Claim claim : claims) {
      String areaMarkerId = faction.name + "-" + claim.x + claim.z;
      markerSet.findAreaMarker(areaMarkerId).deleteMarker();
    }
  }

  public void setHome(Faction faction, Home newHome) {
    String markerId = faction.name + "-home";
    if (markerSet.findMarker(markerId) == null) {
      markerSet.createMarker(markerId, faction.name + "'s Home", dimensionTagToID(newHome.level), newHome.x, newHome.y, newHome.z, null, true);
    } else {
      markerSet.findMarker(markerId).setLocation(dimensionTagToID(newHome.level), newHome.x, newHome.y, newHome.z);
    }
  }

  public void removeHome(Faction faction) {
    String markerId = faction.name + "-home";
    if (markerSet.findMarker(markerId) != null) {
      markerSet.findMarker(markerId).deleteMarker();
    }
  }

  public void reloadAll() {
    markerSet.deleteMarkerSet();
    markerSet = markerApi.createMarkerSet("dynmap-factions", "The Dynmap Factions integration", null, true);
    generateMarkers();
  }
}
