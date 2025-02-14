package wraith.alloyforgery.pond;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.resources.ResourceLocation;
import wraith.alloyforgery.data.RecipeTagLoader;
import wraith.alloyforgery.mixin.RecipeEntryMixin;

/**
 * Helper interface injected into {@link Recipe} through {@link RecipeEntryMixin}
 * to implement Tag check call within {@link RecipeTagLoader}
 */
public interface RecipeTagHelper {

    default boolean isIn(ResourceLocation tag) {
        throw new UnsupportedOperationException("RecipeTagHelper 'isIn' method not implememnted!");
    }
}
