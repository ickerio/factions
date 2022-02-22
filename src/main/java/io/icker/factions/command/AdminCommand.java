package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.icker.factions.database.Query;
import io.icker.factions.util.Message;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import io.icker.factions.database.PlayerConfig;

public class AdminCommand {
  public static int migrateAlly(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayer();

    PlayerConfig config = PlayerConfig.get(player.getUuid());

    if (config.bypass) {
      Query query = new Query("""
          DROP TABLE Allies;

          CREATE TABLE IF NOT EXISTS Allies (
              source VARCHAR(255),
              target VARCHAR(255),
              accept BOOL,
              FOREIGN KEY(source) REFERENCES Faction(name) ON DELETE CASCADE,
              FOREIGN KEY(target) REFERENCES Faction(name) ON DELETE CASCADE
          );
      """);

      query.executeUpdate();

      new Message("Update the database").format(Formatting.YELLOW).send(player, false);
    } else {
      new Message("Please enable admin bypass to execute this command").format(Formatting.RED).send(player, false);
    }

		return 1;
	}
}
