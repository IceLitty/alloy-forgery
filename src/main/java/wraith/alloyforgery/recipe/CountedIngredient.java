package wraith.alloyforgery.recipe;


import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.minecraft.world.item.crafting.Ingredient;
import wraith.alloyforgery.utils.EndecUtils;

public record CountedIngredient(Ingredient ingredient, int count) {
    public static final Endec<CountedIngredient> ENDEC = StructEndecBuilder.of(
        EndecUtils.STRUCT_INGREDIENT.flatFieldOf(CountedIngredient::ingredient),
        Endec.INT.optionalFieldOf("count", CountedIngredient::count, 1),
        CountedIngredient::new
    );
}
