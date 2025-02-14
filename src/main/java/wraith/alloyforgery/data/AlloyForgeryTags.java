package wraith.alloyforgery.data;

import net.fabricmc.fabric.api.tag.convention.v2.TagUtil;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import wraith.alloyforgery.AlloyForgery;

public class AlloyForgeryTags {

    private static final String COMMON_NAMESPACE = TagUtil.C_TAG_NAMESPACE;

    public static class Items {

        //Zinc
        public static final TagKey<Item> STORAGE_BLOCKS_RAW_ZINC = registerCommon("storage_blocks/raw_zinc");
        public static final TagKey<Item> STORAGE_BLOCKS_ZINC = registerCommon("storage_blocks/zinc");
        public static final TagKey<Item> RAW_MATERIALS_ZINC = registerCommon("raw_materials/zinc");
        public static final TagKey<Item> ORES_ZINC = registerCommon("ores/zinc");
        public static final TagKey<Item> INGOTS_ZINC = registerCommon("ingots/zinc");


        //tungsten
        public static final TagKey<Item> STORAGE_BLOCKS_RAW_TUNGSTEN = registerCommon("storage_blocks/raw_tungsten");
        public static final TagKey<Item> STORAGE_BLOCKS_TUNGSTEN = registerCommon("storage_blocks/tungsten");
        public static final TagKey<Item> RAW_MATERIALS_TUNGSTEN = registerCommon("raw_materials/tungsten");
        public static final TagKey<Item> ORES_TUNGSTEN = registerCommon("ores/tungsten");
        public static final TagKey<Item> INGOTS_TUNGSTEN = registerCommon("ingots/tungsten");

        //titanium
        public static final TagKey<Item> STORAGE_BLOCKS_RAW_TITANIUM = registerCommon("storage_blocks/raw_titanium");
        public static final TagKey<Item> STORAGE_BLOCKS_TITANIUM = registerCommon("storage_blocks/titanium");
        public static final TagKey<Item> RAW_MATERIALS_TITANIUM = registerCommon("raw_materials/titanium");
        public static final TagKey<Item> ORES_TITANIUM = registerCommon("ores/titanium");
        public static final TagKey<Item> INGOTS_TITANIUM = registerCommon("ingots/titanium");

        //tin
        public static final TagKey<Item> STORAGE_BLOCKS_RAW_TIN = registerCommon("storage_blocks/raw_tin");
        public static final TagKey<Item> STORAGE_BLOCKS_TIN = registerCommon("storage_blocks/tin");
        public static final TagKey<Item> RAW_MATERIALS_TIN = registerCommon("raw_materials/tin");
        public static final TagKey<Item> ORES_TIN = registerCommon("ores/tin");
        public static final TagKey<Item> INGOTS_TIN = registerCommon("ingots/tin");

        //silver
        public static final TagKey<Item> STORAGE_BLOCKS_RAW_SILVER = registerCommon("storage_blocks/raw_silver");
        public static final TagKey<Item> STORAGE_BLOCKS_SILVER = registerCommon("storage_blocks/silver");
        public static final TagKey<Item> RAW_MATERIALS_SILVER = registerCommon("raw_materials/silver");
        public static final TagKey<Item> ORES_SILVER = registerCommon("ores/silver");
        public static final TagKey<Item> INGOTS_SILVER = registerCommon("ingots/silver");

        //platinum
        public static final TagKey<Item> STORAGE_BLOCKS_RAW_PLATINUM = registerCommon("storage_blocks/raw_platinum");
        public static final TagKey<Item> STORAGE_BLOCKS_PLATINUM = registerCommon("storage_blocks/platinum");
        public static final TagKey<Item> RAW_MATERIALS_PLATINUM = registerCommon("raw_materials/platinum");
        public static final TagKey<Item> ORES_PLATINUM = registerCommon("ores/platinum");
        public static final TagKey<Item> INGOTS_PLATINUM = registerCommon("ingots/platinum");

        //palladium
        public static final TagKey<Item> STORAGE_BLOCKS_RAW_PALLADIUM = registerCommon("storage_blocks/raw_palladium");
        public static final TagKey<Item> STORAGE_BLOCKS_PALLADIUM = registerCommon("storage_blocks/palladium");
        public static final TagKey<Item> RAW_MATERIALS_PALLADIUM = registerCommon("raw_materials/palladium");
        public static final TagKey<Item> ORES_PALLADIUM = registerCommon("ores/palladium");
        public static final TagKey<Item> INGOTS_PALLADIUM = registerCommon("ingots/palladium");

        //osmium
        public static final TagKey<Item> STORAGE_BLOCKS_RAW_OSMIUM = registerCommon("storage_blocks/raw_osmium");
        public static final TagKey<Item> STORAGE_BLOCKS_OSMIUM = registerCommon("storage_blocks/osmium");
        public static final TagKey<Item> RAW_MATERIALS_OSMIUM = registerCommon("raw_materials/osmium");
        public static final TagKey<Item> ORES_OSMIUM = registerCommon("ores/osmium");
        public static final TagKey<Item> INGOTS_OSMIUM = registerCommon("ingots/osmium");

