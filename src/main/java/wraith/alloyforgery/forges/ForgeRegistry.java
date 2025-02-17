package wraith.alloyforgery.forges;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.wispforest.endec.Endec;
import io.wispforest.owo.moddata.ModDataConsumer;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import io.wispforest.owo.util.TagInjector;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import wraith.alloyforgery.AlloyForgery;
import wraith.alloyforgery.ForgeControllerItem;
import wraith.alloyforgery.block.ForgeControllerBlock;
import java.util.*;

public class ForgeRegistry {

    public static Endec<ForgeDefinition> FORGE_DEFINITION = MinecraftEndecs.IDENTIFIER.xmap(
            identifier -> {
                return getForgeDefinition(identifier)
                        .orElseThrow(() -> new IllegalStateException("Unable to locate ForgerDefinition with ResourceLocation: [ID: " + identifier + "]"));
            }, forgeDefinition -> {
                for (var entry : getForgeEntries()) {
                    if(entry.getValue() == forgeDefinition) return entry.getKey();
                }

                throw new IllegalStateException();
            }
    );

    public static final Gson GSON = new Gson();
    private static final ResourceLocation MINEABLE_PICKAXE = ResourceLocation.parse("mineable/pickaxe");

    private static final Map<ResourceLocation, ForgeDefinition> FORGE_DEFINITION_REGISTRY = new HashMap<>();
    private static final Map<ResourceLocation, Block> CONTROLLER_BLOCK_REGISTRY = new HashMap<>();

    static void registerDefinition(ResourceLocation forgeDefinitionId, ForgeDefinition definition) {
        final var controllerBlock = new ForgeControllerBlock(definition);
        final var controllerBlockRegistryId = AlloyForgery.id(BuiltInRegistries.BLOCK.getKey(definition.material()).getPath() + "_forge_controller");

        Registry.register(BuiltInRegistries.BLOCK, controllerBlockRegistryId, controllerBlock);
        Registry.register(BuiltInRegistries.ITEM, controllerBlockRegistryId, new ForgeControllerItem(controllerBlock, new Item.Properties()));

        TagInjector.inject(BuiltInRegistries.BLOCK, MINEABLE_PICKAXE, controllerBlock);

        store(forgeDefinitionId, definition, controllerBlock);
    }

    public static Optional<ForgeDefinition> getForgeDefinition(ResourceLocation id) {
        return FORGE_DEFINITION_REGISTRY.containsKey(id) ? Optional.of(FORGE_DEFINITION_REGISTRY.get(id)) : Optional.empty();
    }

    public static Optional<Block> getControllerBlock(ResourceLocation id) {
        return FORGE_DEFINITION_REGISTRY.containsKey(id) ? Optional.of(CONTROLLER_BLOCK_REGISTRY.get(id)) : Optional.empty();
    }

    public static Set<Map.Entry<ResourceLocation, ForgeDefinition>> getForgeEntries(){
        return FORGE_DEFINITION_REGISTRY.entrySet();
    }

    public static Set<ResourceLocation> getForgeIds() {
        return FORGE_DEFINITION_REGISTRY.keySet();
    }

    public static List<Block> getControllerBlocks() {
        return CONTROLLER_BLOCK_REGISTRY.values().stream().toList();
    }

    private static void store(ResourceLocation id, ForgeDefinition definition, ForgeControllerBlock block) {
        FORGE_DEFINITION_REGISTRY.put(id, definition);
        CONTROLLER_BLOCK_REGISTRY.put(id, block);
        AlloyForgery.FORGE_CONTROLLER_BLOCK_ENTITY_BLOCK_LIST.add(block);
    }

    public static final class Loader implements ModDataConsumer {

        public static final Loader INSTANCE = new Loader();

        private Loader() {
        }

        @Override
        public String getDataSubdirectory() {
            return "alloy_forges";
        }

        @Override
        public void acceptParsedFile(ResourceLocation id, JsonObject object) {
            ForgeDefinition.loadAndEnqueue(id, object);
        }
    }

}
