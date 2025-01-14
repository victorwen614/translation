package dev.xkmc.l2artifacts.content.effects.v1;

import dev.xkmc.l2artifacts.content.capability.ArtifactData;
import dev.xkmc.l2artifacts.content.config.ArtifactSetConfig;
import dev.xkmc.l2artifacts.content.effects.SetEffect;
import dev.xkmc.l2artifacts.init.registrate.entries.LinearFuncEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.CriticalHitEvent;

import java.util.List;

public class DamoclesSword extends SetEffect {

	private final LinearFuncEntry amplify;

	public DamoclesSword(LinearFuncEntry amplify) {
		super(0);
		this.amplify = amplify;
	}

	@Override
	public void tick(Player player, ArtifactSetConfig.Entry ent, int rank, boolean enabled) {
		if (!enabled || player.level.isClientSide())
			return;
		if (player.getHealth() > 0 && player.getHealth() < player.getMaxHealth() / 2 && player.hurtTime == 0) {
			if (ArtifactData.HOLDER.get(player).tickSinceDeath > 60)
				player.hurt(DamageSource.OUT_OF_WORLD, player.getMaxHealth());
		}
	}

	@Override
	public List<MutableComponent> getDetailedDescription(int rank) {
		double amplify = this.amplify.getFromRank(rank);
		int amount = (int) Math.round(amplify * 100);
		return List.of(Component.translatable(getDescriptionId() + ".desc", amount));
	}

	@Override
	public void playerAttackModifyEvent(Player player, ArtifactSetConfig.Entry ent, int rank, CriticalHitEvent crit) {
		if (player.getHealth() < player.getMaxHealth()) return;
		double amplify = 1 + this.amplify.getFromRank(rank);
		crit.setDamageModifier((float) (crit.getDamageModifier() * amplify));
	}

}
