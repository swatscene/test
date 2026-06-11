package com.crystalmacro;

import com.crystalmacro.modules.CrystalModule;
import com.crystalmacro.modules.Module;
import com.crystalmacro.modules.TotemRestockModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class CrystalMacro implements ClientModInitializer {

    public static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("crystalmacro", "main"));

    public static final List<Module> MODULES = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        MODULES.add(new CrystalModule());
        MODULES.add(new TotemRestockModule());

        for (Module m : MODULES) m.registerKeybind();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            for (Module m : MODULES) m.handleKeybind();
            for (Module m : MODULES) {
                if (m.isEnabled()) m.tick(client);
            }
        });
    }
}
