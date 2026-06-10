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

    public static double reach           = 5.0;
    public static int    hitsPerTick     = 5;
    public static int    placementWindow = 30;
    public static double matchRadius     = 1.2;

    private int tickNum   = 0;
    private int rcActiveTicks = 0;       // how many ticks since last rclick activity
    private int rcSeenRecently  = 0;     // sub-tick edge tracker
    private boolean lastUse = false;

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
        seenCrystals.clear();
        rcActiveTicks = 0;
        rcSeenRecently = 0;
    }

    @Override
    public void tick(MinecraftClient client) {
        tickNum++;
        if (client.player == null || client.world == null || client.interactionManager == null) return;

        boolean useDown = client.options.useKey.isPressed();
        boolean hasCrystal = client.player.getMainHandStack().isOf(Items.END_CRYSTAL)
                          || client.player.getOffHandStack().isOf(Items.END_CRYSTAL);

        // Detect rising edge AND held — both count as "active placing"
        if (useDown || (lastUse != useDown)) {
            rcSeenRecently = 5;   // remain "active" for 5 ticks after any click activity
        }
        lastUse = useDown;
        if (rcSeenRecently > 0) rcSeenRecently--;
        boolean activelyPlacing = (rcSeenRecently > 0) && hasCrystal;

        // Record placement targets every tick we see use key + crystal + valid block
        if (useDown && hasCrystal && client.crosshairTarget instanceof BlockHitResult bhr
                && bhr.getType() == HitResult.Type.BLOCK) {
            BlockPos bp = bhr.getBlockPos();
            Vec3d expected = new Vec3d(bp.getX() + 0.5, bp.getY() + 1.0, bp.getZ() + 0.5);
            boolean dup = pending.stream().anyMatch(p -> p.pos.squaredDistanceTo(expected) < 0.05);
            if (!dup) pending.add(new PendingPlace(expected, tickNum));
        }
        pending.removeIf(p -> tickNum - p.tick() > placementWindow);

        // Find current crystals; match new ones to placements
        Set<UUID> present = new HashSet<>();
        List<EndCrystalEntity> allCrystals = new ArrayList<>();
        for (var e : client.world.getEntities()) {
            if (!(e instanceof EndCrystalEntity c)) continue;
            present.add(c.getUuid());
            allCrystals.add(c);

            if (seenCrystals.contains(c.getUuid())) continue;

            Vec3d cp = new Vec3d(c.getX(), c.getY(), c.getZ());
            Iterator<PendingPlace> it = pending.iterator();
            while (it.hasNext()) {
                PendingPlace p = it.next();
                if (p.pos().squaredDistanceTo(cp) < matchRadius * matchRadius) {
                    myCrystals.add(c.getUuid());
                    it.remove();
                    break;
                }
            }
        }
        seenCrystals.clear();
        seenCrystals.addAll(present);
        myCrystals.retainAll(present);

        // FALLBACK — works for both holding AND spam-clicking:
        // While the user has recently interacted with rclick and has crystals,
        // claim any unrecognized crystals in reach as ours.
        if (activelyPlacing) {
            double reachSqFb = reach * reach;
            for (EndCrystalEntity c : allCrystals) {
                if (myCrystals.contains(c.getUuid())) continue;
                if (c.squaredDistanceTo(client.player) <= reachSqFb) {
                    myCrystals.add(c.getUuid());
                }
            }
        }

        if (!useDown || !hasCrystal) return;

        // Attack
        double reachSq = reach * reach;
        List<EndCrystalEntity> targets = new ArrayList<>();
        for (EndCrystalEntity c : allCrystals) {
            if (!myCrystals.contains(c.getUuid())) continue;
            if (c.squaredDistanceTo(client.player) <= reachSq) targets.add(c);
        }
        targets.sort(Comparator.comparingDouble(c -> c.squaredDistanceTo(client.player)));

        int hits = 0;
        for (EndCrystalEntity c : targets) {
            if (hits >= hitsPerTick) break;
            client.interactionManager.attackEntity(client.player, c);
            client.player.swingHand(client.player.getActiveHand());
            myCrystals.remove(c.getUuid());
            hits++;
        }
    }
}
