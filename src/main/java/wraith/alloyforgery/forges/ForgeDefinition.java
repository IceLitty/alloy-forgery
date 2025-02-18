package wraith.alloyforgery.forges;

import com.google.common.collect.ImmutableList;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.format.gson.GsonEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.moddata.ModDataLoader;
import io.wispforest.owo.registration.ComplexRegistryAction;
import io.wispforest.owo.registration.RegistryHelper;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.world.level.block.Block;
import io.wispforest.endec.Endec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import wraith.alloyforgery.AlloyForgery;
import wraith.alloyforgery.utils.RecipeInjector;
import wraith.alloyforgery.utils.data.EndecableModDataLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record ForgeDefinition(Block material, ImmutableList<Block> additionalMaterials, boolean blockEntity) {

    public ForgeDefinition(Block material, ImmutableList<Block> additionalMaterials) {
        this(material, additionalMaterials, false);
    }

    public static Endec<ForgeDefinition> FORGE_DEFINITION = MinecraftEndecs.IDENTIFIER.xmap(
            identifier -> {
                return ForgeRegistry.getForgeDefinition(identifier)
                        .orElseThrow(() -> new IllegalStateException("Unable to locate ForgerDefinition with ResourceLocation: [ID: " + identifier + "]"));
            }, forgeDefinition -> {
                return forgeDefinition.id()
                        .orElseThrow(() -> new IllegalStateException("A Given forge Definition was not found within the ForgeRegistry!"));
            }
    );

    public Optional<ResourceLocation> id() {
        return ForgeRegistry.getId(this);
    }

    @Deprecated
    public static void loadAndEnqueue(ResourceLocation id, JsonObject json) {
        final int forgeTier = GsonHelper.getAsInt(json, "tier");
        final float speedMultiplier = GsonHelper.getAsFloat(json, "speed_multiplier", 1);
        final int fuelCapacity = GsonHelper.getAsInt(json, "fuel_capacity", 48000);

        // TODO: ADD DEPRECATION WARNING ABOUT LOADING TIER INFO
        var tier = new ForgeTier(forgeTier, speedMultiplier, fuelCapacity, Optional.empty());

        final var mainMaterialId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "material"));

        final var additionalMaterialIds = new ArrayList<ResourceLocation>();
        GsonHelper.getAsJsonArray(json, "additional_materials", new JsonArray()).forEach(jsonElement -> additionalMaterialIds.add(ResourceLocation.tryParse(jsonElement.getAsString())));

        loadAndEnqueue(id, new RawForgeDefinition(mainMaterialId, additionalMaterialIds, false));
    }

    private static void loadAndEnqueue(ResourceLocation id, RawForgeDefinition rawForgeDefinition) {
        final var action = ComplexRegistryAction.Builder.create(() -> {
            final var mainMaterial = BuiltInRegistries.BLOCK.get(rawForgeDefinition.materialId());
            final var additionalMaterialsBuilder = new ImmutableList.Builder<Block>();
            rawForgeDefinition.additionalMaterialIds().forEach(identifier -> additionalMaterialsBuilder.add(BuiltInRegistries.BLOCK.get(identifier)));

            final var definition = new ForgeDefinition(mainMaterial, additionalMaterialsBuilder.build());

            ForgeRegistry.registerDefinition(id, definition);
        }).entries(rawForgeDefinition.blockIds()).build();

        RegistryHelper.get(BuiltInRegistries.BLOCK).runWhenPresent(action);
    }

    public boolean isBlockValid(Block block) {
        return block == material || this.additionalMaterials.contains(block);
    }

    //why kubejs why
    private static final String RECIPE_PATTERN =
            """
                    {
                        "type": "minecraft:crafting_shaped",
                        "pattern": [
                            "###",
                            "#B#",
                            "###"
                        ],
                        "key": {
                            "#": {
                                "item": "{material}"
                            },
                            "B": {
                                "item": "minecraft:blast_furnace"
                            }
                        },
                        "result": {
                            "id": "{controller}",
                            "count": 1
                        }
                    }
                    """;

    public JsonElement generateRecipe(ResourceLocation id) {
        String recipe = RECIPE_PATTERN.replace("{material}", BuiltInRegistries.ITEM.getKey(material.asItem()).toString());
        recipe = recipe.replace("{controller}", BuiltInRegistries.ITEM.getKey(ForgeRegistry.getControllerBlock(id).get().asItem()).toString());

        return ForgeRegistry.GSON.fromJson(recipe, JsonObject.class);
    }

    public static void initLoaders() {
        EndecableModDataLoader.of(
                AlloyForgery.id("old_forge_definition_loader"),
                "alloy_forges",
                GsonEndec.INSTANCE.xmap(JsonElement::getAsJsonObject, jsonObject -> jsonObject),
                ForgeDefinition::loadAndEnqueue
        ).load();

        EndecableModDataLoader.of(
                AlloyForgery.id("forge_definition_loader"),
                "alloy_forge/forge",
                RawForgeDefinition.ENDEC,
                ForgeDefinition::loadAndEnqueue
        ).load();

        RecipeInjector.ADD_RECIPES.register(instance -> {
            for (var forgeEntry : ForgeRegistry.getForgeEntries()) {
                var id = forgeEntry.getKey();

                var recipe = RecipeSerializer.SHAPED_RECIPE.codec()
                        .codec()
                        .decode(JsonOps.INSTANCE, forgeEntry.getValue().generateRecipe(id))
                        .getOrThrow(string -> new IllegalStateException("Unable to generate recipe for given ForgeDefinition [" + id + "]: " + string))
                        .getFirst();

                instance.addRecipe(id.withSuffix("_recipe"), recipe);
            }
        });
    }

    private record RawForgeDefinition(ResourceLocation materialId, List<ResourceLocation> additionalMaterialIds, boolean isBlockEntity) {
        public static final StructEndec<RawForgeDefinition> ENDEC = StructEndecBuilder.of(
                MinecraftEndecs.IDENTIFIER.fieldOf("material", RawForgeDefinition::materialId),
                MinecraftEndecs.IDENTIFIER.listOf().optionalFieldOf("additional_materials", RawForgeDefinition::additionalMaterialIds, List.of()),
                Endec.BOOLEAN.optionalFieldOf("is_block_entity", RawForgeDefinition::isBlockEntity, false),
                RawForgeDefinition::new
        );

        public List<ResourceLocation> blockIds() {
            var list = new ArrayList<>(additionalMaterialIds);
            list.addFirst(materialId);
            return list;
        }
    }

    @Override
    public String toString() {
        return "ForgeDefinition{" +
                "material=" + material +
                ", additionalMaterials=" + additionalMaterials +
                '}';
    }
}
