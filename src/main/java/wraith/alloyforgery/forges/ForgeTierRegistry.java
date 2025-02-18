package wraith.alloyforgery.forges;

import io.wispforest.endec.Endec;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import wraith.alloyforgery.AlloyForgery;
import wraith.alloyforgery.networking.AlloyForgeNetworking;
import wraith.alloyforgery.networking.TierDataSync;
import wraith.alloyforgery.utils.data.EndecableDataLoader;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ForgeTierRegistry {

    private static final ForgeTierRegistry SERVER = new ForgeTierRegistry();
    private static final ForgeTierRegistry CLIENT = new ForgeTierRegistry();

    private static final EndecableDataLoader TIER_DATA_LOADER = EndecableDataLoader.of(
            AlloyForgery.id("forge_tier"),
            "alloy_forge/tier",
            ForgeTier.ENDEC,
            (identifier, forgeTier) -> {
                SERVER.idToForgeTier.put(identifier, forgeTier);
                SERVER.forgeTierToId.put(forgeTier, identifier);
            });

    private static final EndecableDataLoader TIER_BINDING_LOADER = EndecableDataLoader.of(
            AlloyForgery.id("tier_binding"),
            "alloy_forge/tier_binding",
            Endec.map(ResourceLocation::toString, ResourceLocation::tryParse, MinecraftEndecs.IDENTIFIER),
            (identifier, map) -> map.forEach(SERVER.forgeDefinitionToTier::putIfAbsent)
    ).addDependencies(TIER_DATA_LOADER.getFabricId());

    public static void initDataLoaders() {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(TIER_DATA_LOADER);
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(TIER_BINDING_LOADER);

        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> {
            AlloyForgeNetworking.CHANNEL.serverHandle(player).send(new TierDataSync(SERVER.idToForgeTier(), SERVER.forgeDefinitionToTier()));
        });
    }

    private final Map<ResourceLocation, ForgeTier> idToForgeTier = new HashMap<>();
    private final Map<ForgeTier, ResourceLocation> forgeTierToId = new HashMap<>();

    private final Map<ResourceLocation, ResourceLocation> forgeDefinitionToTier = new HashMap<>();

    public static ForgeTierRegistry getForgeRegistry(boolean isClientSide) {
        return (isClientSide) ? CLIENT : SERVER;
    }

    @Nullable
    public static ResourceLocation getForgeTierId(boolean isClientSide, ForgeTier forgeTier) {
        return getForgeRegistry(isClientSide).forgeTierToId().get(forgeTier);
    }

    @Nullable
    public static ForgeTier getForgeTier(boolean isClientSide, ResourceLocation tierId) {
        return getForgeRegistry(isClientSide).idToForgeTier().get(tierId);
    }

    @Nullable
    public ForgeTier getForgeTier(ForgeDefinition forgeDefinition) {
        return forgeDefinition.id()
                .map(forgeDefinitionToTier()::get)
                .map(idToForgeTier()::get)
                .orElse(null);
    }

    public Map<ResourceLocation, ForgeTier> idToForgeTier() {
        return Collections.unmodifiableMap(idToForgeTier);
    }

    public Map<ForgeTier, ResourceLocation> forgeTierToId() {
        return Collections.unmodifiableMap(forgeTierToId);
    }

    public Map<ResourceLocation, ResourceLocation> forgeDefinitionToTier() {
        return Collections.unmodifiableMap(forgeDefinitionToTier);
    }

    @ApiStatus.Internal
    public void forgeDefinitionBindings(Map<ResourceLocation, ResourceLocation> forgeDefinitionToTier) {
        this.forgeDefinitionToTier.clear();
        this.forgeDefinitionToTier.putAll(forgeDefinitionToTier);
    }

    @ApiStatus.Internal
    public void setTierInfo(Map<ResourceLocation, ForgeTier> idToForgeTier) {
        this.idToForgeTier.clear();
        this.idToForgeTier.putAll(idToForgeTier);

        this.forgeTierToId.clear();

        idToForgeTier.forEach((identifier, forgeTier) -> this.forgeTierToId.put(forgeTier, identifier));
    }
}
