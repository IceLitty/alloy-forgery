package wraith.alloyforgery.mixin;

import com.google.common.collect.Multimap;
import net.minecraft.world.item.crafting.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Map;

@Mixin(RecipeManager.class)
public interface RecipeManagerAccessor {
    @Accessor("byType")
    Multimap<RecipeType<?>, RecipeHolder<?>> af$getRecipes();

    @Accessor("byType")
    void af$setRecipes(Multimap<RecipeType<?>, RecipeHolder<?>> recipesByType);

    @Accessor("byName")
    Map<ResourceLocation, RecipeHolder<?>> af$getRecipesById();

    @Accessor("byName")
    void af$setRecipesById(Map<ResourceLocation, RecipeHolder<?>> recipesById);

    @Accessor("registries")
    HolderLookup.Provider af$getRegistryLookup();
}
