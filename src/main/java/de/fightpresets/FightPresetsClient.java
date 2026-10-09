package de.fightpresets;

import com.mojang.blaze3d.platform.InputConstants;
import de.fightpresets.mixin.AbstractContainerScreenAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public class FightPresetsClient implements ClientModInitializer {
    private static final int GREEN = 0x6600C800;
    private static final int RED = 0x66FF0000;
    private static final int RED_LIGHT = 0x33FF0000;
    private static final int GHOST_FADE = 0xB88B8B8B; // Slot-Grau mit Alpha => Item wirkt "durchsichtig"

    private static KeyMapping openKey;
    private static KeyMapping toggleKey;

    @Override
    public void onInitializeClient() {
        PresetManager.load();

        openKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.fightpresets.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, KeyMapping.Category.MISC));
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.fightpresets.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, KeyMapping.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.consumeClick()) mc.setScreen(new PresetScreen(mc.screen));
            while (toggleKey.consumeClick()) PresetManager.toggle();
        });

        ScreenEvents.AFTER_INIT.register((mc, screen, w, h) -> {
            if (!(screen instanceof InventoryScreen)) return;

            Screens.getButtons(screen).add(Button.builder(Component.literal("Presets"),
                    b -> mc.setScreen(new PresetScreen(screen))).bounds(4, 4, 60, 20).build());
            Screens.getButtons(screen).add(Button.builder(
                    Component.literal(PresetManager.isEnabled() ? "Overlay: AN" : "Overlay: AUS"),
                    b -> {
                        PresetManager.toggle();
                        b.setMessage(Component.literal(PresetManager.isEnabled() ? "Overlay: AN" : "Overlay: AUS"));
                    }).bounds(4, 26, 80, 20).build());

            ScreenEvents.afterRender(screen).register((s, g, mx, my, dt) -> renderOverlay((AbstractContainerScreen<?>) s, g));
        });
    }

    private static void renderOverlay(AbstractContainerScreen<?> screen, GuiGraphics g) {
        if (!PresetManager.isEnabled()) return;
        PresetManager.Preset preset = PresetManager.active();
        Minecraft mc = Minecraft.getInstance();
        if (preset == null || mc.player == null) return;

        AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) screen;
        int left = acc.fp_getLeftPos();
        int top = acc.fp_getTopPos();

        for (Slot slot : screen.getMenu().slots) {
            if (slot.container != mc.player.getInventory()) continue;
            String wanted = preset.slots.get(slot.getContainerSlot());
            if (wanted == null) continue; // Slot gehoert nicht zum Preset

            int x = left + slot.x;
            int y = top + slot.y;
            ItemStack have = slot.getItem();

            if (have.isEmpty()) {
                // Ghost-Item: Item zeichnen und mit Slot-Grau abdunkeln
                g.renderItem(new ItemStack(PresetManager.itemOf(wanted)), x, y);
                g.fill(x, y, x + 16, y + 16, GHOST_FADE);
                g.fill(x, y, x + 16, y + 16, RED_LIGHT);
            } else if (PresetManager.idOf(have.getItem()).equals(wanted)) {
                g.fill(x, y, x + 16, y + 16, GREEN);
            } else {
                g.fill(x, y, x + 16, y + 16, RED);
            }
        }
    }
}
