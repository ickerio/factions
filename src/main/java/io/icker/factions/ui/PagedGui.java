package io.icker.factions.ui;

import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementBuilderInterface;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.SimpleGui;

import io.icker.factions.util.GuiInteract;
import io.icker.factions.util.Icons;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

// Shamelessly stolen from Get off My Lawn https://github.com/Patbox/get-off-my-lawn-reserved
@ApiStatus.Internal
public abstract class PagedGui extends SimpleGui {
    public static final int PAGE_SIZE = 9 * 4;
    protected final Runnable closeCallback;
    protected int page = 0;
    public boolean ignoreCloseCallback;

    public PagedGui(ServerPlayer player, @Nullable Runnable closeCallback) {
        super(MenuType.GENERIC_9x5, player, false);
        this.closeCallback = closeCallback;
    }

    public void refreshOpen() {
        this.updateDisplay();
        this.open();
    }

    @Override
    public void onClose() {
        if (this.closeCallback != null && !ignoreCloseCallback) {
            this.closeCallback.run();
        }
    }

    protected void nextPage() {
        this.page = Math.min(this.getPageAmount() - 1, this.page + 1);
        this.updateDisplay();
    }

    protected boolean canNextPage() {
        return this.getPageAmount() > this.page + 1;
    }

    protected void previousPage() {
        this.page = Math.max(0, this.page - 1);
        this.updateDisplay();
    }

    protected boolean canPreviousPage() {
        return this.page - 1 >= 0;
    }

    protected void updateDisplay() {
        var offset = this.page * PAGE_SIZE;

        for (int i = 0; i < PAGE_SIZE; i++) {
            var element = this.getElement(offset + i);

            if (element == null) {
                element = DisplayElement.empty();
            }

            if (element.element() != null) {
                this.setSlot(i, element.element());
            } else if (element.slot() != null) {
                this.setSlotRedirect(i, element.slot());
            }
        }

        for (int i = 0; i < 9; i++) {
            var navElement = this.getNavElement(i);

            if (navElement == null) {
                navElement = DisplayElement.EMPTY;
            }

            if (navElement.element != null) {
                this.setSlot(i + PAGE_SIZE, navElement.element);
            } else if (navElement.slot != null) {
                this.setSlotRedirect(i + PAGE_SIZE, navElement.slot);
            }
        }
    }

    protected int getPage() {
        return this.page;
    }

    protected abstract int getPageAmount();

    protected abstract DisplayElement getElement(int id);

    protected DisplayElement getNavElement(int id) {
        return switch (id) {
            case 1 -> DisplayElement.previousPage(this);
            case 3 -> DisplayElement.nextPage(this);
            case 7 ->
                    DisplayElement.of(
                            new GuiElementBuilder(Items.STRUCTURE_VOID)
                                    .setName(
                                            Component.translatable(
                                                            this.closeCallback != null
                                                                    ? "factions.gui.generic.back"
                                                                    : "factions.gui.generic.close")
                                                    .withStyle(ChatFormatting.RED))
                                    .hideDefaultTooltip()
                                    .setCallback(
                                            (x, y, z) -> {
                                                playClickSound(this.player);
                                                this.close(this.closeCallback != null);
                                            }));
            default -> DisplayElement.filler();
        };
    }

    public record DisplayElement(@Nullable GuiElementInterface element, @Nullable Slot slot) {
        private static final DisplayElement EMPTY =
                DisplayElement.of(
                        new GuiElement(ItemStack.EMPTY, GuiElementInterface.EMPTY_CALLBACK));
        private static final DisplayElement FILLER =
                DisplayElement.of(
                        new GuiElementBuilder(Items.WHITE_STAINED_GLASS_PANE)
                                .setName(Component.empty())
                                .hideTooltip());

        public static DisplayElement of(GuiElementInterface element) {
            return new DisplayElement(element, null);
        }

        public static DisplayElement of(GuiElementBuilderInterface<?> element) {
            return new DisplayElement(element.build(), null);
        }

        public static DisplayElement of(Slot slot) {
            return new DisplayElement(null, slot);
        }

        public static DisplayElement nextPage(PagedGui gui) {
            if (gui.canNextPage()) {
                return DisplayElement.of(
                        new GuiElementBuilder(Items.PLAYER_HEAD)
                                .setName(
                                        Component.translatable("factions.gui.generic.next_page")
                                                .withStyle(ChatFormatting.WHITE))
                                .hideDefaultTooltip()
                                .setProfileSkinTexture(Icons.GUI_NEXT_PAGE)
                                .setCallback(
                                        (x, y, z) -> {
                                            playClickSound(gui.player);
                                            gui.nextPage();
                                        }));
            } else {
                return DisplayElement.of(
                        new GuiElementBuilder(Items.PLAYER_HEAD)
                                .setName(
                                        Component.translatable("factions.gui.generic.next_page")
                                                .withStyle(ChatFormatting.DARK_GRAY))
                                .hideDefaultTooltip()
                                .setProfileSkinTexture(Icons.GUI_NEXT_PAGE_BLOCKED));
            }
        }

        public static DisplayElement previousPage(PagedGui gui) {
            if (gui.canPreviousPage()) {
                return DisplayElement.of(
                        new GuiElementBuilder(Items.PLAYER_HEAD)
                                .setName(
                                        Component.translatable("factions.gui.generic.previous_page")
                                                .withStyle(ChatFormatting.WHITE))
                                .hideDefaultTooltip()
                                .setProfileSkinTexture(Icons.GUI_PREVIOUS_PAGE)
                                .setCallback(
                                        (x, y, z) -> {
                                            playClickSound(gui.player);
                                            gui.previousPage();
                                        }));
            } else {
                return DisplayElement.of(
                        new GuiElementBuilder(Items.PLAYER_HEAD)
                                .setName(
                                        Component.translatable("factions.gui.generic.previous_page")
                                                .withStyle(ChatFormatting.DARK_GRAY))
                                .hideDefaultTooltip()
                                .setProfileSkinTexture(Icons.GUI_PREVIOUS_PAGE_BLOCKED));
            }
        }

        public static DisplayElement filler() {
            return FILLER;
        }

        public static DisplayElement empty() {
            return EMPTY;
        }
    }

    public static final void playClickSound(ServerPlayer player) {
        GuiInteract.playSound(player, SoundEvents.UI_BUTTON_CLICK.value(), 1f, 1f);
    }
}
