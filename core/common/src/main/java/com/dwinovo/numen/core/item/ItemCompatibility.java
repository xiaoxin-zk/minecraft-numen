package com.dwinovo.numen.core.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.block.CropBlock;

/**
 * Cross-loader item capability checks used by tasks and path planning.
 *
 * <p>Checking the vanilla singleton (for example {@code Items.SHIELD}) misses
 * modded implementations.  Class checks cover mods that extend vanilla item
 * classes, while common Forge/Fabric tags cover independent implementations.
 * The small id fallback is intentional for the pack's KubeJS seed packets,
 * which are basic items and cannot implement a crop interface.
 */
public final class ItemCompatibility {
    private static final TagKey<Item> FORGE_SHIELDS = tag("forge", "shields");
    private static final TagKey<Item> FORGE_TOOL_SHIELDS = tag("forge", "tools/shields");
    private static final TagKey<Item> COMMON_SHIELDS = tag("c", "shields");
    private static final TagKey<Item> COMMON_TOOL_SHIELDS = tag("c", "tools/shields");
    private static final TagKey<Item> FORGE_BOWS = tag("forge", "tools/bows");
    private static final TagKey<Item> COMMON_BOWS = tag("c", "tools/bows");
    private static final TagKey<Item> COMMON_BOW = tag("c", "tools/bow");
    private static final TagKey<Item> FORGE_CROSSBOWS = tag("forge", "tools/crossbows");
    private static final TagKey<Item> COMMON_CROSSBOWS = tag("c", "tools/crossbows");
    private static final TagKey<Item> COMMON_CROSSBOW = tag("c", "tools/crossbow");
    private static final TagKey<Item> FORGE_FISHING_RODS = tag("forge", "tools/fishing_rods");
    private static final TagKey<Item> COMMON_FISHING_RODS = tag("c", "tools/fishing_rods");
    private static final TagKey<Item> COMMON_FISHING_ROD = tag("c", "tools/fishing_rod");
    private static final TagKey<Item> TIDE_FISHING_RODS = tag("tide", "fishing_rods");
    private static final TagKey<Item> FORGE_SEEDS = tag("forge", "seeds");
    private static final TagKey<Item> COMMON_SEEDS = tag("c", "seeds");
    private static final TagKey<Item> VANILLA_SEEDS = tag("minecraft", "seeds");
    private static final TagKey<Item> FORGE_WATER_BUCKETS = tag("forge", "buckets/water");
    private static final TagKey<Item> COMMON_WATER_BUCKETS = tag("c", "buckets/water");
    private static final TagKey<Item> FORGE_EMPTY_BUCKETS = tag("forge", "buckets/empty");
    private static final TagKey<Item> COMMON_EMPTY_BUCKETS = tag("c", "buckets/empty");

    private ItemCompatibility() {}

    private static TagKey<Item> tag(String namespace, String path) {
        return ItemTags.create(new ResourceLocation(namespace, path));
    }

    public static boolean isShield(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof ShieldItem
                || stack.is(FORGE_SHIELDS) || stack.is(FORGE_TOOL_SHIELDS)
                || stack.is(COMMON_SHIELDS) || stack.is(COMMON_TOOL_SHIELDS));
    }

    public static boolean isBow(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof BowItem
                || stack.is(FORGE_BOWS) || stack.is(COMMON_BOWS) || stack.is(COMMON_BOW));
    }

    public static boolean isCrossbow(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof CrossbowItem
                || stack.is(FORGE_CROSSBOWS) || stack.is(COMMON_CROSSBOWS)
                || stack.is(COMMON_CROSSBOW));
    }

    public static boolean isFishingRod(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof FishingRodItem
                || stack.is(FORGE_FISHING_RODS) || stack.is(COMMON_FISHING_RODS)
                || stack.is(COMMON_FISHING_ROD) || stack.is(TIDE_FISHING_RODS));
    }

    public static boolean isWaterBucket(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(FORGE_WATER_BUCKETS)
                || stack.is(COMMON_WATER_BUCKETS) || bucketFluid(stack) == Fluids.WATER);
    }

    public static boolean isEmptyBucket(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(FORGE_EMPTY_BUCKETS)
                || stack.is(COMMON_EMPTY_BUCKETS)
                || bucketFluid(stack) == Fluids.EMPTY && stack.getItem() instanceof BucketItem);
    }

    /**
     * Whether this stack can stay in the main hand while a block item in the
     * offhand is used for scaffolding. Many Forge tools are plain items with a
     * zero-duration use action and must not be rejected as non-tiered tools.
     */
    public static boolean isNonConsumptiveMainHand(ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (stack.getItem() instanceof BlockItem) return false;
        return stack.getItem().getUseDuration(stack) <= 0;
    }

    private static Fluid bucketFluid(ItemStack stack) {
        return stack.isEmpty() || !(stack.getItem() instanceof BucketItem bucket)
                ? Fluids.EMPTY : bucket.getFluid();
    }

    /**
     * Recognises ordinary seed tags and the FarmingTales KubeJS packet items.
     * This is used for user-facing interaction guidance; planting remains the
     * item's own right-click behaviour, so custom crop rules are preserved.
     */
    public static boolean isSeedLike(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(VANILLA_SEEDS) || stack.is(FORGE_SEEDS) || stack.is(COMMON_SEEDS)) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String path = id.getPath();
        if (stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof CropBlock) {
            return true;
        }
        if (BuiltInRegistries.BLOCK.getOptional(id).map(block -> block instanceof CropBlock).orElse(false)) {
            return true;
        }
        if (id.getNamespace().equals("farmingtales") && path.endsWith("_packet")) {
            return true;
        }
        if (path.contains("seed") || path.contains("seeds")
                || path.endsWith("_pip") || path.endsWith("_pips")
                || path.endsWith("_packet") || path.endsWith("_packets")) {
            return true;
        }
        // A number of crops in this pack use edible/plantable names instead of
        // the word "seed" (kernels, onion, beans, rice, coffee beans, etc.).
        return path.equals("kernels") || path.equals("onion")
                || path.equals("redbean") || path.equals("soybean")
                || path.equals("coffee_beans") || path.equals("sweet_potato")
                || path.equals("rice") || path.equals("tea_blossom")
                || path.endsWith("_sprout") || path.endsWith("_sapling");
    }
}
