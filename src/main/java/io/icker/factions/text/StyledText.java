package io.icker.factions.text;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;

public abstract class StyledText implements Text {
    protected Style style = Style.EMPTY;

    public MutableText style(MutableText text) {
        return text.setStyle(style);
    };

    public StyledText format(Formatting... format) {
        style = style.withFormatting(format);
        return this;
    }

    public StyledText click(String command) {
        style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        return this;
    }

    public StyledText fail() {
        return format(Formatting.RED);
    }
}
