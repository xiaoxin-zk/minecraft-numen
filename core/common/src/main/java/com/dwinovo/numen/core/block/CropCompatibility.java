package com.dwinovo.numen.core.block;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Collections;
import java.util.List;

/** Generic growth-state support for vanilla and modded crops. */
public final class CropCompatibility {
    private static final List<String> GROWTH_NAMES = List.of(
            "age", "growth", "stage", "growth_stage", "crop_age");

    private CropCompatibility() {}

    public static IntegerProperty growthProperty(BlockState state) {
        if (state == null) return null;
        for (String name : GROWTH_NAMES) {
            for (Property<?> property : state.getProperties()) {
                if (property instanceof IntegerProperty integer
                        && name.equals(property.getName())) {
                    return integer;
                }
            }
        }
        return null;
    }

    public static int growth(BlockState state) {
        IntegerProperty property = growthProperty(state);
        return property == null ? -1 : state.getValue(property);
    }

    public static boolean hasGrowth(BlockState state) {
        return growthProperty(state) != null;
    }

    public static int maxGrowth(BlockState state) {
        IntegerProperty property = growthProperty(state);
        return property == null ? -1 : Collections.max(property.getPossibleValues());
    }

    public static boolean isMature(BlockState state) {
        int growth = growth(state);
        return growth >= 0 && growth >= maxGrowth(state);
    }
}
