package com.crystalmacro;

import com.crystalmacro.gui.ConfigScreen;
import com.crystalmacro.modules.CrystalModule;
import com.crystalmacro.modules.Module;
import com.crystalmacro.modules.SprintModule;
import com.crystalmacro.modules.TotemHitModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class CrystalMacro implements ClientModInitializer {

    public static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("crystalmacro", "main"));

    public static final List<Module> MODULES = new ArrayList<>();

    private static KeyBinding guiKey;

    @Override
    public void onInitializeClient() {
        // Register modules
        MODULES.add(new CrystalModule());
        MODULES.add(new SprintModule());
        MODULES.add(new TotemHitModule());

        // GUI keybind (default: [ )
        guiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.crystalmacro.gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_BRACKET,
                CATEGORY
        ));

        // Register all module keybinds
        for (Module m : MODULES) m.registerKeybind();

        // Per-tick: handle GUI key + run modules
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (guiKey.wasPressed()) {
                client.setScreen(new ConfigScreen());
            }
            for (Module m : MODULES) m.handleKeybind();
            for (Module m : MODULES) {
                if (m.isEnabled()) m.tick(client);
            }
        });
    }
}
