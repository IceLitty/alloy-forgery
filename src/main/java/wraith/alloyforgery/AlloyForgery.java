package wraith.alloyforgery;

import io.wispforest.owo.moddata.ModDataLoader;
import io.wispforest.owo.network.OwoNetChannel;
import io.wispforest.owo.particles.ClientParticles;
import io.wispforest.owo.particles.systems.ParticleSystem;
import io.wispforest.owo.particles.systems.ParticleSystemController;
import io.wispforest.owo.util.OwoFreezer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wraith.alloyforgery.block.ForgeControllerBlockEntity;
import wraith.alloyforgery.client.AlloyForgeryClient;
import wraith.alloyforgery.compat.AlloyForgeryConfig;
import wraith.alloyforgery.data.AlloyForgeryGlobalRemaindersLoader;
import wraith.alloyforgery.data.RecipeTagLoader;
import wraith.alloyforgery.forges.ForgeRegistry;
import wraith.alloyforgery.forges.FuelDataLoader;
import wraith.alloyforgery.recipe.*;
import wraith.alloyforgery.utils.RecipeInjector;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Mod(AlloyForgery.MOD_ID)
public class AlloyForgery implements ModInitializer {

    public static final OwoNetChannel CHANNEL = OwoNetChannel.create(id("main"));

    public static final String MOD_ID = "alloy_forgery";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static final AlloyForgeryConfig CONFIG = AlloyForgeryConfig.createAndLoad();

    public static BlockEntityType<ForgeControllerBlockEntity> FORGE_CONTROLLER_BLOCK_ENTITY;
    public static final List<Block> FORGE_CONTROLLER_BLOCK_ENTITY_BLOCK_LIST = new CopyOnWriteArrayList<>();
    public static MenuType<AlloyForgeScreenHandler> ALLOY_FORGE_SCREEN_HANDLER_TYPE;

    private static final ParticleSystemController CONTROLLER = new ParticleSystemController(id("particles"));
    public static final ParticleSystem<Direction> FORGE_PARTICLES = CONTROLLER.register(Direction.class, (world, pos, facing) -> {
        final Vec3 particleSide = pos.add(0.5 + facing.getStepX() * 0.515, 0.25, 0.5 + facing.getStepZ() * 0.515);
        ClientParticles.spawnPrecise(ParticleTypes.FLAME, world, particleSide,
                facing.getStepZ() * 0.65,
                0.175,
                facing.getStepX() * 0.65);

        ClientParticles.spawnPrecise(ParticleTypes.SMOKE, world, particleSide,
                facing.getStepZ() * 0.65,
                0.175,
                facing.getStepX() * 0.65);
    });

    public AlloyForgery(ModContainer modContainer, IEventBus bus) {
        onInitialize();
        if (FMLLoader.getDist() == Dist.CLIENT) {
            new AlloyForgeryClient().onInitializeClient();
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void onInitialize() {
        ALLOY_FORGE_SCREEN_HANDLER_TYPE = Registry.register(BuiltInRegistries.MENU, id("alloy_forge"), new MenuType<>(AlloyForgeScreenHandler::new, FeatureFlags.DEFAULT_FLAGS));

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new AlloyForgeryGlobalRemaindersLoader());
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(FuelDataLoader.INSTANCE);

        var recipeTagLoader = new RecipeTagLoader();

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(recipeTagLoader);

        recipeTagLoader.initEvents();

        CHANNEL.registerClientboundDeferred(RecipeTagLoader.TagPacket.class);

        RecipeInjector.initEvents();
        RecipeInjector.ADD_RECIPES.register(new BlastFurnaceRecipeAdapter());

        ModDataLoader.load(ForgeRegistry.Loader.INSTANCE);
        Block[] blocks = FORGE_CONTROLLER_BLOCK_ENTITY_BLOCK_LIST.toArray(Block[]::new);
        LOGGER.warn("AlloyForgery loaded {} controller blocks", FORGE_CONTROLLER_BLOCK_ENTITY_BLOCK_LIST);
        FORGE_CONTROLLER_BLOCK_ENTITY = BlockEntityType.Builder.of(ForgeControllerBlockEntity::new, blocks).build(null);

        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("forge_controller"), FORGE_CONTROLLER_BLOCK_ENTITY);

        Registry.register(BuiltInRegistries.RECIPE_TYPE, AlloyForgeRecipe.Type.ID, AlloyForgeRecipe.Type.INSTANCE);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, AlloyForgeRecipe.Type.ID, AlloyForgeRecipeSerializer.INSTANCE);

        AlloyForgeryItemGroup.GROUP.initialize();

        ItemStorage.SIDED.registerFallback((world, pos, state, blockEntity, context) -> {
            if (context == Direction.DOWN && world.getBlockEntity(pos.above()) instanceof ForgeControllerBlockEntity froge)
                return InventoryStorage.of(froge, Direction.DOWN);

            return null;
        });

        OwoFreezer.registerFreezeCallback(() -> FluidStorage.SIDED.registerSelf(AlloyForgery.FORGE_CONTROLLER_BLOCK_ENTITY));
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
