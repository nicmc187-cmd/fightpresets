package de.fightpresets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class PresetScreen extends Screen {
    private static final int PER_PAGE = 5;

    private final Screen parent;
    private String nameText = "";
    private int iconIdx = 0;
    private int page = 0;
    private EditBox nameBox;

    public PresetScreen(Screen parent) {
        super(Component.literal("Fight Presets"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int top = Math.max(10, height / 2 - 120);

        nameBox = new EditBox(font, cx - 100, top + 24, 200, 20, Component.literal("Name"));
        nameBox.setHint(Component.literal("Preset-Name"));
        nameBox.setMaxLength(24);
        nameBox.setValue(nameText);
        nameBox.setResponder(t -> nameText = t);
        addRenderableWidget(nameBox);

        addRenderableWidget(Button.builder(Component.literal("Symbol wechseln"), b -> {
            iconIdx = (iconIdx + 1) % PresetManager.ICONS.length;
        }).bounds(cx - 76, top + 48, 100, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Inventar speichern"), b -> {
            PresetManager.saveFromInventory(nameText.trim(), PresetManager.ICONS[iconIdx]);
            rebuildWidgets();
        }).bounds(cx + 28, top + 48, 72, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("Overlay im Inventar: " + (PresetManager.isEnabled() ? "AN" : "AUS")),
                b -> { PresetManager.toggle(); rebuildWidgets(); }
        ).bounds(cx - 100, top + 72, 200, 20).build());

        List<PresetManager.Preset> list = PresetManager.presets();
        int maxPage = Math.max(0, (list.size() - 1) / PER_PAGE);
        page = Math.min(page, maxPage);

        for (int i = 0; i < PER_PAGE; i++) {
            int idx = page * PER_PAGE + i;
            if (idx >= list.size()) break;
            PresetManager.Preset p = list.get(idx);
            boolean active = PresetManager.active() == p;
            int y = top + 100 + i * 22;

            addRenderableWidget(Button.builder(
                    Component.literal((active ? "> " : "") + p.name + " (" + p.slots.size() + ")"),
                    b -> {
                        PresetManager.setActive(p.name);
                        nameText = p.name;
                        for (int k = 0; k < PresetManager.ICONS.length; k++)
                            if (PresetManager.ICONS[k].equals(p.icon)) iconIdx = k;
                        rebuildWidgets();
                    }).bounds(cx - 76, y, 150, 20).build());

            addRenderableWidget(Button.builder(Component.literal("X"), b -> {
                PresetManager.delete(p.name);
                rebuildWidgets();
            }).bounds(cx + 78, y, 22, 20).build());
        }

        int by = top + 100 + PER_PAGE * 22 + 4;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            page = Math.max(0, page - 1); rebuildWidgets();
        }).bounds(cx - 100, by, 24, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            page = Math.min(maxPage, page + 1); rebuildWidgets();
        }).bounds(cx + 76, by, 24, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Fertig"), b -> onClose())
                .bounds(cx - 48, by, 96, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        int cx = width / 2;
        int top = Math.max(10, height / 2 - 120);

        g.drawCenteredString(font, title, cx, top + 6, 0xFFFFFFFF);
        g.renderItem(new ItemStack(PresetManager.itemOf(PresetManager.ICONS[iconIdx])), cx - 98, top + 50);

        List<PresetManager.Preset> list = PresetManager.presets();
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = page * PER_PAGE + i;
            if (idx >= list.size()) break;
            g.renderItem(PresetManager.iconOf(list.get(idx)), cx - 98, top + 102 + i * 22);
        }
        if (list.isEmpty()) {
            g.drawCenteredString(font, Component.literal("Noch keine Presets - Inventar aufbauen, Namen eingeben, speichern."),
                    cx, top + 130, 0xFFAAAAAA);
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
