package com.crystalmacro.modules;

import com.crystalmacro.CrystalMacro;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public abstract class Module {
    private final String name;
    private final String translationKey;
    private final int defaultKey;
    private boolean enabled = false;
    private KeyBinding keybind;

    protected Module(String name, String translationKey, int defaultKey) {
        this.name = name;
        this.translationKey = translationKey;
        this.defaultKey = defaultKey;
    }

    public String getName() { return name; }
    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean e) {
        enabled = e;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal(
                "§7[" + name + "] " + (enabled ? "§aON" : "§cOFF")), true);
        }
        if (enabled) onEnable(); else onDisable();
    }

    public void toggle() { setEnabled(!enabled); }

    public void registerKeybind() {
        keybind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                translationKey, InputUtil.Type.KEYSYM, defaultKey, CrystalMacro.CATEGORY));
    }

    public void handleKeybind() {
        if (keybind != null) {
            while (keybind.wasPressed()) toggle();
        }
    }

    protected void onEnable() {}
    protected void onDisable() {}
    public abstract void tick(MinecraftClient client);
}
