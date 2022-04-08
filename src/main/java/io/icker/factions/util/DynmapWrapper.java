package io.icker.factions.util;

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

        ArrayList<Faction> factions = Faction.all();
        for (int i = 0; i < factions.size(); i++) {
          Faction faction = factions.get(i);
          ArrayList<Claim> claims = faction.getClaims();

          String factionInfo = "Name: " + faction.name + "<br>"
                + "Description: " + faction.description + "<br>"
                + "Power: " + faction.power + "<br>"
                + "Number of members: " + faction.getMembers().size() + "<br>"
                + "Allies: " + Ally.getAllies(faction.name).stream().map(ally -> ally.target).collect(Collectors.joining(", "));

          for (int i2 = 0; i2 < claims.size(); i2++) {
            Claim claim = claims.get(i2);
            ChunkPos pos = new ChunkPos(claim.x, claim.z);

            String areaMarkerId = faction.name + "-" + claim.x + claim.z;
            AreaMarker marker = markerSet.createAreaMarker(areaMarkerId, factionInfo, true, "world", new double[]{pos.getStartX(), pos.getEndX()+1}, new double[]{pos.getStartZ(), pos.getEndZ()+1}, true);
            marker.setFillStyle(marker.getFillOpacity(), faction.color.getColorValue());
            marker.setLineStyle(marker.getLineWeight(), marker.getLineOpacity(), faction.color.getColorValue());
          }
        }
      }
    });
  }
}
