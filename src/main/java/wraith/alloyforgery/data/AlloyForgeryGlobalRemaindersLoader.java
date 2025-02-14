package wraith.alloyforgery.data;


import com.google.gson.*;
import com.mojang.logging.LogUtils;
import io.wispforest.endec.Endec;
import io.wispforest.endec.format.gson.GsonDeserializer;
import io.wispforest.owo.serialization.CodecUtils;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import wraith.alloyforgery.AlloyForgery;
import wraith.alloyforgery.recipe.AlloyForgeRecipe;
import java.util.HashMap;
import java.util.Map;

public class AlloyForgeryGlobalRemaindersLoader extends SimpleJsonResourceReloadListener implements IdentifiableResourceReloadListener {

    private static final Endec<ItemStack> RECIPE_RESULT_ENDEC = CodecUtils.toEndec(ItemStack.STRICT_CODEC);

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public AlloyForgeryGlobalRemaindersLoader() {
        super(GSON, "forge_remainder");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> prepared, ResourceManager manager, ProfilerFiller profiler) {
        prepared.forEach((identifier, jsonElement) -> {
            try {
                if (jsonElement instanceof JsonObject jsonObject) {
                    var remainders = new HashMap<Item, ItemStack>();

                    for (var remainderEntry : GsonHelper.getAsJsonObject(jsonObject, "remainders").entrySet()) {
                        var item = GsonHelper.convertToItem(new JsonPrimitive(remainderEntry.getKey()), remainderEntry.getKey()).value();

                        if (remainderEntry.getValue().isJsonObject()) {
                            var remainderStack = RECIPE_RESULT_ENDEC.decodeFully(GsonDeserializer::of, remainderEntry.getValue().getAsJsonObject());
                            remainders.put(item, remainderStack);
                        } else {
                            var remainderItem = GsonHelper.convertToItem(remainderEntry.getValue(), "item").value();
                            remainders.put(item, new ItemStack(remainderItem));
                        }
                    }

                    AlloyForgeRecipe.addRemainders(remainders);
                } else {
                    throw new JsonSyntaxException("Expected alloy forge remainders definition to be a json object");
                }
            } catch (IllegalArgumentException | JsonParseException exception) {
                LOGGER.error("[AlloyForgeRemainders]: Parsing error loading recipe {}", identifier, exception);
            }
        });
    }

    @Override
    public ResourceLocation getFabricId() {
        return ResourceLocation.fromNamespaceAndPath(AlloyForgery.MOD_ID, "forge_remainder");
    }
}
