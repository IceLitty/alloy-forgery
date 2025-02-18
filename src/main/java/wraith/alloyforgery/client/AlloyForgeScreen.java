package wraith.alloyforgery.client;

import io.wispforest.owo.client.screens.OwoScreenHandler;
import io.wispforest.owo.ui.base.BaseUIModelHandledScreen;
import io.wispforest.owo.ui.base.BaseUIModelScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.TextureComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.PositionedRectangle;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.inject.ComponentStub;
import io.wispforest.owo.ui.util.MatrixStackTransformer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
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

        ButtonComponent component;
        try {
            component = (ButtonComponent) layout.childById((Class<io.wispforest.owo.ui.core.Component>) (Object) ButtonComponent.class, "slot-toggle-btn");
        } catch (Exception e) {
            throw new RuntimeException("Can not resolve owo-lib because AbstractWidget is not implements by ComponentStub", e);
        }
        component.onPress(btn -> {
                    this.allowSlotToggling = !allowSlotToggling;

                    ((io.wispforest.owo.ui.core.Component) btn).tooltip(Component.translatable("tooltip.alloy_forgery.slot_toggle_" + (this.allowSlotToggling ? "enable" : "disable")));
                })
                .renderer((context, button, delta) -> {
                    ButtonComponent.Renderer.VANILLA.draw(context, button, delta);

                    ((MatrixStackTransformer) context).push().translate(-0.75, -0.75, 0);

                    context.drawCenteredString(Minecraft.getInstance().font, "⏻", ((ComponentStub) button).x() + 8, ((ComponentStub) button).y() + 4, 0xFFFFFFFF);

                    ((MatrixStackTransformer) context).pop();
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
                && this.hoveredSlot instanceof ForgeInputSlot
                && this.menu.getCarried().isEmpty()
                && !this.hoveredSlot.hasItem()
                && !((OwoScreenHandler) this.menu).player().isSpectator()) {

            if (this.menu.isSlotDisabled(this.hoveredSlot)) {
                context.renderTooltip(this.font, DISABLED_SLOT_TEXT, mouseX, mouseY);
            } else {
                context.renderTooltip(this.font, ENABLED_SLOT_TEXT, mouseX, mouseY);
            }
        }
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int button, ClickType actionType) {
        var player = ((OwoScreenHandler) this.menu).player();

        if(allowSlotToggling) {
            if (slot instanceof ForgeInputSlot && !slot.hasItem() && !player.isSpectator()) {
                switch (actionType) {
                    case PICKUP:
                        if (this.menu.isSlotDisabled(slot)) {
                            this.enableInputSlot(slot);
                        } else /*if (this.menu.getCarried().isEmpty())*/ {
                            this.disableInputSlot(slot);
                        }
                        break;
                    case SWAP:
                        ItemStack itemStack = player.getInventory().getItem(button);
                        if (this.menu.isSlotDisabled(slot) && !itemStack.isEmpty()) {
                            this.enableInputSlot(slot);
                        }
                }
            }
        }

        super.slotClicked(slot, slotId, button, actionType);
    }

    private void enableInputSlot(Slot slot) {
        this.setSlotEnabled(slot, true);
    }

    private void disableInputSlot(Slot slot) {
        this.setSlotEnabled(slot, false);
    }

    private void setSlotEnabled(Slot slot, boolean enabled) {
        AlloyForgeNetworking.CHANNEL.clientHandle().send(new DisableSlotToggle(this.menu.forge, slot.getContainerSlot(), !enabled));

        super.handleSlotStateChanged(slot.index, this.menu.containerId, enabled);
        float f = enabled ? 1.0F : 0.75F;
        ((OwoScreenHandler) this.menu).player().playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.4F, f);
    }

    @Override
    public void renderSlot(GuiGraphics context, Slot slot) {
        if (slot instanceof ForgeInputSlot crafterInputSlot && this.menu.isSlotDisabled(slot)) {
            this.drawDisabledSlot(context, crafterInputSlot);

            super.renderSlot(context, slot);

            return;
        }

        super.renderSlot(context, slot);
    }

    private void drawDisabledSlot(GuiGraphics context, ForgeInputSlot slot) {
        context.blit(DISABLED_SLOT_TEXTURE, slot.x - 1, slot.y - 1, 3, 0, 0, 18, 18, 18, 18);
    }

    public int rootX() {
        return this.leftPos;
    }

    public int rootY() {
        return this.topPos;
    }
}
