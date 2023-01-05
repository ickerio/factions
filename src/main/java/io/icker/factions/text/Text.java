package io.icker.factions.text;

import net.minecraft.text.MutableText;

import java.util.UUID;

public interface Text {
    MutableText build(UUID id);
}
