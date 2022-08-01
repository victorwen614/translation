package dev.xkmc.l2artifacts.content.effects.v2;

import dev.xkmc.l2artifacts.content.config.ArtifactSetConfig;
import dev.xkmc.l2artifacts.content.effects.AttrSetEntry;
import dev.xkmc.l2artifacts.content.effects.AttributeSetEffect;
import dev.xkmc.l2artifacts.init.registrate.entries.LinearFuncEntry;
import dev.xkmc.l2library.init.events.attack.AttackCache;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.List;

public class PhysicalDamageEffect extends AttributeSetEffect {

	private final LinearFuncEntry factor;

	public PhysicalDamageEffect(AttrSetEntry entry, LinearFuncEntry factor) {
		super(entry);
		this.factor = factor;
	}

	@Override
	public List<MutableComponent> getDetailedDescription(int rank) {
		var ans = super.getDetailedDescription(rank);
		double val = factor.getFromRank(rank) * 100;
		ans.add(Component.translatable(getDescriptionId() + ".desc", (int) Math.round(val)));
		return ans;
	}

	@Override
	public void playerHurtOpponentEvent(Player player, ArtifactSetConfig.Entry ent, int rank, AttackCache event) {
		LivingHurtEvent hurt = event.getLivingHurtEvent();
		assert hurt != null;
		if (hurt.getSource().isMagic()) {
			event.setDamageModified((float) (event.getDamageModified() * factor.getFromRank(rank)));
		}
	}
}
