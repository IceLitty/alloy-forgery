package wraith.alloyforgery;

import io.wispforest.endec.Endec;
import io.wispforest.owo.client.screens.*;
import io.wispforest.owo.util.EventStream;
import io.wispforest.owo.util.pond.OwoScreenHandlerExtension;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.*;
import net.minecraft.server.level.ServerPlayer;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;
import wraith.alloyforgery.block.ForgeControllerBlockEntity;
import wraith.alloyforgery.forges.ForgeFuelRegistry;
import wraith.alloyforgery.utils.ExtObservable;
import wraith.alloyforgery.utils.ForgeInputSlot;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class AlloyForgeScreenHandler extends AbstractContainerMenu {

    private final Container controllerInventory;

    private final boolean isServer;

    public final ForgeControllerBlockEntity forge;

    private final SyncedProperty<Integer> smeltProgress;
    private final SyncedProperty<Integer> fuelProgress;
    private final SyncedProperty<Integer> lavaProgress;
    private final SyncedProperty<Integer> requiredTierToCraft;

    private final SyncedProperty<Set<Integer>> disabledSlots;

    private final OwoScreenHandler mixin;

    public AlloyForgeScreenHandler(int syncId, Inventory playerInventory, ForgeControllerBlockEntity forge) {
        super(AlloyForgery.ALLOY_FORGE_SCREEN_HANDLER_TYPE, syncId);

        if (!(this instanceof OwoScreenHandler)) {
            throw new RuntimeException("Can not resolve owo-lib because AbstractContainerMenu is not implements by OwoScreenHandler");
        }
        mixin = (OwoScreenHandler) this;

        this.isServer = playerInventory.player instanceof ServerPlayer;

        this.forge = forge;

        this.controllerInventory = (forge != null) ? forge : new SimpleContainer(ForgeControllerBlockEntity.INVENTORY_SIZE);

        this.smeltProgress = createProperty(Integer.class, forge, (provider) -> provider.smeltProgress, 0);
        this.fuelProgress = createProperty(Integer.class, forge, (provider) -> provider.fuelProgress, 0);
        this.lavaProgress = createProperty(Integer.class, forge, (provider) -> provider.lavaProgress, 0);
        this.requiredTierToCraft = createProperty(Integer.class, forge, (provider) -> provider.requiredTierToCraft, -1);

        this.disabledSlots = createSetProperty(Integer.class, forge, (provider) -> provider.disabledSlots);

        //Fuel Slot
        this.addSlot(new Slot(controllerInventory, 11, 8, 74) {
            @Override public boolean mayPlace(ItemStack stack) { return ForgeFuelRegistry.hasFuel(stack.getItem()); }
        });

        //Recipe Output
        this.addSlot(new Slot(controllerInventory, 10, 145, 50) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });

        SlotGenerator.begin(this::addSlot, 44, 43)
                .slotFactory((inventory, index, x, y) -> new ForgeInputSlot(inventory, index, x, y, this))
                .grid(controllerInventory, 0, 5, 2)
                .defaultSlotFactory()
                .moveTo(8, 107)
                .playerInventory(playerInventory);
    }

    //--

    private final EventStream<Runnable> onClosedEvent = new EventStream<>(closeRuns -> () -> closeRuns.forEach(Runnable::run));

    public <P, T> SyncedProperty<Set<T>> createSetProperty(Class<T> elementType, @Nullable P provider, Function<P, ExtObservable<Set<T>>> observableProviderFunc) {
        var type = createParameterizedType(Set.class, elementType);

        return (SyncedProperty<Set<T>>) (Object) createProperty(
                Set.class,
                (Endec<Set>) mixin.endecBuilder().get(type),
                provider,
                observableProviderFunc.andThen(listExtObservable -> (ExtObservable<Set>) (Object) listExtObservable),
                set -> new HashSet<>(set),
                new HashSet<>());
    }

    public <P, T> SyncedProperty<List<T>> createListProperty(Class<T> elementType, @Nullable P provider, Function<P, ExtObservable<List<T>>> observableProviderFunc) {
        var type = createParameterizedType(List.class, elementType);

        return (SyncedProperty<List<T>>) (Object) createProperty(
                List.class,
                (Endec<List>) mixin.endecBuilder().get(type),
                provider,
                observableProviderFunc.andThen(listExtObservable -> (ExtObservable<List>) (Object) listExtObservable),
                list -> new ArrayList<>(list),
                new ArrayList<>());
    }

    private static ParameterizedType createParameterizedType(Type rawType, Type ...typeArgs) {
        if (typeArgs.length == 0) throw new IllegalStateException("Unable to create ParameterizedType with zero type args!");

        return new ParameterizedType() {
            @Override public Type[] getActualTypeArguments() { return typeArgs; }
            @Override public Type getRawType() { return rawType; }
            @Override public Type getOwnerType() { return null; }
        };
    }

    public <P, T> SyncedProperty<T> createProperty(Class<T> clazz, @Nullable P provider, Function<P, ExtObservable<T>> observableProviderFunc, T initial) {
        return createProperty(clazz, mixin.endecBuilder().get(clazz), provider, observableProviderFunc, t -> t, initial);
    }

    public <P, T> SyncedProperty<T> createProperty(Class<T> clazz, @Nullable P provider, Function<P, ExtObservable<T>> observableProviderFunc, Function<T, T> cloneFunc, T initial) {
        return createProperty(clazz, mixin.endecBuilder().get(clazz), provider, observableProviderFunc, cloneFunc, initial);
    }

    public <P, T> SyncedProperty<T> createProperty(Class<T> clazz, Endec<T> endec, @Nullable P provider, Function<P, ExtObservable<T>> observableProviderFunc, Function<T, T> cloneFunc, T initial) {
        return (provider != null && this.isServer)
                ? createProperty(clazz, endec, observableProviderFunc.apply(provider), cloneFunc)
                : mixin.createProperty(clazz, endec, initial);
    }

    public <T> SyncedProperty<T> createProperty(Class<T> clazz, ExtObservable<T> initial, Function<T, T> cloneFunc) {
        return createProperty(clazz, mixin.endecBuilder().get(clazz), initial, cloneFunc);
    }

    public <T> SyncedProperty<T> createProperty(Class<T> clazz, Endec<T> endec, ExtObservable<T> initial, Function<T, T> cloneFunc) {
        var property = mixin.createProperty(clazz, endec, cloneFunc.apply(initial.get()));

        property.markDirty();

        this.onClosedEvent.source().subscribe(initial.observeSub(t -> property.set(cloneFunc.apply(t)))::cancel);

        return property;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);

        this.onClosedEvent.sink().run();
    }

    //--

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        return ScreenUtils.handleSlotTransfer(this, invSlot, this.controllerInventory.getContainerSize());
    }

    public int getSmeltProgress() {
        return this.smeltProgress.get();
    }

    public int getFuelProgress() {
        return this.fuelProgress.get();
    }

    public int getLavaProgress() {
        return this.lavaProgress.get();
    }

    public int getRequiredTierData() {
        return this.requiredTierToCraft.get();
    }

    public boolean isSlotDisabled(Slot slot) {
        return this.disabledSlots.get().contains(slot.getContainerSlot());
    }

    @Override
    public boolean stillValid(Player player) {
        return this.controllerInventory.stillValid(player);
    }

    public Container getControllerInventory() {
        return this.controllerInventory;
    }
}
