package wraith.alloyforgery.mixin;

import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wraith.alloyforgery.AlloyForgery;
import wraith.alloyforgery.recipe.AlloyForgeRecipe;
import java.util.HashMap;

@Mixin(ReloadableServerResources.class)
public abstract class DataPackContentsMixin {

    @Shadow @Final private RecipeManager recipes;
    @Shadow @Final private ReloadableServerRegistries.Holder fullRegistryHolder;

    @Inject(method = "updateRegistryTags", at = @At("TAIL"))
    private void alloy_forgery$onRefresh(CallbackInfo ci) {
        var recipeEntries = recipes.getAllRecipesFor(AlloyForgeRecipe.Type.INSTANCE);

        var map = new HashMap<AlloyForgeRecipe, ResourceLocation>();

        for (var entry : recipeEntries) {
            map.put(entry.value(), entry.id());
        }

        AlloyForgeRecipe.PENDING_RECIPES.forEach((recipe, pendingRecipeData) -> recipe.finishRecipe(this.fullRegistryHolder.get(), pendingRecipeData, key -> map.getOrDefault(key, ResourceLocation.fromNamespaceAndPath(AlloyForgery.MOD_ID, "unknown_recipe"))));

        AlloyForgeRecipe.PENDING_RECIPES.clear();
    }
}
