package wraith.alloyforgery;

import io.wispforest.owo.client.screens.ScreenUtils;
import io.wispforest.owo.client.screens.SlotGenerator;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.*;
import wraith.alloyforgery.block.ForgeControllerBlockEntity;
import wraith.alloyforgery.forges.ForgeFuelRegistry;

public class AlloyForgeScreenHandler extends AbstractContainerMenu {

    private final Container controllerInventory;
    private final ContainerData propertyDelegate;

    public AlloyForgeScreenHandler(int syncId, Inventory inventory) {
        this(syncId, inventory, new SimpleContainer(ForgeControllerBlockEntity.INVENTORY_SIZE), new SimpleContainerData(4));
    }

    public AlloyForgeScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate) {
        super(AlloyForgery.ALLOY_FORGE_SCREEN_HANDLER_TYPE, syncId);

        this.controllerInventory = inventory;
        this.propertyDelegate = propertyDelegate;
        this.addDataSlots(propertyDelegate);

        //Fuel Slot
        this.addSlot(new Slot(controllerInventory, 11, 8, 74) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ForgeFuelRegistry.hasFuel(stack.getItem());
            }
        });

        //Recipe Output
        this.addSlot(new Slot(controllerInventory, 10, 145, 50) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        SlotGenerator.begin(this::addSlot, 44, 43)
                .grid(controllerInventory, 0, 5, 2)
                .moveTo(8, 107)
                .playerInventory(playerInventory);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        return ScreenUtils.handleSlotTransfer(this, invSlot, this.controllerInventory.getContainerSize());
    }

    public int getSmeltProgress() {
        return propertyDelegate.get(0);
    }

    public int getFuelProgress() {
        return propertyDelegate.get(1);
    }

    public int getLavaProgress() {
        return propertyDelegate.get(2);
    }

    public int getRequiredTierData() {
        return propertyDelegate.get(3);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.controllerInventory.stillValid(player);
    }

    public Container getControllerInventory() {
        return this.controllerInventory;
    }
}
