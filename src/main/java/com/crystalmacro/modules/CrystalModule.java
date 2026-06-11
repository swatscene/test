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
 *
 * /crystalspeed <1-50>
 *   1  = INSTANT (hits every tick + bursts up to 5 times per tick if crosshair stays on a crystal)
 *   2  = every tick, single hit
 *   3+ = wait N-2 extra ticks between hits (slower)
 */
public class CrystalModule extends Module {

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
                            String label = v == 1 ? "INSTANT (burst mode)"
                                          : v == 2 ? "fast"
                                          : v == 50 ? "slowest"
                                          : "tick delay";
                            ctx.getSource().sendFeedback(
                                Text.literal("§7[CrystalMacro] §fSpeed set to §a" + v
                                        + " §7(" + label + ")"));
                            return 1;
                        })
                    )
                    .executes(ctx -> {
                        ctx.getSource().sendFeedback(
                            Text.literal("§7[CrystalMacro] §fSpeed: §a" + speed
                                    + " §7| §f/crystalspeed <1-50> §7(1=instant)"));
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

        // Find what crystal we're aiming at
        HitResult hit = client.crosshairTarget;
        if (!(hit instanceof EntityHitResult ehr)) return;
        if (!(ehr.getEntity() instanceof EndCrystalEntity crystal)) return;

        if (speed == 1) {
            // INSTANT mode — burst up to 5 attacks in a single tick.
            // The first one breaks the crystal; the rest hit whatever new crystal
            // appears under the crosshair before the next tick.
            for (int i = 0; i < 5; i++) {
                client.interactionManager.attackEntity(client.player, crystal);
                client.player.swingHand(client.player.getActiveHand());
            }
            cooldown = 0;
        } else {
            client.interactionManager.attackEntity(client.player, crystal);
            client.player.swingHand(client.player.getActiveHand());
            cooldown = speed - 2;   // 2=no wait, 3=1 tick wait, ..., 50=48 ticks
        }
    }
}
