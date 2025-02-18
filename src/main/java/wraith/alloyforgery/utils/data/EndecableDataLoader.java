package wraith.alloyforgery.utils.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.BiConsumer;

public class EndecableDataLoader extends SimpleJsonResourceReloadListener implements IdentifiableResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().setLenient().create();

    private final ResourceLocation id;

    private final Set<ResourceLocation> dependencies = new HashSet<>();

    private final EndecedHandler<?> handler;

    private EndecableDataLoader(ResourceLocation id, String dataType, EndecedHandler<?> handler) {
        super(GSON, dataType);

        this.id = id;
        this.handler = handler;
    }

    public static <T> EndecableDataLoader of(ResourceLocation id, String dataType, Endec<T> endec, BiConsumer<ResourceLocation, T> consumer) {
        return new EndecableDataLoader(id, dataType, new EndecedHandler<>(endec, consumer));
    }

    public static <T> EndecableDataLoader of(ResourceLocation id, String dataType, String fieldName, Endec<T> endec, BiConsumer<ResourceLocation, T> consumer) {
        return new EndecableDataLoader(id, dataType,
                EndecedHandler.of(fieldName, endec, consumer, entryId -> {
                    LOGGER.warn("A given entry within the [{}] Data Loader was found to be missing any data! [EntryId: {}]", id, entryId);
                }));
    }

    public EndecableDataLoader addDependencies(ResourceLocation ...dependencies) {
        this.dependencies.addAll(Set.of(dependencies));

        return this;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> prepared, ResourceManager manager, ProfilerFiller profiler) {
        prepared.forEach((identifier, jsonElement) -> {
            try {
                handler.handle(identifier, jsonElement);
            } catch (JsonSyntaxException e){
                LOGGER.error("An error has occurred during [{}] stage:", id, e);
            }
        });
    }

    @Override
    public ResourceLocation getFabricId() {
        return id;
    }

    @Override
    public Collection<ResourceLocation> getFabricDependencies() {
        return this.dependencies;
    }
}
