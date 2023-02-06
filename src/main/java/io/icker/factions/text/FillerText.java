package io.icker.factions.text;

import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;

import java.util.UUID;

public class FillerText extends StyledText {
    private final String symbol;

    public FillerText(String symbol) {
        this.symbol = symbol;
    }

    @Override
    public MutableText build(UUID id) {
        this.format(Formatting.DARK_GRAY);
        this.style = this.style.withHoverEvent(null);
        return style((MutableText) net.minecraft.text.Text.of(" " + symbol + " "));
    }
}
