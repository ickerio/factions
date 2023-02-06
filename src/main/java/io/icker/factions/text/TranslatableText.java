package io.icker.factions.text;

import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Translator;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.UUID;

public class TranslatableText extends StyledText {
    private final String key;
    private final Object[] args;

    private String hoverKey = null;

    public TranslatableText(String key, Object... args) {
        this.key = key;
        this.args = args;
    }

    public TranslatableText hover(String key) {
        this.hoverKey = key;
        return this;
    }

    private String translate(String key, UUID receiver, Object[] args) {
        return String.format(Translator.get(key, User.get(receiver).language), args);
    }

    public MutableText build(UUID receiver) {
        return hoverStyle((MutableText) Text.of(translate(key, receiver, args)), receiver);
    }

    public MutableText hoverStyle(MutableText text, UUID receiver) {
        if (hoverKey != null) {
            style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(translate(hoverKey, receiver, new Object[0]))));
        }
        return super.style(text);
    };
}
