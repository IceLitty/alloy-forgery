package wraith.alloyforgery.client;

import io.wispforest.owo.ui.base.BaseUIModelHandledScreen;
import io.wispforest.owo.ui.base.BaseUIModelScreen;
import io.wispforest.owo.ui.component.TextureComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.PositionedRectangle;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import wraith.alloyforgery.AlloyForgeScreenHandler;
import wraith.alloyforgery.AlloyForgery;
import java.util.List;

public class AlloyForgeScreen extends BaseUIModelHandledScreen<FlowLayout, AlloyForgeScreenHandler> {

    private TextureComponent fuelGauge;
    private TextureComponent progressGauge;
    private TextureComponent invalidCross;
    private FlowLayout lavaBar;

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
    }

    public int rootX() {
        return this.leftPos;
    }

    public int rootY() {
        return this.topPos;
    }
}
