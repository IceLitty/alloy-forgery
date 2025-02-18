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
import org.jetbrains.annotations.Nullable;
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

    private static final Map<ResourceLocation, ForgeDefinition> ID_TO_FORGE_DEFINITION = new HashMap<>();
    private static final Map<ForgeDefinition, ResourceLocation> FORGE_DEFINITION_TO_ID = new HashMap<>();

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
        return ID_TO_FORGE_DEFINITION.containsKey(id) ? Optional.of(ID_TO_FORGE_DEFINITION.get(id)) : Optional.empty();
    }

    public static Optional<Block> getControllerBlock(ResourceLocation id) {
        return ID_TO_FORGE_DEFINITION.containsKey(id) ? Optional.of(CONTROLLER_BLOCK_REGISTRY.get(id)) : Optional.empty();
    }

    public static Optional<ResourceLocation> getId(ForgeDefinition definition) {
        return FORGE_DEFINITION_TO_ID.containsKey(definition) ? Optional.of(FORGE_DEFINITION_TO_ID.get(definition)) : Optional.empty();
    }

    public static Set<Map.Entry<ResourceLocation, ForgeDefinition>> getForgeEntries(){
        return ID_TO_FORGE_DEFINITION.entrySet();
    }

    public static Set<ResourceLocation> getForgeIds() {
        return ID_TO_FORGE_DEFINITION.keySet();
    }

    public static List<Block> getControllerBlocks() {
        return CONTROLLER_BLOCK_REGISTRY.values().stream().toList();
    }

    private static void store(ResourceLocation id, ForgeDefinition definition, ForgeControllerBlock block) {
        FORGE_DEFINITION_TO_ID.put(definition, id);
        ID_TO_FORGE_DEFINITION.put(id, definition);
        CONTROLLER_BLOCK_REGISTRY.put(id, block);
        AlloyForgery.FORGE_CONTROLLER_BLOCK_ENTITY_BLOCK_LIST.add(block);
    }
}
