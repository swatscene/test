package com.crystalmacro.modules;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public class SprintModule extends Module {
    public SprintModule() {
        super("AutoSprint", "key.crystalmacro.sprint", GLFW.GLFW_KEY_J);
    }

    @Override
    public void tick(MinecraftClient client) {
        if (client.player == null) return;
        // Force sprint whenever moving forward and not sneaking
        if (client.options.forwardKey.isPressed() && !client.player.isSneaking()) {
            client.player.setSprinting(true);
        }
    }
}
