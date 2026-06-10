package com.crystalmacro.gui;

import com.crystalmacro.CrystalMacro;
import com.crystalmacro.modules.CrystalModule;
import com.crystalmacro.modules.Module;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class ConfigScreen extends Screen {

    public ConfigScreen() {
        super(Text.literal("Crystal Macro Config"));
    }

    @Override
    protected void init() {
        int y = 40;

        // Module toggles
        for (Module m : CrystalMacro.MODULES) {
            final Module mod = m;
            ButtonWidget btn = ButtonWidget.builder(
                    Text.literal(mod.getName() + ": " + (mod.isEnabled() ? "§aON" : "§cOFF")),
                    b -> {
                        mod.toggle();
                        b.setMessage(Text.literal(
                            mod.getName() + ": " + (mod.isEnabled() ? "§aON" : "§cOFF")));
                    }
            ).dimensions(this.width / 2 - 100, y, 200, 20).build();
            this.addDrawableChild(btn);
            y += 24;
        }

        y += 10;

        // Reach slider
        this.addDrawableChild(new SliderWidget(
                this.width / 2 - 100, y, 200, 20,
                Text.literal("Reach: " + String.format("%.1f", CrystalModule.reach)),
                (CrystalModule.reach - 3.0) / 5.0   // 3.0–8.0 range
        ) {
            @Override protected void updateMessage() {
                setMessage(Text.literal("Reach: " + String.format("%.1f", CrystalModule.reach)));
            }
            @Override protected void applyValue() {
                CrystalModule.reach = 3.0 + value * 5.0;
            }
        });
        y += 24;

        // Hits per tick slider
        this.addDrawableChild(new SliderWidget(
                this.width / 2 - 100, y, 200, 20,
                Text.literal("Hits/Tick: " + CrystalModule.hitsPerTick),
                (CrystalModule.hitsPerTick - 1) / 9.0   // 1–10 range
        ) {
            @Override protected void updateMessage() {
                setMessage(Text.literal("Hits/Tick: " + CrystalModule.hitsPerTick));
            }
            @Override protected void applyValue() {
                CrystalModule.hitsPerTick = 1 + (int) Math.round(value * 9);
            }
        });
        y += 24;

        // Close button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Close"),
                b -> this.close()
        ).dimensions(this.width / 2 - 50, y + 20, 100, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                "§dCrystal Macro §7v2", this.width / 2, 16, 0xFFFFFF);
    }

    @Override public boolean shouldPause() { return false; }
}
