package io.icker.factions.text;

import io.icker.factions.api.persistents.Faction;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.UUID;

public class FactionText extends StyledText {
    private final Faction faction;

    public FactionText(Faction faction) {
        this.faction = faction;
    }

    @Override
    public MutableText build(UUID id) {
        return ((MutableText) Text.of(faction.getColor() + faction.getName()))
                .styled(s -> s.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(faction.getDescription()))))
                .append(new FillerText("»").build(id));
    }
}
