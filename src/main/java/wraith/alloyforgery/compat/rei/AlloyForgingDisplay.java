package wraith.alloyforgery.compat.rei;

import com.google.common.collect.ImmutableMap;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import io.wispforest.owo.serialization.format.nbt.NbtDeserializer;
import io.wispforest.owo.serialization.format.nbt.NbtSerializer;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.resources.ResourceLocation;
import wraith.alloyforgery.recipe.AlloyForgeRecipe;
import java.util.*;

public class AlloyForgingDisplay implements Display {

    private final List<EntryIngredient> inputs;
    private final EntryIngredient output;

    public final int minForgeTier;
    public final int requiredFuel;

    public final Map<AlloyForgeRecipe.OverrideRange, ItemStack> overrides;

    public final Optional<ResourceLocation> recipeID;

    private AlloyForgingDisplay(List<EntryIngredient> inputs, EntryIngredient output, int minForgeTier, int requiredFuel, Map<AlloyForgeRecipe.OverrideRange, ItemStack> overrides, Optional<ResourceLocation> recipeID) {
        this.inputs = inputs;
        this.output = output;

        this.minForgeTier = minForgeTier;
        this.requiredFuel = requiredFuel;

        this.overrides = overrides;

        this.recipeID = recipeID;
    }

    public static AlloyForgingDisplay of(RecipeHolder<AlloyForgeRecipe> recipeEntry) {
        List<EntryIngredient> convertedInputs = new ArrayList<>();

        var recipe = recipeEntry.value();

        for (Map.Entry<Ingredient, Integer> entry : recipe.getIngredientsMap().entrySet()) {
            for (int i = entry.getValue(); i > 0; ) {
                int stackCount = Math.min(i, 64);

                convertedInputs.add(
                        EntryIngredients.ofItemStacks(Arrays.stream(entry.getKey().getItems())
                                .map(ItemStack::copy)
                                .peek(stack -> stack.setCount(stackCount))
                                .toList()));

                i -= stackCount;
            }
        }

        return new AlloyForgingDisplay(
                convertedInputs,
                EntryIngredients.of(recipe.getBaseResult()),
                recipe.getMinForgeTier(),
                recipe.getFuelPerTick(),
                recipe.getTierOverrides(),
                recipe.secondaryID().or(() -> Optional.of(recipeEntry.id())));
    }

    @Override
    public List<EntryIngredient> getInputEntries() {
        return inputs;
    }

    @Override
    public List<EntryIngredient> getOutputEntries() {
        return Collections.singletonList(output);
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return AlloyForgeryCommonPlugin.ID;
    }

    @Override
    public Optional<ResourceLocation> getDisplayLocation() {
        return recipeID;
    }

    public enum Serializer implements DisplaySerializer<AlloyForgingDisplay> {

        INSTANCE;

        @Override
        public CompoundTag save(CompoundTag tag, AlloyForgingDisplay display) {
            // Store the fuel per tick
            tag.putInt("fuel_per_tick", display.requiredFuel);

            // Save the minimum Forge Tier
            tag.putInt("min_forge_tier", display.minForgeTier);

            // Store the recipe inputs
            ListTag inputs = new ListTag();
            inputs.addAll(display.inputs.stream().map(EntryIngredient::saveIngredient).toList());
            tag.put("inputs", inputs);

            // Store the recipe output
            tag.put("output", display.output.saveIngredient());

            ListTag overrides = new ListTag();
            display.overrides.forEach((overrideRange, itemStack) -> {
                CompoundTag overrideTag = new CompoundTag();

                overrideTag.putInt("lower", overrideRange.lowerBound());
                overrideTag.putInt("upper", overrideRange.upperBound());
                overrideTag.put("stack", MinecraftEndecs.ITEM_STACK.encodeFully(NbtSerializer::of, itemStack));

                overrides.add(overrideTag);
            });
            tag.put("overrides", overrides);

            display.recipeID.ifPresent(id -> tag.putString("recipeID", id.toString()));

            return tag;
        }

        @Override
        public AlloyForgingDisplay read(CompoundTag tag) {
            // Get the fuel per tick
            int requiredFuel = tag.getInt("fuel_per_tick");

            // Get the minimum Forge Tier
            int minForgeTier = tag.getInt("fuel_per_tick");

            // We get a list of all the recipe inputs
            List<EntryIngredient> input = new ArrayList<>();
            tag.getList("inputs", Tag.TAG_LIST).forEach(nbtElement -> input.add(EntryIngredient.read((ListTag) nbtElement)));

            // We get the single recipe output
            EntryIngredient output = EntryIngredient.read(tag.getList("output", Tag.TAG_LIST));

            // Last thing we grab is the recipes Override Range Values
            ImmutableMap.Builder<AlloyForgeRecipe.OverrideRange, ItemStack> builder = new ImmutableMap.Builder<>();
            tag.getList("overrides", Tag.TAG_COMPOUND).forEach(nbtElement -> {
                CompoundTag overrideTag = (CompoundTag) nbtElement;

                AlloyForgeRecipe.OverrideRange range = new AlloyForgeRecipe.OverrideRange(overrideTag.getInt("lower"), overrideTag.getInt("upper"));
                ItemStack stack = MinecraftEndecs.ITEM_STACK.decodeFully(NbtDeserializer::of, overrideTag.getCompound("stack"));

                builder.put(range, stack);
            });

            var recipeID = tag.contains("recipeID") ? ResourceLocation.tryParse(tag.getString("recipeID")) : null;

            return new AlloyForgingDisplay(input, output, minForgeTier, requiredFuel, builder.build(), Optional.ofNullable(recipeID));
        }
    }
}
