package io.icker.factions.util;

import io.icker.factions.api.events.AddClaimEvent;
import io.icker.factions.api.events.JoinFactionEvent;
import io.icker.factions.api.events.PowerChangeEvent;
import io.icker.factions.api.events.RemoveAllClaimsEvent;
import io.icker.factions.api.events.RemoveClaimEvent;
import io.icker.factions.api.events.RemoveFactionEvent;
import io.icker.factions.api.events.LeaveFactionEvent;
import io.icker.factions.api.events.SetHomeEvent;
import io.icker.factions.api.events.UpdateFactionEvent;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Home;

import org.dynmap.DynmapCommonAPIListener;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;
import org.dynmap.markers.AreaMarker;

import java.util.Collection;
import java.util.List;

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

    RemoveClaimEvent.register(this::removeClaim);
    AddClaimEvent.register(this::addClaim);
    RemoveAllClaimsEvent.register(this::removeAll);
    SetHomeEvent.register(this::setHome);
    RemoveFactionEvent.register((faction -> {
      removeHome(faction);
      removeAll(faction);
    }));

    UpdateFactionEvent.register((faction) -> {
      updateFaction(faction);
    });

    JoinFactionEvent.register(member -> updateFaction(member.getFaction()));
    LeaveFactionEvent.register(member -> updateFaction(member.getFaction()));
    PowerChangeEvent.register(this::updateFaction);
    PowerChangeEvent.register(this::updateFaction);

    //AllyAcceptEvent.register((ally) -> {
    //  updateFaction(Faction.get(ally.source));
    //  updateFaction(Faction.get(ally.target));
    //});
    //AllyRemoveEvent.register((ally) -> {
    //  updateFaction(Faction.get(ally.source));
    //  updateFaction(Faction.get(ally.target));
    //});
  }

  private void generateMarkers() {
    Collection<Faction> factions = Faction.all();
    for (Faction faction : factions) {
      List<Claim> claims = faction.getClaims();

      if (faction.getHome() != null) {
        Home home = faction.getHome();
        String markerId = faction.getName() + "-home";
        markerSet.createMarker(markerId, faction.getName() + "'s Home", dimensionTagToID(home.level), home.x, home.y, home.z, null, true);
      }

      for (Claim claim : claims) {
        ChunkPos pos = new ChunkPos(claim.x, claim.z);

        String areaMarkerId = faction.getName() + "-" + claim.x + claim.z;
        AreaMarker marker = markerSet.createAreaMarker(areaMarkerId, getInfo(faction), true, dimensionTagToID(claim.level), new double[]{pos.getStartX(), pos.getEndX() + 1}, new double[]{pos.getStartZ(), pos.getEndZ() + 1}, true);
        if (marker != null) {
          marker.setFillStyle(marker.getFillOpacity(), faction.getColor().getColorValue());
          marker.setLineStyle(marker.getLineWeight(), marker.getLineOpacity(), faction.getColor().getColorValue());
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
    return "Name: " + faction.getName() + "<br>"
            + "Description: " + faction.getDescription() + "<br>"
            + "Power: " + faction.getPower() + "<br>"
            + "Number of members: " + faction.getMembers().size();// + "<br>"
            //+ "Allies: " + Ally.getAllies(faction.getName).stream().map(ally -> ally.target).collect(Collectors.joining(", "));
  }

  public void addClaim(Claim claim) {
    Faction faction = claim.getFaction();
    ChunkPos pos = new ChunkPos(claim.x, claim.z);

    String areaMarkerId = faction.getName() + "-" + claim.x + claim.z;
    AreaMarker marker = markerSet.createAreaMarker(areaMarkerId, getInfo(faction), true, dimensionTagToID(claim.level), new double[]{pos.getStartX(), pos.getEndX()+1}, new double[]{pos.getStartZ(), pos.getEndZ()+1}, true);
    if (marker != null) {
      marker.setFillStyle(marker.getFillOpacity(), faction.getColor().getColorValue());
      marker.setLineStyle(marker.getLineWeight(), marker.getLineOpacity(), faction.getColor().getColorValue());
    }
  }

  public void removeClaim(Claim claim) {
    Faction faction = claim.getFaction();
    String areaMarkerId = faction.getName() + "-" + claim.x + claim.z;

    AreaMarker marker = markerSet.findAreaMarker(areaMarkerId);
    marker.deleteMarker();
  }

  public void updateFaction(Faction faction) {
    List<Claim> claims = faction.getClaims();

    for (Claim claim : claims) {
      String areaMarkerId = faction.getName() + "-" + claim.x + claim.z;
      AreaMarker marker = markerSet.findAreaMarker(areaMarkerId);

      marker.setFillStyle(marker.getFillOpacity(), faction.getColor().getColorValue());
      marker.setLineStyle(marker.getLineWeight(), marker.getLineOpacity(), faction.getColor().getColorValue());
      marker.setDescription(getInfo(faction));
    }
  }

  public void removeAll(Faction faction) {
    List<Claim> claims = faction.getClaims();

    for (Claim claim : claims) {
      String areaMarkerId = faction.getName() + "-" + claim.x + claim.z;
      markerSet.findAreaMarker(areaMarkerId).deleteMarker();
    }
  }

  public void setHome(Faction faction, Home newHome) {
    String markerId = faction.getName() + "-home";
    if (markerSet.findMarker(markerId) == null) {
      markerSet.createMarker(markerId, faction.getName() + "'s Home", dimensionTagToID(newHome.level), newHome.x, newHome.y, newHome.z, null, true);
    } else {
      markerSet.findMarker(markerId).setLocation(dimensionTagToID(newHome.level), newHome.x, newHome.y, newHome.z);
    }
  }

  public void removeHome(Faction faction) {
    String markerId = faction.getName() + "-home";
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
