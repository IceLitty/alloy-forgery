package wraith.alloyforgery.block;

import com.google.common.collect.ImmutableList;
import io.wispforest.owo.ops.ItemOps;
import io.wispforest.owo.serialization.format.nbt.NbtDeserializer;
import io.wispforest.owo.serialization.format.nbt.NbtSerializer;
import io.wispforest.owo.util.ImplementedInventory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.core.NonNullList;
import net.minecraft.core.*;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import wraith.alloyforgery.AlloyForgeScreenHandler;
import wraith.alloyforgery.AlloyForgery;
import wraith.alloyforgery.client.BlockEntityLocation;
import wraith.alloyforgery.forges.ForgeDefinition;
import wraith.alloyforgery.forges.ForgeFuelRegistry;
import wraith.alloyforgery.forges.ForgeTier;
import wraith.alloyforgery.forges.ForgeTierRegistry;
import wraith.alloyforgery.mixin.HopperBlockEntityAccessor;
import wraith.alloyforgery.recipe.AlloyForgeRecipe;
import wraith.alloyforgery.recipe.AlloyForgeRecipeInput;
import wraith.alloyforgery.utils.EndecUtils;
import wraith.alloyforgery.utils.ExtObservable;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class ForgeControllerBlockEntity extends BlockEntity implements ImplementedInventory, WorldlyContainer, ExtendedScreenHandlerFactory<BlockEntityLocation>, InsertionOnlyStorage<FluidVariant> {

    private static final int[] DOWN_SLOTS = new int[]{10, 11};
    private static final Integer[] RIGHT_SLOTS = new Integer[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    private static final int[] LEFT_SLOTS = new int[]{11};

    public static final int INVENTORY_SIZE = 12;
    private final NonNullList<ItemStack> items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);

    public final ExtObservable<Set<Integer>> disabledSlots = ExtObservable.of(new HashSet<>());

    private final NonNullList<ItemStack> previousItems = NonNullList.create();
    private boolean checkForRecipes = true;

    private Optional<RecipeHolder<AlloyForgeRecipe>> recipeCache = Optional.empty();

    public final ExtObservable<Integer> requiredTierToCraft = ExtObservable.of(-1);

    private final FluidHolder fluidHolder = new FluidHolder();

    private final ForgeDefinition forgeDefinition;
    private final ImmutableList<BlockPos> multiblockPositions;
    private final Direction facing;

    private float fuel;
    private int currentSmeltTime;

    public final ExtObservable<Integer> smeltProgress = ExtObservable.of(0);
    public final ExtObservable<Integer> fuelProgress = ExtObservable.of(0);
    public final ExtObservable<Integer> lavaProgress = ExtObservable.of(0);

    public ForgeControllerBlockEntity(BlockPos pos, BlockState state) {
        super(AlloyForgery.FORGE_CONTROLLER_BLOCK_ENTITY, pos, state);
        forgeDefinition = ((ForgeControllerBlock) state.getBlock()).forgeDefinition;
        facing = state.getValue(ForgeControllerBlock.FACING);

        multiblockPositions = generateMultiblockPositions(pos.immutable(), state.getValue(ForgeControllerBlock.FACING));
    }

    public ForgeTier forgeTier() {
        if (this.world == null) return ForgeTier.DEFAULT;

        var tier = ForgeTierRegistry.getForgeRegistry(this.world.isClient()).getForgeTier(this.forgeDefinition);

        if (tier == null) return ForgeTier.DEFAULT;

        return tier;
    }

    @Override
    public BlockEntityLocation getScreenOpeningData(ServerPlayer player) {
        return BlockEntityLocation.of(this);
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        ContainerHelper.loadAllItems(nbt, items, registryLookup);

        this.currentSmeltTime = nbt.getInt("CurrentSmeltTime");
        this.fuel = nbt.getInt("Fuel");

        final var fluidNbt = nbt.getCompound("FuelFluidInput");
        this.fluidHolder.amount = fluidNbt.getLong("Amount");
        this.fluidHolder.variant = EndecUtils.FLUID_VARIANT.decodeFully(NbtDeserializer::of, nbt.getCompound("Variant"));
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        ContainerHelper.saveAllItems(nbt, items, registryLookup);

        nbt.putInt("Fuel", Math.round(fuel));
        nbt.putInt("CurrentSmeltTime", currentSmeltTime);

        final var fluidNbt = new CompoundTag();
        fluidNbt.putLong("Amount", this.fluidHolder.amount);
        fluidNbt.put("Variant", EndecUtils.FLUID_VARIANT.encodeFully(NbtSerializer::of, this.fluidHolder.variant));
        nbt.put("FuelFluidInput", fluidNbt);
    }

    @Override
    public NonNullList<ItemStack> getItems() {
        return items;
    }

    public ItemStack getFuelStack() {
        return getItem(11);
    }

    public boolean canAddFuel(int fuel) {
        return this.fuel + fuel <= forgeTier().fuelCapacity();
    }

    public void addFuel(int fuel) {
        this.fuel += fuel;
    }

    public int getSmeltProgress() {
        return smeltProgress.get();
    }

    public int getCurrentSmeltTime() {
        return currentSmeltTime;
    }

    public ForgeDefinition getForgeDefinition() {
        return this.forgeDefinition;
    }

    public void disableSlot(int index) {
        this.disabledSlots.get().add(index);
        this.disabledSlots.markDirty();
    }

    public void enableSlot(int index) {
        this.disabledSlots.get().remove(index);
        this.disabledSlots.markDirty();
    }

    @Override
    public void setChanged() {
        if (ItemStackComparisonUtil.itemsChanged(items, previousItems)) {
            this.previousItems.clear();
            this.previousItems.addAll(items.stream().map(ItemStack::copy).toList());

            this.checkForRecipes = true;
        }

        super.setChanged();
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        ImplementedInventory.super.setItem(slot, stack);

        this.setChanged();
    }

    public void tick() {
        this.smeltProgress.set(Math.round((this.currentSmeltTime / (float) forgeTier().maxSmeltTime()) * 19));
        this.fuelProgress.set(Math.round((this.fuel / (float) forgeTier().fuelCapacity()) * 48));
        this.lavaProgress.set(Math.round((this.fluidHolder.getAmount() / (float) FluidConstants.BUCKET) * 50));

        level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());

        if (!this.verifyMultiblock()) {
            this.currentSmeltTime = 0;

            final var currentState = level.getBlockState(worldPosition);
            if (currentState.getValue(ForgeControllerBlock.LIT)) {
                level.setBlockAndUpdate(worldPosition, currentState.setValue(ForgeControllerBlock.LIT, false));
            }

            return;
        }

        if (!this.getFuelStack().isEmpty()) {
            final var fuelStack = this.getFuelStack();
            final var fuelDefinition = ForgeFuelRegistry.getFuelForItem(fuelStack.getItem());

            if (fuelDefinition != ForgeFuelRegistry.ForgeFuelDefinition.EMPTY && canAddFuel(fuelDefinition.fuel())) {
                this.getFuelStack().shrink(1);

                attemptInsertOnIndex(11, fuelDefinition.hasReturnType() ? new ItemStack(fuelDefinition.returnType()) : ItemStack.EMPTY);

                this.fuel += fuelDefinition.fuel();
            }
        }

        // Failsafe just incase something was within the disabled slot and was disabled
        for (var i : this.disabledSlots.get()) {
            var stack = this.getStack(i);

            if (!stack.isEmpty()) insertIntoHopperOrScatterAtFront(stack);

            this.setStack(i, ItemStack.EMPTY);
        }

        final var emptyFuelSpace = this.forgeTier().fuelCapacity() - this.fuel;

        if (this.fluidHolder.amount >= 81 && emptyFuelSpace > 0f) {
            final float fuelInsertAmount = Math.min((this.fluidHolder.amount / 81f) * 24, ((emptyFuelSpace) / 24) * 24);

            this.fuel += fuelInsertAmount;
            this.fluidHolder.amount -= (fuelInsertAmount / 24) * 81;
        }

        final var currentBlockState = this.level.getBlockState(worldPosition);
        if (this.fuel > 100 && !currentBlockState.getValue(ForgeControllerBlock.LIT)) {
            this.level.setBlockAndUpdate(worldPosition, currentBlockState.setValue(ForgeControllerBlock.LIT, true));
        } else if (fuel < 100 && currentBlockState.getValue(ForgeControllerBlock.LIT)) {
            this.level.setBlockAndUpdate(worldPosition, currentBlockState.setValue(ForgeControllerBlock.LIT, false));
        }

        // 1: Check if the inventory is full
        // 2: Prevent crafting when we know that there is not enough fuel to craft at all
        // 3: Prevent recipe checking if the inventory has not changed
        if (this.isEmpty()) {
            this.currentSmeltTime = 0;

            return;
        }

        if (this.fuel < 5 || !this.checkForRecipes) {
            this.currentSmeltTime = 0;

            return;
        }

        //--

        var recipeInput = new AlloyForgeRecipeInput(this);

        if (this.recipeCache.isEmpty() || !this.recipeCache.get().value().matches(recipeInput, this.level)) {
            this.recipeCache = this.level.getRecipeManager().getRecipeFor(AlloyForgeRecipe.Type.INSTANCE, recipeInput, this.level);
        }

        if (this.recipeCache.isEmpty() && this.requiredTierToCraft.get() != -1) {
            this.requiredTierToCraft.set( -1);
        }

        if (this.recipeCache.isEmpty() || !canSmelt(this.recipeCache.get().value())) {
            this.checkForRecipes = false;
            this.currentSmeltTime = 0;
            return;
        }

        //--

        var recipe = recipeCache.get().value();

        if (this.currentSmeltTime < this.forgeTier().maxSmeltTime()) {
            final float fuelRequirement = recipe.getFuelPerTick() * this.forgeTier().speedMultiplier();

            if (this.fuel - fuelRequirement < 0) {
                this.currentSmeltTime = 0;
                return;
            }

            this.currentSmeltTime++;
            this.fuel -= fuelRequirement;

            if (this.level.random.nextDouble() > 0.75) {
                AlloyForgery.FORGE_PARTICLES.spawn(this.level, Vec3.atLowerCornerOf(this.worldPosition), this.facing);
            }
        } else {
            var remainderList = AlloyForgeRecipe.gatherRemainders(recipeCache.get(), recipeInput);

            if (remainderList != null) this.handleForgingRemainders(remainderList);

            var outputStack = this.getItem(10);
            var recipeOutput = recipe.assemble(recipeInput, this.level.registryAccess());

            recipe.consumeIngredients(recipeInput);

            if (outputStack.isEmpty()) {
                this.setItem(10, recipeOutput);
            } else {
                outputStack.grow(recipeOutput.getCount());
            }

            this.currentSmeltTime = 0;
        }
    }

    private boolean canSmelt(AlloyForgeRecipe recipe) {
        final var outputStack = this.getItem(10);
        final var recipeOutput = recipe.getResult(this.forgeDefinition.forgeTier().value());

        if (recipe.getMinForgeTier() > this.forgeTier().value()) {
            this.requiredTierToCraft.set(recipe.getMinForgeTier());

            return false;
        } else if (requiredTierToCraft.get() != -1) {
            this.requiredTierToCraft.set(-1);
        }

        return outputStack.isEmpty() || ItemOps.canStack(outputStack, recipeOutput);
    }

    private void handleForgingRemainders(NonNullList<ItemStack> remainderList) {
        for (int i = 0; i < remainderList.size(); ++i) {
            attemptInsertOnIndex(i, remainderList.get(i));
        }
    }

    public void attemptInsertOnIndex(int i, ItemStack itemstack) {
        if (itemstack.isEmpty()) return;

        var slotStack = this.getItem(i);

        if (slotStack.isEmpty()) {
            this.setItem(i, itemstack);
        } else if (ItemStack.isSameItem(slotStack, itemstack) && ItemStack.isSameItemSameComponents(slotStack, itemstack)) {
            itemstack.grow(slotStack.getCount());

            if (itemstack.getCount() > itemstack.getMaxStackSize()) {
                int excess = itemstack.getCount() - itemstack.getMaxStackSize();
                itemstack.shrink(excess);

                var insertStack = itemstack.copy();
                insertStack.setCount(excess);

                insertIntoHopperOrScatterAtFront(insertStack);
            }

            this.setItem(i, itemstack);
        } else {
            insertIntoHopperOrScatterAtFront(itemstack);
        }
    }

    private void insertIntoHopperOrScatterAtFront(ItemStack stack) {
        if (!this.attemptToInsertIntoHopper(stack)) {
            var frontForgePos = worldPosition.relative(getBlockState().getValue(ForgeControllerBlock.FACING));

            level.playSound(null, frontForgePos.getX(), frontForgePos.getY(), frontForgePos.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 0.2F);
            Containers.dropItemStack(level, frontForgePos.getX(), frontForgePos.getY(), frontForgePos.getZ(), stack);
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean attemptToInsertIntoHopper(ItemStack remainderStack) {
        if (remainderStack.isEmpty()) return true;

        HopperBlockEntity blockEntity = null;

        for (int y = 1; y <= 2; y++) {
            if (level.getBlockEntity(this.worldPosition.below(y)) instanceof HopperBlockEntity hopperBlockEntity) {
                blockEntity = hopperBlockEntity;

                break;
            }
        }

        if (blockEntity != null) {
            var isHopperEmpty = blockEntity.isEmpty();

            for (int slotIndex = 0; slotIndex < blockEntity.getContainerSize(); ++slotIndex) {
                if (remainderStack.isEmpty()) break;

                if (!blockEntity.getItem(slotIndex).isEmpty()) {
                    final var itemStack = blockEntity.getItem(slotIndex);

                    if (itemStack.isEmpty()) {
                        blockEntity.setItem(slotIndex, remainderStack);
                        remainderStack = ItemStack.EMPTY;
                    } else if (ItemOps.canStack(itemStack, remainderStack)) {
                        int availableSpace = itemStack.getMaxStackSize() - itemStack.getCount();
                        int j = Math.min(itemStack.getCount(), availableSpace);
                        remainderStack.shrink(j);
                        itemStack.grow(j);
                    }
                } else {
                    blockEntity.setItem(slotIndex, remainderStack);
                    break;
                }
            }

            if (isHopperEmpty && !((HopperBlockEntityAccessor) blockEntity).alloyForge$isDisabled()) {
                ((HopperBlockEntityAccessor) blockEntity).alloyForge$setTransferCooldown(8);
            }

            blockEntity.setChanged();

            return true;
        }

        return false;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean verifyMultiblock() {
        final BlockState belowController = level.getBlockState(multiblockPositions.get(0));
        if (!(belowController.is(Blocks.HOPPER) || forgeDefinition.isBlockValid(belowController.getBlock())))
            return false;

        for (int i = 1; i < multiblockPositions.size(); i++) {
            if (!forgeDefinition.isBlockValid(level.getBlockState(multiblockPositions.get(i)).getBlock())) return false;
        }

        return true;
    }

    private static ImmutableList<BlockPos> generateMultiblockPositions(BlockPos controllerPos, Direction controllerFacing) {
        final List<BlockPos> posses = new ArrayList<>();
        final BlockPos center = controllerPos.relative(controllerFacing.getOpposite());

        for (BlockPos pos : BlockPos.betweenClosed(center.offset(1, -1, 1), center.offset(-1, -1, -1))) {
            posses.add(pos.immutable());
        }

        posses.remove(controllerPos.below());
        posses.add(0, controllerPos.below());

        for (int i = 0; i < 2; i++) {
            final var newCenter = center.offset(0, i, 0);

            posses.add(newCenter.east());
            posses.add(newCenter.west());
            posses.add(newCenter.north());
            posses.add(newCenter.south());
        }

        posses.remove(controllerPos);
        return ImmutableList.copyOf(posses);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return DOWN_SLOTS;
        } else if (side == facing.getClockWise()) {
            return LEFT_SLOTS;
        } else if (side == facing.getCounterClockWise() && this.currentSmeltTime == 0) {
            return Arrays.stream(RIGHT_SLOTS)
                    .filter(i -> !this.disabledSlots.get().contains(i))
                    .mapToInt(value -> value).toArray();
        } else {
            return new int[0];
        }
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == 11) return ForgeFuelRegistry.hasFuel(stack.getItem());
        if (this.disabledSlots.get().contains(slot)) return false;

        var slotStack = getItem(slot);

        return slotStack.isEmpty() || ItemOps.canStack(slotStack, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot == 10 || (slot == 11 && !ForgeFuelRegistry.hasFuel(stack.getItem()));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.alloy_forgery.forge_controller");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        return new AlloyForgeScreenHandler(syncId, inv, this);
    }

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        return this.fluidHolder.insert(resource, maxAmount, transaction);
    }

    @Override
    public Iterator<StorageView<FluidVariant>> iterator() {
        return this.fluidHolder.iterator();
    }

    private class FluidHolder extends SingleVariantStorage<FluidVariant> implements InsertionOnlyStorage<FluidVariant> {
        @Override
        protected FluidVariant getBlankVariant() {
            return FluidVariant.blank();
        }

        @Override
        protected long getCapacity(FluidVariant variant) {
            return FluidConstants.BUCKET + 81;
        }

        @Override
        protected void onFinalCommit() {
            ForgeControllerBlockEntity.this.setChanged();
        }

        @Override
        protected boolean canInsert(FluidVariant variant) {
            return variant.isOf(Fluids.LAVA);
        }

        @Override
        protected boolean canExtract(FluidVariant variant) {
            return false;
        }

        @Override
        public Iterator<StorageView<FluidVariant>> iterator() {
            return InsertionOnlyStorage.super.iterator();
        }
    }
}
