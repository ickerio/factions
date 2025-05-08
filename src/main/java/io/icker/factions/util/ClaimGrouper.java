package io.icker.factions.util;

import com.flowpowered.math.vector.Vector2i;

import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class ClaimGrouper {
    /**
     * Takes a list of claims from a faction and converts them into just their coordinates,
     * separated by levels. This is the first step to group claims.
     *
     * @param faction The faction whose claims will be sorted
     * @return A map where the keys are the level and the values is a set of 2d integer vectors that
     *     represent the coordinates of a claim
     */
    public static Map<String, Set<Vector2i>> separateClaimsByLevel(Faction faction) {
        Map<String, Set<Vector2i>> claims = new HashMap<>();
        for (Claim claim : faction.getClaims()) {
            Set<Vector2i> level_claim_points = claims.get(claim.level);
            if (level_claim_points != null) {
                level_claim_points.add(new Vector2i(claim.x, claim.z));
            } else {
                level_claim_points = new HashSet<Vector2i>();
                level_claim_points.add(new Vector2i(claim.x, claim.z));
                claims.put(claim.level, level_claim_points);
            }
        }

        return claims;
    }

    /**
     * Takes a list of all claims in a level and groups them using BFS. While doing that it measures
     * all the sides of all the claims that are on a border from claimed territory and wilderness.
     *
     * <p>The returned segments are directional to make it easier to determine if the edge is inside
     * or outside the claimed territory.
     *
     * @param claims A set of vectors that represents the coordinates of a claim (@see
     *     ClaimGrouper#separateClaimsByLevel())
     * @return A list of edges. The edges are returned as a map where the entries represent one
     *     corner of a claim and the values are other corners in connects too. The list has either
     *     one or two elements.
     */
    public static List<Map<Vector2i, Vector2i[]>> convertClaimsToLineSegmentGroups(
            Set<Vector2i> claims) {
        Set<Vector2i> remaining_claims = new HashSet<>(claims);
        Queue<Vector2i> queue = new LinkedList<>();
        List<Map<Vector2i, Vector2i[]>> groups = new ArrayList<>();

        while (!remaining_claims.isEmpty()) {
            Iterator<Vector2i> iter = remaining_claims.iterator();
            Vector2i item = iter.next();
            queue.add(item);
            remaining_claims.remove(item);

            Map<Vector2i, Vector2i[]> lines = new HashMap<>();

            while (!queue.isEmpty()) {
                Vector2i claim = queue.remove();

                for (Vector2i dir :
                        new Vector2i[] {
                            new Vector2i(1, 0),
                            new Vector2i(-1, 0),
                            new Vector2i(0, 1),
                            new Vector2i(0, -1)
                        }) {
                    Vector2i new_claim = claim.add(dir);
                    // The BFS part
                    if (remaining_claims.contains(new_claim)) {
                        queue.add(new_claim);
                        remaining_claims.remove(new_claim);
                    }

                    // The line segment detection part
                    if (!claims.contains(new_claim)) {
                        Vector2i start; // both of these are in block coordinates
                        Vector2i end;

                        if (dir.getX() == 0) {
                            // the y component is being used as the x component so that the
                            // direction is correct
                            start = claim.mul(16).add(new Vector2i(dir.getY(), dir.getY()).mul(8));
                            end = claim.mul(16).add(new Vector2i(-dir.getY(), dir.getY()).mul(8));
                        } else {
                            start = claim.mul(16).add(new Vector2i(dir.getX(), -dir.getX()).mul(8));
                            end = claim.mul(16).add(new Vector2i(dir.getX(), dir.getX()).mul(8));
                        }

                        start =
                                start.add(
                                        new Vector2i(
                                                8,
                                                8)); // offset to match how minecraft converts chunk
                        // coords to block coords
                        end = end.add(new Vector2i(8, 8));

                        if (lines.containsKey(start)) {
                            lines.put(start, new Vector2i[] {lines.get(start)[0], end});
                        } else {
                            lines.put(start, new Vector2i[] {end});
                        }
                    }
                }
            }

            groups.add(lines);
        }

        return groups;
    }

    /**
     * 0 is up, 1 is right, 2 is down, 3 in left These directions are in a standard plane where +x
     * is right and +y is up, I don't think minecraft follows this, but it doesn't matter
     */
    private static int getDir(Vector2i end, Vector2i start) {
        if (end.getY() > start.getY()) {
            return 0;
        } else if (end.getY() < start.getY()) {
            return 2;
        } else if (end.getX() > start.getX()) {
            return 1;
        } else {
            return 3;
        }
    }

    private static boolean isCounterClockwiseTurn(int next, int last) {
        if (next == 0 && last == 3) {
            return false;
        } else if (next == 3 && last == 0) {
            return true;
        } else {
            return next < last;
        }
    }

    /**
     * Takes a group of line segments from one claimed territory and returns a list of outlines
     * where the first one is the outside and the rest of the outlines are cutouts in the shape.
     * This method also combines adjacent line segments that are going in the same direction.
     *
     * @param lines A group of line segments with specific requirements (@see
     *     ClaimGrouper#convertClaimsToLineSegmentGroups())
     * @return a list of outlines where the first one is the outside and the rest are holes.
     */
    public static List<List<Vector2i>> convertLineSegmentsToOutlines(
            Map<Vector2i, Vector2i[]> lines) {
        List<List<Vector2i>> holes = new ArrayList<>();
        List<Vector2i> outline = null;
        while (!lines.isEmpty()) {
            List<Vector2i> line = new ArrayList<>();

            // The total rotations are counted so the
            int rotations = 0;
            int last_dir = -1;

            Vector2i point = lines.values().iterator().next()[0];

            // loop until a point is found where the direction changes
            // If this isn't done, there may be a line with an extra point in the middle where it
            // started
            while (true) {
                Vector2i[] dests = lines.get(point);
                Vector2i new_point = dests[0];

                int dir = ClaimGrouper.getDir(new_point, point);

                if (last_dir != -1 && last_dir != dir) {
                    break;
                }

                point = new_point;
                last_dir = dir;
            }

            while (lines.containsKey(point)) {
                Vector2i[] dests = lines.remove(point);
                Vector2i new_point;
                if (dests.length > 1) {
                    // chose the direction that goes counter clockwise because that's the way the
                    // outer outline goes.
                    int dir0 = getDir(dests[0], point);
                    int dir1 = getDir(dests[1], point);

                    int chosen_idx;

                    if ((dir0 + 2) % 4 == last_dir) {
                        chosen_idx = 1;
                    } else if ((dir1 + 2) % 4 == last_dir) {
                        chosen_idx = 0;
                    } else if (isCounterClockwiseTurn(dir0, last_dir)) {
                        chosen_idx = 0;
                    } else if (isCounterClockwiseTurn(dir1, last_dir)) {
                        chosen_idx = 1;
                    } else if (dir0 == last_dir) {
                        chosen_idx = 0;
                    } else {
                        chosen_idx = 1;
                    }

                    new_point = dests[chosen_idx];
                    lines.put(point, new Vector2i[] {dests[(chosen_idx + 1) % 2]});
                } else {
                    new_point = dests[0];
                }

                int dir = getDir(new_point, point);

                if (dir == 0 && last_dir == 3) {
                    rotations += 1;
                } else if (dir == 3 && last_dir == 0) {
                    rotations -= 1;
                } else {
                    rotations += dir - last_dir;
                }

                // only add this point if there was an actual turn. This also filters 180 degree
                // turns which are created when creating an outline without holes
                if (last_dir % 2 != dir % 2) {
                    line.add(point);
                }

                point = new_point;
                last_dir = dir;
            }

            if (rotations < 0) {
                outline = line;
            } else {
                holes.add(line);
            }
        }

        holes.add(0, outline);

        return holes;
    }

    /**
     * Takes a list of all claims in a level and groups them using BFS. While doing that it measures
     * all the sides of all the claims that are on a border from claimed territory and wilderness.
     *
     * <p>The returned segments are directional.
     *
     * <p>As opposed to @see ClaimGrouper#convertClaimsToLineSegmentGroups(), this function also
     * creates line segments to bridge the gap between the outside outline and any inner holes. This
     * is need by dymap because dynmap doesn't support holes in markers.
     *
     * @param claims A set of vectors that represents the coordinates of a claim (@see
     *     ClaimGrouper#separateClaimsByLevel())
     * @return A list of edges. The edges are returned as a map where the entries represent one
     *     corner of a claim and the values are other corners in connects too. The list has either
     *     one or two elements.
     */
    public static List<Map<Vector2i, Vector2i[]>> convertClaimsToLineSegmentGroupsWithoutHoles(
            Set<Vector2i> claims) {
        Set<Vector2i> remaining_claims = new HashSet<>(claims);
        Queue<Vector2i> queue = new LinkedList<>();
        List<Map<Vector2i, Vector2i[]>> groups = new ArrayList<>();

        while (!remaining_claims.isEmpty()) {
            Iterator<Vector2i> iter = remaining_claims.iterator();
            Vector2i item = iter.next();
            queue.add(item);
            remaining_claims.remove(item);

            Map<Vector2i, Vector2i[]> lines = new HashMap<>();

            while (!queue.isEmpty()) {
                Vector2i claim = queue.remove();

                for (Vector2i dir :
                        new Vector2i[] {
                            new Vector2i(1, 0),
                            new Vector2i(-1, 0),
                            new Vector2i(0, 1),
                            new Vector2i(0, -1)
                        }) {
                    Vector2i new_claim = claim.add(dir);
                    // The BFS part
                    if (remaining_claims.contains(new_claim)) {
                        queue.add(new_claim);
                        remaining_claims.remove(new_claim);
                    } else {
                        Vector2i start; // both of these are in block coordinates
                        Vector2i end;

                        if (dir.getX() == 0) {
                            // the y component is being used as the x component so that the
                            // direction is correct
                            start = claim.mul(16).add(new Vector2i(dir.getY(), dir.getY()).mul(8));
                            end = claim.mul(16).add(new Vector2i(-dir.getY(), dir.getY()).mul(8));
                        } else {
                            start = claim.mul(16).add(new Vector2i(dir.getX(), -dir.getX()).mul(8));
                            end = claim.mul(16).add(new Vector2i(dir.getX(), dir.getX()).mul(8));
                        }

                        start =
                                start.add(
                                        new Vector2i(
                                                8,
                                                8)); // offset to match how minecraft converts chunk
                        // coords to block coords
                        end = end.add(new Vector2i(8, 8));

                        boolean has_opposite_line = false;

                        if (lines.containsKey(end)) {
                            for (Vector2i possible_start : lines.get(end)) {
                                if (possible_start.equals(start)) {
                                    has_opposite_line = true;
                                }
                            }
                        }

                        if (!has_opposite_line
                                && claims.contains(new_claim)
                                && !(queue.contains(new_claim)
                                        || remaining_claims.contains(new_claim))) {
                            continue;
                        }

                        if (lines.containsKey(start)) {
                            lines.put(start, new Vector2i[] {lines.get(start)[0], end});
                        } else {
                            lines.put(start, new Vector2i[] {end});
                        }
                    }
                }
            }

            groups.add(lines);
        }

        return groups;
    }
}
