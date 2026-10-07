package com.dwinovo.numen.core.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;

/** Optional fishing integrations that must not make Tide a hard dependency. */
public final class FishingCompatibility {
    private static final String TIDE_ACCESSOR =
            "com.li64.tide.registries.entities.misc.fishing.HookAccessor";

    private static final Method TIDE_GET_HOOK = staticMethod("getHook", Player.class);

    private FishingCompatibility() {}

    /** Returns Tide's real hook, or {@code null} when Tide is absent/not active. */
    public static Object tideHook(Player player) {
        if (player == null || TIDE_GET_HOOK == null) return null;
        try {
            Object hook = TIDE_GET_HOOK.invoke(null, player);
            if (hook instanceof Entity entity && !entity.isRemoved()) return hook;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Optional mod APIs are allowed to disappear between pack versions.
        }
        return null;
    }

    public static boolean isTideHook(Object hook) {
        return hook != null && hook.getClass().getName().equals(
                "com.li64.tide.registries.entities.misc.fishing.TideFishingHook");
    }

    public static boolean isTideRod(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem().getClass().getName().equals(
                "com.li64.tide.registries.items.TideFishingRodItem");
    }

    /** Tide exposes its bite state as isFishHooked(). */
    public static boolean isBiting(Object hook) {
        if (!isTideHook(hook)) return false;
        try {
            return (boolean) hook.getClass().getMethod("isFishHooked").invoke(hook);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    public static Entity hookedEntity(Object hook) {
        if (!isTideHook(hook)) return null;
        try {
            Object value = hook.getClass().getMethod("getHookedIn").invoke(hook);
            return value instanceof Entity entity ? entity : null;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    /** Reels Tide in through its server-side reward path, bypassing client minigames. */
    public static boolean retrieve(Player player) {
        Object hook = tideHook(player);
        if (hook == null) return false;
        try {
            hook.getClass().getMethod("retrieve").invoke(hook);
            return true;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    /** Clears Tide's wrapper and its real hook together. */
    public static boolean discard(Player player) {
        Object hook = tideHook(player);
        if (hook == null) return false;
        try {
            Object accessor = player.fishing;
            if (accessor != null) {
                Method clear = accessor.getClass().getMethod("clearHook", Player.class);
                clear.invoke(accessor, player);
            } else if (hook instanceof Entity entity) {
                entity.discard();
            }
            return true;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            if (hook instanceof Entity entity) entity.discard();
            return true;
        }
    }

    private static Method staticMethod(String name, Class<?>... parameterTypes) {
        try {
            Class<?> type = Class.forName(TIDE_ACCESSOR, false,
                    FishingCompatibility.class.getClassLoader());
            return type.getMethod(name, parameterTypes);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }
}
