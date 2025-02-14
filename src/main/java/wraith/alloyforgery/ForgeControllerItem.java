package wraith.alloyforgery;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import wraith.alloyforgery.block.ForgeControllerBlock;
import wraith.alloyforgery.forges.ForgeDefinition;
import java.util.List;

public class ForgeControllerItem extends BlockItem {

    public ForgeControllerItem(ForgeControllerBlock block, Item.Properties settings) {
        super(block, settings);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        tooltip.add(Component.translatable("tooltip.alloy_forgery.forge_tier", getForgeDefinition().forgeTier()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.alloy_forgery.fuel_capacity", getForgeDefinition().fuelCapacity()).withStyle(ChatFormatting.GRAY));
    }

    public ForgeDefinition getForgeDefinition() {
        return ((ForgeControllerBlock) getBlock()).forgeDefinition;
    }
}
