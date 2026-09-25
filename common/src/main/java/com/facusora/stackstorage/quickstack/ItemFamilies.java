package com.facusora.stackstorage.quickstack;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Works out which vertical column an item belongs to and which row of that column
 * it sits in.
 *
 * Minerals are resolved from the loaders' common tags rather than a hardcoded item
 * list, so modded ores that follow the convention line up next to the vanilla ones for
 * free. NeoForge moved those tags from {@code forge:} to {@code c:} in 1.20.5, so both
 * namespaces are read.
 * Armor is resolved from the vanilla armor slot tags, so any mod's set that joins them
 * works too; its material is read off the item id ({@code diamond_helmet} is diamond).
 */
public final class ItemFamilies
{
    public enum Kind
    {
        /** Helmet, chestplate, leggings, boots. */
        ARMOR(4),
        /** Storage block, ingot/gem/dust, nugget, raw ore, raw block. */
        MINERAL(5);

        public final int tiers;

        Kind(int tiers)
        {
            this.tiers = tiers;
        }
    }

    /** A column: every item sharing a material, each one on the row its tier dictates. */
    public record Family(Kind kind, String material, int tier)
    {
    }

    private static final Set<String> COMMON_TAG_NAMESPACES = Set.of("c", "forge");

    /** common tag prefix -> row of the mineral column. Ordered heaviest (most compressed) first. */
    private static final Map<String, Integer> MINERAL_TIER_BY_TAG = Map.of(
            "storage_blocks", 0,
            "ingots", 1,
            "gems", 1,
            "dusts", 1,
            "nuggets", 2,
            "raw_materials", 3);

    /** Row for {@code c:storage_blocks/raw_<material>}: the bottom of the column. */
    private static final int RAW_BLOCK_TIER = 4;
    private static final String RAW_PREFIX = "raw_";

    /**
     * Vanilla items that belong to a material but carry no common family tag, so they
     * would otherwise never find their own column. Charcoal rides just under coal, and
     * netherite fills its two empty rows with the forms it actually has: ancient debris
     * where the nugget would go, then the scrap it smelts into.
     */
    private static final Map<Identifier, Family> UNTAGGED = Map.of(
            Identifier.withDefaultNamespace("coal"), new Family(Kind.MINERAL, "coal", 1),
            Identifier.withDefaultNamespace("charcoal"), new Family(Kind.MINERAL, "coal", 2),
            Identifier.withDefaultNamespace("ancient_debris"), new Family(Kind.MINERAL, "netherite", 2),
            Identifier.withDefaultNamespace("netherite_scrap"), new Family(Kind.MINERAL, "netherite", 3));

    /** Column order, most valuable first. Materials missing here sort alphabetically after these. */
    private static final List<String> MINERAL_ORDER = List.of(
            "netherite", "diamond", "emerald", "gold", "iron", "copper",
            "redstone", "lapis", "coal", "quartz", "amethyst");

    /** Row order of an armor column, helmet on top. */
    private static final List<TagKey<Item>> ARMOR_SLOTS = List.of(
            ItemTags.HEAD_ARMOR, ItemTags.CHEST_ARMOR, ItemTags.LEG_ARMOR, ItemTags.FOOT_ARMOR);

    private static final List<String> ARMOR_ORDER = List.of(
            "netherite", "diamond", "iron", "gold", "chainmail", "turtle", "leather");

    /** Armor columns first, then minerals; each block ordered by value, unknowns last. */
    public static final Comparator<Family> COLUMN_ORDER = Comparator
            .comparingInt((Family f) -> f.kind().ordinal())
            .thenComparingInt(f -> priorityOf(f.kind(), f.material()))
            .thenComparing(Family::material);

    private ItemFamilies()
    {
    }

    /**
     * @return the column and row this stack belongs to, or null if it is neither a
     *         recognised mineral nor armor (those fall back to the alphabetical pass)
     */
    public static Family classify(ItemStack stack)
    {
        int armorTier = armorTier(stack);
        if (armorTier >= 0)
            return new Family(Kind.ARMOR, armorMaterial(stack), armorTier);

        Family tagged = classifyMineral(stack);
        return tagged != null ? tagged : UNTAGGED.get(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    /** Helmet to boots is 0 to 3; -1 for anything that is not armor. */
    private static int armorTier(ItemStack stack)
    {
        for (int tier = 0; tier < ARMOR_SLOTS.size(); tier++)
            if (stack.is(ARMOR_SLOTS.get(tier)))
                return tier;
        return -1;
    }

    /** The id minus its piece: {@code diamond_helmet} is diamond, {@code golden_boots} is gold. */
    private static String armorMaterial(ItemStack stack)
    {
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        int underscore = path.lastIndexOf('_');
        String material = underscore > 0 ? path.substring(0, underscore) : path;
        return material.equals("golden") ? "gold" : material;
    }

    private static Family classifyMineral(ItemStack stack)
    {
        Family best = null;
        // An item can carry several matching tags; the most compressed form wins so the
        // result does not depend on tag iteration order.
        for (TagKey<Item> tag : stack.typeHolder().tags().toList())
        {
            Identifier id = tag.location();
            if (!COMMON_TAG_NAMESPACES.contains(id.getNamespace()))
                continue;

            String path = id.getPath();
            int slash = path.indexOf('/');
            if (slash < 0)
                continue; // umbrella tag such as c:ingots, carries no material

            String prefix = path.substring(0, slash);
            String material = path.substring(slash + 1);
            Integer tier = MINERAL_TIER_BY_TAG.get(prefix);
            if (tier == null)
                continue;

            // c:storage_blocks/raw_iron is the raw ore block, not the iron block
            if (tier == 0 && material.startsWith(RAW_PREFIX))
            {
                material = material.substring(RAW_PREFIX.length());
                tier = RAW_BLOCK_TIER;
            }

            if (best == null || tier < best.tier())
                best = new Family(Kind.MINERAL, material, tier);
        }
        return best;
    }

    private static int priorityOf(Kind kind, String material)
    {
        List<String> order = kind == Kind.ARMOR ? ARMOR_ORDER : MINERAL_ORDER;
        int index = order.indexOf(material);
        return index < 0 ? Integer.MAX_VALUE : index;
    }
}
