package wraith.alloyforgery.mixin;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import wraith.alloyforgery.data.RecipeTagLoader;
import wraith.alloyforgery.pond.RecipeTagHelper;

@Mixin(RecipeHolder.class)
public abstract class RecipeEntryMixin implements RecipeTagHelper {
    @Override
    public boolean isIn(ResourceLocation tag) {
        return RecipeTagLoader.isWithinTag(tag, ((RecipeHolder<Recipe<?>>) (Object) this));
    }
}
