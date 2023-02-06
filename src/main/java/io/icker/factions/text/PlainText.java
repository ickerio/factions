package io.icker.factions.text;

import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.UUID;

public class PlainText extends StyledText {
    private final String text;
    private String hover;

    public PlainText(String text) {
        this.text = text;
    }

    public PlainText hover(String text) {
        this.hover = text;
        return this;
    }

    public MutableText hoverStyle(MutableText text) {
        if (hover != null) {
            style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(hover)));
        }
        return super.style(text);
    };

    @Override
    public MutableText build(UUID id) {
        return hoverStyle((MutableText) Text.of(text));
    }
}
