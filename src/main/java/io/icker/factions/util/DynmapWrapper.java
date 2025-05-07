package io.icker.factions.util;

import com.flowpowered.math.vector.Vector2i;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.ClaimEvents;
import io.icker.factions.api.events.FactionEvents;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Home;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapCommonAPIListener;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.GenericMarker;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;
import org.dynmap.markers.PolyLineMarker;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DynmapWrapper {
    private DynmapCommonAPI api;
    private MarkerAPI markerApi;
    private MarkerSet markerSet;
    private boolean loadWhenReady = false;

    public DynmapWrapper() {
        DynmapCommonAPIListener.register(
                new DynmapCommonAPIListener() {
                    @Override
                    public void apiEnabled(DynmapCommonAPI dCAPI) {
                        api = dCAPI;
                        markerApi = api.getMarkerAPI();
                        markerSet = markerApi.getMarkerSet("dynmap-factions");
                        if (markerSet == null) {
                            markerSet =
                                    markerApi.createMarkerSet(
                                            "dynmap-factions",
                                            "The Dynmap Factions integration",
                                            null,
                                            true);
                        }
                        markerSet.getMarkers().forEach(GenericMarker::deleteMarker);
                        generateMarkers();
                    }
                });

        ClaimEvents.ADD.register(
                (claim) -> {
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

        for (PolyLineMarker marker : markerSet.getPolyLineMarkers()) {
            marker.deleteMarker();
        }

        for (AreaMarker marker : markerSet.getAreaMarkers()) {
            marker.deleteMarker();
        }

        for (Faction faction : Faction.all()) {
            Home home = faction.getHome();
            if (home != null) {
                setHome(faction, home);
            }

            String info = getInfo(faction);
            for (Claim claim : faction.getClaims()) {
                ChunkPos pos = new ChunkPos(claim.x, claim.z);

                AreaMarker marker =
                        markerSet.createAreaMarker(
                                claim.getKey(),
                                info,
                                true,
                                dimensionTagToID(claim.level),
                                new double[] {pos.getStartX(), pos.getEndX() + 1},
                                new double[] {pos.getStartZ(), pos.getEndZ() + 1},
                                true);
                if (marker != null) {
                    marker.setFillStyle(
                            marker.getFillOpacity(), faction.getColor().getColorValue());
                    marker.setLineStyle(0, 0, 0);
                }
            }
            for (Map.Entry<String, Set<Vector2i>> entry :
                    ClaimGrouper.separateClaimsByLevel(faction).entrySet()) {
                String level = entry.getKey();
                for (Map<Vector2i, Vector2i[]> group :
                        ClaimGrouper.convertClaimsToLineSegmentGroupsWithoutHoles(
                                entry.getValue())) {
                    List<List<Vector2i>> outlines =
                            ClaimGrouper.convertLineSegmentsToOutlines(group);
                    if (outlines.size() > 1) {
                        FactionsMod.LOGGER.error(
                                "The claim chunking algorithm used for dynmap has failed, please"
                                    + " report this asap.");
                    }
                    double[] x_coords =
                            outlines.getFirst().stream()
                                    .mapToDouble((point) -> (double) point.getX())
                                    .toArray();
                    double[] z_coords =
                            outlines.getFirst().stream()
                                    .mapToDouble((point) -> (double) point.getY())
                                    .toArray();
                    double[] y_coords =
                            outlines.getFirst().stream().mapToDouble((point) -> 320.0).toArray();

                    PolyLineMarker marker =
                            markerSet.createPolyLineMarker(
                                    UUID.randomUUID().toString(),
                                    "",
                                    false,
                                    dimensionTagToID(level),
                                    x_coords,
                                    y_coords,
                                    z_coords,
                                    true);
                    if (marker != null) {
                        marker.setLineStyle(
                                marker.getLineWeight(),
                                marker.getLineOpacity(),
                                faction.getColor().getColorValue());
                    }
                }
            }
        }
    }

    private void updateFaction(Faction faction) {
        String info = getInfo(faction);

        for (Claim claim : faction.getClaims()) {
            AreaMarker marker = markerSet.findAreaMarker(claim.getKey());

            marker.setFillStyle(marker.getFillOpacity(), faction.getColor().getColorValue());
            marker.setLineStyle(
                    marker.getLineWeight(),
                    marker.getLineOpacity(),
                    faction.getColor().getColorValue());
            marker.setDescription(info);
        }
    }

    private void setHome(Faction faction, Home home) {
        Marker marker = markerSet.findMarker(faction.getID().toString() + "-home");
        if (home == null && marker != null) {
            marker.deleteMarker();
            return;
        }

        if (marker == null) {
            markerSet.createMarker(
                    faction.getID().toString() + "-home",
                    faction.getName() + "'s Home",
                    dimensionTagToID(home.level),
                    home.x,
                    home.y,
                    home.z,
                    null,
                    true);
        } else {
            marker.setLocation(dimensionTagToID(home.level), home.x, home.y, home.z);
        }
    }

    public String getWorldName(World w) { // Taken from the Dynmap mod (Credit to them)
        RegistryKey<World> rk = w.getRegistryKey();
        if (rk == World.OVERWORLD) {
            return w.getServer().getSaveProperties().getLevelName();
        } else if (rk == World.END) {
            return "DIM1";
        } else if (rk == World.NETHER) {
            return "DIM-1";
        } else {
            return rk.getValue().getNamespace() + "_" + rk.getValue().getPath();
        }
    }

    public String dimensionTagToID(String dimension_id) {
        if (!WorldUtils.isReady()) {
            FactionsMod.LOGGER.warn(
                    "Server object has not been initialized please run the dynmap reload command");
            return dimension_id;
        }

        ServerWorld world = WorldUtils.getWorld(dimension_id);

        if (world == null) {
            FactionsMod.LOGGER.error(
                    String.format("Unable to find dimension id: %s", dimension_id));
            return dimension_id;
        }

        return getWorldName(world);
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

    public void reloadAll() {
        markerSet.deleteMarkerSet();
        markerSet =
                markerApi.createMarkerSet(
                        "dynmap-factions", "The Dynmap Factions integration", null, true);
        generateMarkers();
    }
}
