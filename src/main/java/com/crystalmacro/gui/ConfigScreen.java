package com.crystalmacro.gui;

import com.crystalmacro.CrystalMacro;
import com.crystalmacro.modules.CrystalModule;
import com.crystalmacro.modules.Module;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ConfigScreen extends Screen {

    public ConfigScreen() {
        super(Text.literal("Crystal Macro Config"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = 40;

        // ── Module toggles ──
        for (Module m : CrystalMacro.MODULES) {
            final Module mod = m;
            ButtonWidget btn = ButtonWidget.builder(
                    Text.literal(mod.getName() + ": " + (mod.isEnabled() ? "§aON" : "§cOFF")),
                    b -> {
                        mod.toggle();
                        b.setMessage(Text.literal(
                                mod.getName() + ": " + (mod.isEnabled() ? "§aON" : "§cOFF")));
                    }
            ).dimensions(cx - 100, y, 200, 20).build();
            this.addDrawableChild(btn);
            y += 24;
        }

        y += 10;

        // ── Reach -/+ ──
        this.addDrawableChild(ButtonWidget.builder(Text.literal("-"), b -> {
            CrystalModule.reach = Math.max(3.0, CrystalModule.reach - 0.5);
            refresh();
        }).dimensions(cx - 100, y, 40, 20).build());

        reachLabel = ButtonWidget.builder(
                Text.literal("Reach: " + fmt(CrystalModule.reach)),
                b -> {}
        ).dimensions(cx - 55, y, 110, 20).build();
        this.addDrawableChild(reachLabel);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> {
            CrystalModule.reach = Math.min(8.0, CrystalModule.reach + 0.5);
            refresh();
        }).dimensions(cx + 60, y, 40, 20).build());

        y += 24;

        // ── Hits per Tick -/+ ──
        this.addDrawableChild(ButtonWidget.builder(Text.literal("-"), b -> {
            CrystalModule.hitsPerTick = Math.max(1, CrystalModule.hitsPerTick - 1);
            refresh();
        }).dimensions(cx - 100, y, 40, 20).build());

        hitsLabel = ButtonWidget.builder(
                Text.literal("Hits/Tick: " + CrystalModule.hitsPerTick),
                b -> {}
        ).dimensions(cx - 55, y, 110, 20).build();
        this.addDrawableChild(hitsLabel);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> {
            CrystalModule.hitsPerTick = Math.min(20, CrystalModule.hitsPerTick + 1);
            refresh();
        }).dimensions(cx + 60, y, 40, 20).build());

        y += 30;

        // ── Close ──
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Close"),
                b -> this.close()
        ).dimensions(cx - 50, y, 100, 20).build());
    }

    private ButtonWidget reachLabel;
    private ButtonWidget hitsLabel;

    private void refresh() {
        if (reachLabel != null)
            reachLabel.setMessage(Text.literal("Reach: " + fmt(CrystalModule.reach)));
        if (hitsLabel != null)
            hitsLabel.setMessage(Text.literal("Hits/Tick: " + CrystalModule.hitsPerTick));
    }

    private static String fmt(double d) {
        return String.format("%.1f", d);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                "§dCrystal Macro §7v3", this.width / 2, 16, 0xFFFFFF);
    }

    @Override
    public boolean shouldPause() { return false; }
}
