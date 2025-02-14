package wraith.alloyforgery.mixin;

import com.google.gson.JsonElement;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wraith.alloyforgery.forges.ForgeRegistry;
import java.util.Map;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin {

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("HEAD"))
    public void injectForgeRecipes(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci) {
        for (var id : ForgeRegistry.getForgeIds()) {
            final var forgeDefinition = ForgeRegistry.getForgeDefinition(id).get();
            map.putIfAbsent(id, forgeDefinition.generateRecipe(id));
        }
    }

}
