package wraith.alloyforgery.client;

import io.wispforest.owo.ui.base.BaseUIModelHandledScreen;
import io.wispforest.owo.ui.base.BaseUIModelScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.TextureComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.PositionedRectangle;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import wraith.alloyforgery.AlloyForgeScreenHandler;
import wraith.alloyforgery.AlloyForgery;
import wraith.alloyforgery.networking.AlloyForgeNetworking;
import wraith.alloyforgery.networking.DisableSlotToggle;
import wraith.alloyforgery.utils.ForgeInputSlot;

import java.util.List;

public class AlloyForgeScreen extends BaseUIModelHandledScreen<FlowLayout, AlloyForgeScreenHandler> {

    private static final ResourceLocation DISABLED_SLOT_TEXTURE = AlloyForgery.id("textures/gui/disabled_forge_slot.png");

    private static final Component ENABLED_SLOT_TEXT = Component.translatable("tooltip.alloy_forgery.enabled_slot");
    private static final Component DISABLED_SLOT_TEXT = Component.translatable("tooltip.alloy_forgery.disabled_slot");

    private TextureComponent fuelGauge;
    private TextureComponent progressGauge;
    private TextureComponent invalidCross;
    private FlowLayout lavaBar;

    private boolean allowSlotToggling = false;

    public AlloyForgeScreen(AlloyForgeScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, FlowLayout.class, BaseUIModelScreen.DataSource.asset(AlloyForgery.id("forge")));
        this.imageWidth = 176;
        this.imageHeight = 189;

        this.titleLabelY = 69420;
        this.inventoryLabelY = this.imageHeight - 93;
    }

    @Override
    protected void build(FlowLayout layout) {
        this.fuelGauge = layout.childById(TextureComponent.class, "fuel-gauge");
        this.invalidCross = layout.childById(TextureComponent.class, "invalid-cross");
        this.progressGauge = layout.childById(TextureComponent.class, "progress-gauge");
        this.lavaBar = layout.childById(FlowLayout.class, "lava-bar");

        layout.childById(ButtonComponent.class, "slot-toggle-btn")
                .onPress(btn -> {
                    this.allowSlotToggling = !allowSlotToggling;

                    btn.tooltip(Component.translatable("tooltip.alloy_forgery.slot_toggle_" + (this.allowSlotToggling ? "enable" : "disable")));
                })
                .renderer((context, button, delta) -> {
                    ButtonComponent.Renderer.VANILLA.draw(context, button, delta);

                    context.push().translate(-0.75, -0.75, 0);

                    context.drawCenteredTextWithShadow(Minecraft.getInstance().textRenderer, "⏻", button.leftPos() + 8, button.topPos() + 4, 0xFFFFFFFF);

                    context.pop();
                });
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.fuelGauge.visibleArea(PositionedRectangle.of(0, this.fuelGauge.height() - this.menu.getFuelProgress(), this.fuelGauge.fullSize()));
        this.progressGauge.visibleArea(PositionedRectangle.of(0, 0, this.progressGauge.width(), this.menu.getSmeltProgress()));
        this.lavaBar.horizontalSizing(Sizing.fixed(this.menu.getLavaProgress()));

        int requiredTier = this.menu.getRequiredTierData();

        if (requiredTier <= -1) {
            this.invalidCross
                    .visibleArea(PositionedRectangle.of(0, 0, 0, 0))
                    .tooltip(List.<ClientTooltipComponent>of());
        } else {
            this.invalidCross
                    .resetVisibleArea()
                    .tooltip(Component.translatable("tooltip.alloy_forgery.invalid_tier", requiredTier));
        }

        if (this.allowSlotToggling
                && this.focusedSlot instanceof ForgeInputSlot
                && this.handler.getCursorStack().isEmpty()
                && !this.focusedSlot.hasStack()
                && !this.handler.player().isSpectator()) {

            if (this.handler.isSlotDisabled(this.focusedSlot)) {
                context.drawTooltip(this.textRenderer, DISABLED_SLOT_TEXT, mouseX, mouseY);
            } else {
                context.drawTooltip(this.textRenderer, ENABLED_SLOT_TEXT, mouseX, mouseY);
            }
        }
    }

    @Override
    protected void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType) {
        var player = this.handler.player();

        if(allowSlotToggling) {
            if (slot instanceof ForgeInputSlot && !slot.hasStack() && !player.isSpectator()) {
                switch (actionType) {
                    case PICKUP:
                        if (this.handler.isSlotDisabled(slot)) {
                            this.enableInputSlot(slot);
                        } else /*if (this.handler.getCursorStack().isEmpty())*/ {
                            this.disableInputSlot(slot);
                        }
                        break;
                    case SWAP:
                        ItemStack itemStack = player.getInventory().getStack(button);
                        if (this.handler.isSlotDisabled(slot) && !itemStack.isEmpty()) {
                            this.enableInputSlot(slot);
                        }
                }
            }
        }

        super.onMouseClick(slot, slotId, button, actionType);
    }

    private void enableInputSlot(Slot slot) {
        this.setSlotEnabled(slot, true);
    }

    private void disableInputSlot(Slot slot) {
        this.setSlotEnabled(slot, false);
    }

    private void setSlotEnabled(Slot slot, boolean enabled) {
        AlloyForgeNetworking.CHANNEL.clientHandle().send(new DisableSlotToggle(this.handler.forge, slot.getIndex(), !enabled));

        super.onSlotChangedState(slot.id, this.handler.syncId, enabled);
        float f = enabled ? 1.0F : 0.75F;
        this.handler.player().playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.4F, f);
    }

    @Override
    public void drawSlot(DrawContext context, Slot slot) {
        if (slot instanceof ForgeInputSlot crafterInputSlot && this.handler.isSlotDisabled(slot)) {
            this.drawDisabledSlot(context, crafterInputSlot);

            super.drawSlot(context, slot);

            return;
        }

        super.drawSlot(context, slot);
    }

    private void drawDisabledSlot(DrawContext context, ForgeInputSlot slot) {
        context.drawTexture(DISABLED_SLOT_TEXTURE, slot.x - 1, slot.y - 1, 3, 0, 0, 18, 18, 18, 18);
    }

    public int rootX() {
        return this.leftPos;
    }

    public int rootY() {
        return this.topPos;
    }
}
