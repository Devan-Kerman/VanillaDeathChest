package com.therandomlabs.vanilladeathchest.mixin;

import java.util.ArrayList;
import java.util.List;
import com.therandomlabs.vanilladeathchest.api.event.player.PlayerDropAllItemsCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.DefaultedList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerInventory.class)
public class MixinPlayerInventory {
	@Shadow
	@Final
	public PlayerEntity player;

	private final ArrayList<ItemEntity> drops = new ArrayList<>();

	@Shadow
	@Final
	private List<DefaultedList<ItemStack>> combinedInventory;

	@SuppressWarnings("unchecked")
	@Inject(method = "dropAll", at = @At("TAIL"))
	public void dropAll(CallbackInfo callback) {
		if(!PlayerDropAllItemsCallback.EVENT.invoker().onPlayerDropAllItems(
				(ServerWorld) player.getEntityWorld(), player, (List<ItemEntity>) drops.clone()
		)) {
			drops.forEach(Entity::remove);
		}

		drops.clear();
	}

	@Redirect(method = "dropAll", at = @At(
			value = "INVOKE",
			target = "net/minecraft/entity/player/PlayerEntity.dropItem(" +
					"Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;"
	))
	private ItemEntity dropItem(
			PlayerEntity player, ItemStack stack, boolean flag1, boolean flag2
	) {
		final ItemEntity item = player.dropItem(stack, flag1, flag2);
		drops.add(item);
		return item;
	}
}
