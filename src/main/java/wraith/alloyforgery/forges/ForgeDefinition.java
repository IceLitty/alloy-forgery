package wraith.alloyforgery.forges;

import com.google.common.collect.ImmutableList;
import com.google.gson.*;
import io.wispforest.endec.Endec;
import io.wispforest.owo.registration.ComplexRegistryAction;
import io.wispforest.owo.registration.RegistryHelper;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import java.util.ArrayList;

public record ForgeDefinition(int forgeTier,
                              float speedMultiplier,
                              int fuelCapacity,
                              int maxSmeltTime,
                              Block material,
                              ImmutableList<Block> additionalMaterials) {

    public static final int BASE_MAX_SMELT_TIME = 200;
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

    private ForgeDefinition(int forgeTier, float speedMultiplier, int fuelCapacity, Block material, ImmutableList<Block> additionalMaterials) {
        this(forgeTier, speedMultiplier, fuelCapacity, (int) (BASE_MAX_SMELT_TIME / speedMultiplier), material, additionalMaterials);
    }

    public static final Endec<Block> BLOCK_ENDEC = MinecraftEndecs.ofRegistry(BuiltInRegistries.BLOCK);

    public static Endec<ForgeDefinition> FORGE_DEFINITION = MinecraftEndecs.IDENTIFIER.xmap(
            identifier -> {
                return ForgeRegistry.getForgeDefinition(identifier)
                        .orElseThrow(() -> new IllegalStateException("Unable to locate ForgerDefinition with ResourceLocation: [ID: " + identifier + "]"));
            }, forgeDefinition -> {
                for (var entry : ForgeRegistry.getForgeEntries()) {
                    if(entry.getValue() == forgeDefinition) return entry.getKey();
                }

                throw new IllegalStateException("A Given forge Definition was not found within the ForgeRegistry!");
            }
    );


    public static void loadAndEnqueue(ResourceLocation id, JsonObject json) {
        final int forgeTier = GsonHelper.getAsInt(json, "tier");
        final float speedMultiplier = GsonHelper.getAsFloat(json, "speed_multiplier", 1);
        final int fuelCapacity = GsonHelper.getAsInt(json, "fuel_capacity", 48000);

        final var mainMaterialId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "material"));

        final var additionalMaterialIds = new ArrayList<ResourceLocation>();
        GsonHelper.getAsJsonArray(json, "additional_materials", new JsonArray()).forEach(jsonElement -> additionalMaterialIds.add(ResourceLocation.tryParse(jsonElement.getAsString())));

        final var action = ComplexRegistryAction.Builder.create(() -> {
            final var mainMaterial = BuiltInRegistries.BLOCK.get(mainMaterialId);
            final var additionalMaterialsBuilder = new ImmutableList.Builder<Block>();
            additionalMaterialIds.forEach(identifier -> additionalMaterialsBuilder.add(BuiltInRegistries.BLOCK.get(identifier)));

            final var definition = new ForgeDefinition(forgeTier, speedMultiplier, fuelCapacity, mainMaterial, additionalMaterialsBuilder.build());

            ForgeRegistry.registerDefinition(id, definition);
        }).entry(mainMaterialId).entries(additionalMaterialIds).build();

        RegistryHelper.get(BuiltInRegistries.BLOCK).runWhenPresent(action);
    }

    public boolean isBlockValid(Block block) {
        return block == material || this.additionalMaterials.contains(block);
    }

    public JsonElement generateRecipe(ResourceLocation id) {
        String recipe = RECIPE_PATTERN.replace("{material}", BuiltInRegistries.ITEM.getKey(material.asItem()).toString());
        recipe = recipe.replace("{controller}", BuiltInRegistries.ITEM.getKey(ForgeRegistry.getControllerBlock(id).get().asItem()).toString());

        return ForgeRegistry.GSON.fromJson(recipe, JsonObject.class);
    }

    @Override
    public String toString() {
        return "ForgeDefinition{" +
                "forgeTier=" + forgeTier +
                ", speedMultiplier=" + speedMultiplier +
                ", fuelCapacity=" + fuelCapacity +
                ", maxSmeltTime=" + maxSmeltTime +
                ", material=" + material +
                ", additionalMaterials=" + additionalMaterials +
                '}';
    }
}
