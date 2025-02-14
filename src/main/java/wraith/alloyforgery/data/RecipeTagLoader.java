package wraith.alloyforgery.data;

import io.wispforest.owo.network.ClientAccess;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.tags.TagLoader;
import net.minecraft.server.packs.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.ProfilerFiller;
import wraith.alloyforgery.AlloyForgery;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tag Loader used to load Recipe Based tags with the resolving
 * process being delayed till Data Pack load has ended
 */
public class RecipeTagLoader extends SimplePreparableReloadListener<Map<ResourceLocation, List<TagLoader.EntryWithSource>>> implements IdentifiableResourceReloadListener, ServerLifecycleEvents.ServerStarted, ServerLifecycleEvents.EndDataPackReload {

    private static final Map<ResourceLocation, Set<ResourceLocation>> RESOLVED_ENTRIES = new HashMap<>();

    private static final Map<ResourceLocation, List<TagLoader.EntryWithSource>> RAW_TAG_DATA = new HashMap<>();

    private final DelayedTagGroupLoader<RecipeHolder<Recipe<?>>> tagGroupLoader = new DelayedTagGroupLoader<>("tags/recipe");

    @Override
    protected Map<ResourceLocation, List<TagLoader.EntryWithSource>> prepare(ResourceManager manager, ProfilerFiller profiler) {
        return this.tagGroupLoader.load(manager);
    }

    @Override
    protected void apply(Map<ResourceLocation, List<TagLoader.EntryWithSource>> prepared, ResourceManager manager, ProfilerFiller profiler) {
        RAW_TAG_DATA.clear();

        RAW_TAG_DATA.putAll(prepared);
    }

    @Override
    public ResourceLocation getFabricId() {
        return AlloyForgery.id("recipe_tag");
    }

    //--

    /**
     * @param tag   ResourceLocation for the given Tag
     * @param entry Recipe Entry to check
     * @return true if the tag exists and if the given entry exists within the Tag group
     */
    public static boolean isWithinTag(ResourceLocation tag, RecipeHolder<?> entry) {
        return isWithinTag(tag, entry.id());
    }

    /**
     * @param tag      ResourceLocation for the given Tag
     * @param recipeID Recipe identifier
     * @return true if the tag exists and if the given entry exists within the Tag group
     */
    public static boolean isWithinTag(ResourceLocation tag, ResourceLocation recipeID) {
        if (!RESOLVED_ENTRIES.containsKey(tag)) return false;

        return RESOLVED_ENTRIES.get(tag).contains(recipeID);
    }

    //--

    public void initEvents() {
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(this);
        ServerLifecycleEvents.SERVER_STARTED.register(this);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            AlloyForgery.CHANNEL.serverHandle(handler.player).send(TagPacket.of(RESOLVED_ENTRIES));
        });
    }

    @Override
    public void endDataPackReload(MinecraftServer server, CloseableResourceManager resourceManager, boolean success) {
        if (!success) return;

        resolveEntries(server);

        AlloyForgery.CHANNEL.serverHandle(server).send(TagPacket.of(RESOLVED_ENTRIES));
    }

    @Override
    public void onServerStarted(MinecraftServer server) {
        resolveEntries(server);
    }

    public void resolveEntries(MinecraftServer server) {
        var recipeManager = server.getRecipeManager();

        Map<ResourceLocation, Collection<RecipeHolder<Recipe<?>>>> map = tagGroupLoader.setGetter(identifier -> {
                    return Optional.ofNullable((RecipeHolder<Recipe<?>>) recipeManager.byKey(identifier).orElse(null));
                })
                .build(RAW_TAG_DATA);

        RESOLVED_ENTRIES.clear();

        map.forEach((id, recipes) -> RESOLVED_ENTRIES.put(id, recipes.stream().map(RecipeHolder::id).collect(Collectors.toSet())));
    }

    // Packet that acts as a sync packet for the Recipe Based Tag Entries
    public record TagPacket(List<TagEntry> entries) {
        public static TagPacket of(Map<ResourceLocation, Set<ResourceLocation>> tagEntries) {
            return new TagPacket(tagEntries.entrySet().stream()
                    .map(entry -> new TagEntry(entry.getKey(), List.copyOf(entry.getValue())))
                    .toList());
        }

        public static void handlePacket(TagPacket packet, ClientAccess access) {
            RESOLVED_ENTRIES.clear();

            RESOLVED_ENTRIES.putAll(
                    packet.entries.stream().collect(Collectors.toMap(TagEntry::id, e -> new HashSet<>(e.entries())))
            );
        }
    }

    public record TagEntry(ResourceLocation id, List<ResourceLocation> entries) {
    }

    ;

    //--
}
