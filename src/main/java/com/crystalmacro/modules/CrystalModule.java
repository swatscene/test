package com.crystalmacro.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class CrystalModule extends Module {

    // Tunable (we'll expose these in the GUI later)
    public static double reach        = 5.0;
    public static int    hitsPerTick  = 3;       // how many crystals to attack each tick (fixes spam stall)
    public static int    placementWindow = 20;   // ticks a pending placement stays valid
    public static double matchRadius  = 0.7;

    private int tickNum = 0;
    private final Set<UUID> myCrystals   = new HashSet<>();
    private final Set<UUID> seenCrystals = new HashSet<>();
    private final List<PendingPlace> pending = new ArrayList<>();

    private record PendingPlace(Vec3d pos, int tick) {}

    public CrystalModule() {
        super("CrystalMacro", "key.crystalmacro.toggle", GLFW.GLFW_KEY_R);
    }

    @Override
    protected void onDisable() {
        myCrystals.clear();
        pending.clear();
    }

    @Override
    public void tick(MinecraftClient client) {
        tickNum++;
        if (client.player == null || client.world == null || client.interactionManager == null) return;

        boolean useDown = client.options.useKey.isPressed();
        boolean hasCrystal = client.player.getMainHandStack().isOf(Items.END_CRYSTAL)
                          || client.player.getOffHandStack().isOf(Items.END_CRYSTAL);

        // Record placements
        if (useDown && hasCrystal && client.crosshairTarget instanceof BlockHitResult bhr
                && bhr.getType() == HitResult.Type.BLOCK) {
            BlockPos bp = bhr.getBlockPos();
            Vec3d expected = new Vec3d(bp.getX() + 0.5, bp.getY() + 1.0, bp.getZ() + 0.5);
            boolean dup = pending.stream().anyMatch(p -> p.pos.squaredDistanceTo(expected) < 0.1);
            if (!dup) pending.add(new PendingPlace(expected, tickNum));
        }
        pending.removeIf(p -> tickNum - p.tick() > placementWindow);

        // Match new crystals to placements
        Set<UUID> present = new HashSet<>();
        for (var e : client.world.getEntities()) {
            if (!(e instanceof EndCrystalEntity c)) continue;
            UUID id = c.getUuid();
            present.add(id);
            if (seenCrystals.contains(id)) continue;

            Vec3d cp = new Vec3d(c.getX(), c.getY(), c.getZ());
            Iterator<PendingPlace> it = pending.iterator();
            while (it.hasNext()) {
                PendingPlace p = it.next();
                if (p.pos().squaredDistanceTo(cp) < matchRadius * matchRadius) {
                    myCrystals.add(id);
                    it.remove();
                    break;
                }
            }
        }
        seenCrystals.clear();
        seenCrystals.addAll(present);
        myCrystals.retainAll(present);

        if (!useDown || !hasCrystal) return;

        // Attack up to `hitsPerTick` crystals each tick (no cooldown stall)
        double reachSq = reach * reach;
        int hits = 0;
        List<EndCrystalEntity> targets = new ArrayList<>();
        for (var e : client.world.getEntities()) {
            if (!(e instanceof EndCrystalEntity c)) continue;
            if (!myCrystals.contains(c.getUuid())) continue;
            if (c.squaredDistanceTo(client.player) <= reachSq) targets.add(c);
        }
        targets.sort(Comparator.comparingDouble(c -> c.squaredDistanceTo(client.player)));
        for (EndCrystalEntity c : targets) {
            if (hits >= hitsPerTick) break;
            client.interactionManager.attackEntity(client.player, c);
            client.player.swingHand(client.player.getActiveHand());
            myCrystals.remove(c.getUuid());
            hits++;
        }
    }
}
