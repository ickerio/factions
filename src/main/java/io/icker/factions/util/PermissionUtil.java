package io.icker.factions.util;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.Node;
import net.luckperms.api.query.QueryOptions;

import java.util.UUID;

public abstract class PermissionUtil {
    public static int getPermissionPower(UUID uuid){
        int power = 0;
        try {

            var user = LuckPermsProvider.get().getUserManager().getUser(uuid);
            if (user == null) return 0;
            for (Group inheritedGroup : user.getInheritedGroups(QueryOptions.nonContextual())) {
                for (Node node : inheritedGroup.getNodes()) {
                    var perm = node.getKey();
                    if (perm.contains("factions.power.modifier.")) {
                        int value = getNumberFromPermission(node.getKey());
                        power+=value;
                    }
                }
            }
        } catch (Exception ignored) {}
        return power;
    }

    public static int getNumberFromPermission(String permission) {
        try {
            return Integer.parseInt(permission.substring(permission.lastIndexOf(".")+1));
        } catch (Exception ignored) {
            return 0;
        }
    }
}
