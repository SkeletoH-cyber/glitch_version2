package com.ruslan.glitch.item;

import com.ruslan.glitch.GlitchMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Rarity;

public final class ModItems {
    private ModItems() {}

    public static final Item REALITY_ANCHOR = Registry.register(
            Registries.ITEM,
            GlitchMod.id("reality_anchor"),
            new RealityAnchorItem(new Item.Settings().maxCount(16).rarity(Rarity.EPIC)));

    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(REALITY_ANCHOR));
    }
}
