package wraith.alloyforgery.compat;

import net.minecraft.resources.ResourceLocation;
import wraith.alloyforgery.AlloyForgery;
import java.util.HashMap;
import java.util.Map;

public class LegacyIdMappings {

    private static final Map<ResourceLocation, ResourceLocation> MAPPINGS = new HashMap<>();

    static {
        MAPPINGS.put(id("blackstone_forge_controller"), id("polished_blackstone_forge_controller"));
        MAPPINGS.put(id("brick_forge_controller"), id("bricks_forge_controller"));
        MAPPINGS.put(id("deepslate_forge_controller"), id("deepslate_bricks_forge_controller"));
        MAPPINGS.put(id("end_stone_forge_controller"), id("end_stone_bricks_forge_controller"));
        MAPPINGS.put(id("stone_brick_forge_controller"), id("stone_bricks_forge_controller"));
    }

    public static ResourceLocation remap(ResourceLocation original) {
        return MAPPINGS.getOrDefault(original, original);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(AlloyForgery.MOD_ID, path);
    }
}
