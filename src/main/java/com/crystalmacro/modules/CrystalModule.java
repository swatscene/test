package com.crystalmacro.modules;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

/**
 * Triggerbot — attacks the crystal your crosshair is on.
 * Speed adjustable via /crystalspeed <1-50>  (1 = fastest, 50 = slowest)
 */
public class CrystalModule extends Module {

    // speed value: 1..50 (1 = hit every tick, 50 = hit every 50 ticks ~ 2.5 sec)
    public static int speed = 1;

    private int cooldown = 0;
    private boolean commandRegistered = false;

    public CrystalModule() {
        super("CrystalMacro", "key.crystalmacro.toggle", GLFW.GLFW_KEY_R);
        registerCommand();
    }

    private void registerCommand() {
        if (commandRegistered) return;
        commandRegistered = true;

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                ClientCommandManager.literal("crystalspeed")
                    .then(ClientCommandManager.argument("value", IntegerArgumentType.integer(1, 50))
                        .executes(ctx -> {
                            int v = IntegerArgumentType.getInteger(ctx, "value");
                            speed = v;
                            ctx.getSource().sendFeedback(
                                Text.literal("§7[CrystalMacro] §fSpeed set to §a" + v
                                        + " §7(" + (v == 1 ? "fastest" : v == 50 ? "slowest" : "tick delay") + ")"));
                            return 1;
                        })
                    )
                    .executes(ctx -> {
                        ctx.getSource().sendFeedback(
                            Text.literal("§7[CrystalMacro] §fCurrent speed: §a" + speed
                                    + " §7| Use §f/crystalspeed <1-50>"));
                        return 1;
                    })
            );
        });
    }

    @Override
    public void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        if (client.interactionManager == null) return;

        if (cooldown > 0) { cooldown--; return; }
        if (!client.options.useKey.isPressed()) return;

        HitResult hit = client.crosshairTarget;
        if (!(hit instanceof EntityHitResult ehr)) return;
        if (!(ehr.getEntity() instanceof EndCrystalEntity crystal)) return;

        client.interactionManager.attackEntity(client.player, crystal);
        client.player.swingHand(client.player.getActiveHand());
        cooldown = speed - 1;   // speed=1 → no cooldown, speed=50 → wait 49 ticks
    }
}