        //orichalcum
        public static final TagKey<Item> STORAGE_BLOCKS_RAW_ORICHALCUM = registerCommon("storage_blocks/raw_orichalcum");
        public static final TagKey<Item> STORAGE_BLOCKS_ORICHALCUM = registerCommon("storage_blocks/orichalcum");
        public static final TagKey<Item> RAW_MATERIALS_ORICHALCUM = registerCommon("raw_materials/orichalcum");
        public static final TagKey<Item> ORES_ORICHALCUM = registerCommon("ores/orichalcum");
        public static final TagKey<Item> INGOTS_ORICHALCUM = registerCommon("ingots/orichalcum");

        //nickel
        public static final TagKey<Item> STORAGE_BLOCKS_RAW_NICKEL = registerCommon("storage_blocks/raw_nickel");
        public static final TagKey<Item> STORAGE_BLOCKS_NICKEL = registerCommon("storage_blocks/nickel");
        public static final TagKey<Item> RAW_MATERIALS_NICKEL = registerCommon("raw_materials/nickel");
        public static final TagKey<Item> ORES_NICKEL = registerCommon("ores/nickel");
        public static final TagKey<Item> INGOTS_NICKEL = registerCommon("ingots/nickel");

        //mythril
        public static final TagKey<Item> STORAGE_BLOCKS_RAW_MYTHRIL = registerCommon("storage_blocks/raw_mythril");
        public static final TagKey<Item> STORAGE_BLOCKS_MYTHRIL = registerCommon("storage_blocks/mythril");
        public static final TagKey<Item> RAW_MATERIALS_MYTHRIL = registerCommon("raw_materials/mythril");
        public static final TagKey<Item> ORES_MYTHRIL = registerCommon("ores/mythril");
        public static final TagKey<Item> INGOTS_MYTHRIL = registerCommon("ingots/mythril");

        //mythril
        public static final TagKey<Item> STORAGE_BLOCKS_RAW_MANGANESE = registerCommon("storage_blocks/raw_manganese");
        public static final TagKey<Item> STORAGE_BLOCKS_MANGANESE = registerCommon("storage_blocks/manganese");
        public static final TagKey<Item> RAW_MATERIALS_MANGANESE = registerCommon("raw_materials/manganese");
        public static final TagKey<Item> ORES_MANGANESE = registerCommon("ores/manganese");
        public static final TagKey<Item> INGOTS_MANGANESE = registerCommon("ingots/manganese");

        //lead
        public static final TagKey<Item> STORAGE_BLOCKS_RAW_LEAD = registerCommon("storage_blocks/raw_lead");
        public static final TagKey<Item> STORAGE_BLOCKS_LEAD = registerCommon("storage_blocks/lead");
        public static final TagKey<Item> RAW_MATERIALS_LEAD = registerCommon("raw_materials/lead");
        public static final TagKey<Item> ORES_LEAD = registerCommon("ores/lead");
        public static final TagKey<Item> INGOTS_LEAD = registerCommon("ingots/lead");

        //iridium
        public static final TagKey<Item> STORAGE_BLOCKS_RAW_IRIDIUM = registerCommon("storage_blocks/raw_iridium");
        public static final TagKey<Item> STORAGE_BLOCKS_IRIDIUM = registerCommon("storage_blocks/iridium");
        public static final TagKey<Item> RAW_MATERIALS_IRIDIUM = registerCommon("raw_materials/iridium");
        public static final TagKey<Item> ORES_IRIDIUM = registerCommon("ores/iridium");
        public static final TagKey<Item> INGOTS_IRIDIUM = registerCommon("ingots/iridium");

        //antimony
        public static final TagKey<Item> STORAGE_BLOCKS_RAW_ANTIMONY = registerCommon("storage_blocks/raw_antimony");
        public static final TagKey<Item> STORAGE_BLOCKS_ANTIMONY = registerCommon("storage_blocks/antimony");
        public static final TagKey<Item> RAW_MATERIALS_ANTIMONY = registerCommon("raw_materials/antimony");
        public static final TagKey<Item> ORES_ANTIMONY = registerCommon("ores/antimony");
        public static final TagKey<Item> INGOTS_ANTIMONY = registerCommon("ingots/antimony");

        //adamantite
        public static final TagKey<Item> STORAGE_BLOCKS_RAW_ADAMANTITE = registerCommon("storage_blocks/raw_adamantite");
        public static final TagKey<Item> STORAGE_BLOCKS_ADAMANTITE = registerCommon("storage_blocks/adamantite");
        public static final TagKey<Item> RAW_MATERIALS_ADAMANTITE = registerCommon("raw_materials/adamantite");
        public static final TagKey<Item> ORES_ADAMANTITE = registerCommon("ores/adamantite");
        public static final TagKey<Item> INGOTS_ADAMANTITE = registerCommon("ingots/adamantite");

        private static TagKey<Item> registerCommon(String path) {
            return register(common(path));
        }

        private static TagKey<Item> registerAlloyForgery(String path) {
            return register(alloyForgery(path));
        }

        private static TagKey<Item> register(ResourceLocation id) {
            return TagKey.create(Registries.ITEM, id);
        }
    }

    private static ResourceLocation common(String path) {
        return ResourceLocation.fromNamespaceAndPath(COMMON_NAMESPACE, path);
    }

    private static ResourceLocation alloyForgery(String path) {
        return AlloyForgery.id(path);
    }
}
