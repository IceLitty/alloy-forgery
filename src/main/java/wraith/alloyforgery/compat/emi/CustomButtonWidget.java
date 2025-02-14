package wraith.alloyforgery.compat.emi;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.emi.emi.api.widget.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.function.BooleanSupplier;

public class CustomButtonWidget extends Widget {

    private final int x, y;
    private final BooleanSupplier isActive;
    private final ButtonWidget.ClickAction action;
    private final List<ClientTooltipComponent> tooltipComponent = List.of(ClientTooltipComponent.create(Component.translatable("container.alloy_forgery.rei.button").getVisualOrderText()));

    public CustomButtonWidget(int x, int y, BooleanSupplier isActive, ButtonWidget.ClickAction action) {
        this.x = x;
        this.y = y;
        this.isActive = isActive;
        this.action = action;
    }

    @Override
    public Bounds getBounds() {
        return new Bounds(x, y, 12, 12);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        RenderSystem.setShaderTexture(0, AlloyForgeryEmiRecipe.GUI_TEXTURE);
        int v = 68;
        boolean active = this.isActive.getAsBoolean();
        if (!active) {
            v += 24;
        } else if (getBounds().contains(mouseX, mouseY)) {
            v += 12;
        }
        RenderSystem.enableDepthTest();
        context.blit(AlloyForgeryEmiRecipe.GUI_TEXTURE, this.x, this.y, 176, v, 12, 12, 256, 256);
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!isActive.getAsBoolean()) return false;
        action.click(mouseX, mouseY, button);
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        return true;
    }

    @Override
    public List<ClientTooltipComponent> getTooltip(int mouseX, int mouseY) {
        return tooltipComponent;
    }
}
