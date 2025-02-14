package wraith.alloyforgery.block;

import com.mojang.serialization.MapCodec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.particles.ClientParticles;
import io.wispforest.owo.serialization.CodecUtils;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import wraith.alloyforgery.AlloyForgery;
import wraith.alloyforgery.forges.ForgeDefinition;
import wraith.alloyforgery.forges.ForgeFuelRegistry;

public class ForgeControllerBlock extends BaseEntityBlock {

    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public final ForgeDefinition forgeDefinition;

    public ForgeControllerBlock(ForgeDefinition forgeDefinition) {
        super(FabricBlockSettings.copyOf(Blocks.BLACKSTONE));
        this.forgeDefinition = forgeDefinition;
        this.defaultBlockState().setValue(LIT, false);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CodecUtils.toMapCodec(
                StructEndecBuilder.of(
                        ForgeDefinition.FORGE_DEFINITION.fieldOf("forge_definition", s -> forgeDefinition),
                        ForgeControllerBlock::new
                ));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack playerStack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!world.isClientSide) {
            final var fuelDefinition = ForgeFuelRegistry.getFuelForItem(playerStack.getItem());
            if (!(world.getBlockEntity(pos) instanceof ForgeControllerBlockEntity controller)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

            if (fuelDefinition.hasReturnType() && controller.canAddFuel(fuelDefinition.fuel())) {
                if (!player.getAbilities().instabuild) {
                    player.getItemInHand(hand).shrink(1);
                    player.getInventory().placeItemBackInInventory(new ItemStack(fuelDefinition.returnType()));
                }
                controller.addFuel(fuelDefinition.fuel());
            } else if (FluidStorageUtil.interactWithFluidStorage(controller, player, hand)) {
                return ItemInteractionResult.SUCCESS;
            } else {
                if (!controller.verifyMultiblock()) {
                    player.displayClientMessage(Component.translatable("message.alloy_forgery.invalid_multiblock").withStyle(ChatFormatting.GRAY), true);
                    return ItemInteractionResult.SUCCESS;
                }

                final var screenHandlerFactory = state.getMenuProvider(world, pos);
                if (screenHandlerFactory != null) {
                    player.openMenu(screenHandlerFactory);
                }
            }
        }

        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            if (world.getBlockEntity(pos) instanceof ForgeControllerBlockEntity forgeController) {
                Containers.dropContents(world, pos, forgeController);
                Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), forgeController.getFuelStack());
            }
            super.onRemove(state, world, pos, newState, moved);
        }
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;

        final BlockPos center = pos.relative(state.getValue(FACING).getOpposite());

        ClientParticles.setParticleCount(2);
        ClientParticles.setVelocity(new Vec3(0, 0.1, 0));
        ClientParticles.spawnWithinBlock(ParticleTypes.CAMPFIRE_COSY_SMOKE, world, center);

        ClientParticles.setParticleCount(5);
        ClientParticles.setVelocity(new Vec3(0, 0.1, 0));
        ClientParticles.spawnWithinBlock(ParticleTypes.LARGE_SMOKE, world, center);

        if (random.nextDouble() > 0.65) {
            ClientParticles.setParticleCount(1);
            ClientParticles.setVelocity(new Vec3(0, 0.01, 0));
            ClientParticles.spawnWithinBlock(ParticleTypes.CAMPFIRE_COSY_SMOKE, world, center);
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, FACING);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return world.isClientSide ? null : createTickerHelper(type, AlloyForgery.FORGE_CONTROLLER_BLOCK_ENTITY, (world1, pos, state1, blockEntity) -> blockEntity.tick());
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof ForgeControllerBlockEntity controller)) return 0;
        return controller.getCurrentSmeltTime() == 0 ? 0 : Math.max(1, Math.round(controller.getSmeltProgress() * 0.46875f));
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ForgeControllerBlockEntity(pos, state);
    }
}
