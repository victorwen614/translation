package dev.xkmc.l2artifacts.content.core;

import dev.xkmc.l2artifacts.content.config.ArtifactSetConfig;
import dev.xkmc.l2artifacts.init.data.LangData;
import dev.xkmc.l2artifacts.init.data.ModConfig;
import dev.xkmc.l2artifacts.init.registrate.ArtifactRegistry;
import dev.xkmc.l2library.base.NamedEntry;
import dev.xkmc.l2library.util.Proxy;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotResult;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ArtifactSet extends NamedEntry<ArtifactSet> {

	public record SetContext(int count, int min_rank, int current_index) {

	}

	private MutableComponent getCountDesc(int count) {
		return LangData.getTranslate("set." + count);
	}

	public ArtifactSet() {
		super(() -> ArtifactRegistry.SET);
	}

	public Optional<SetContext> getCount(@Nullable SlotContext context) {
		LivingEntity e = context == null ? Proxy.getPlayer() : context.entity();
		if (e instanceof Player player) {
			List<SlotResult> list = CuriosApi.getCuriosHelper().findCurios(player, stack -> stack.getItem() instanceof BaseArtifact artifact && artifact.set.get() == this);
			int rank = ModConfig.COMMON.maxRank.get();
			int index = -1;
			for (int i = 0; i < list.size(); i++) {
				SlotResult result = list.get(i);
				if (result.stack().getItem() instanceof BaseArtifact artifact) {
					rank = Math.min(rank, artifact.rank);
					if (context != null && context.index() == result.slotContext().index())
						index = i;
				}
			}
			return Optional.of(new SetContext(list.size(), rank, index));
		}
		return Optional.empty();
	}

	public void update(SlotContext context) {
		LivingEntity e = context.entity();
		if (e instanceof Player player) {
			Optional<SetContext> result = getCount(context);
			if (result.isPresent()) {
				ArtifactSetConfig config = ArtifactSetConfig.getInstance();
				ArrayList<ArtifactSetConfig.Entry> list = config.map.get(this);
				for (ArtifactSetConfig.Entry ent : list) {
					ent.effect.update(player, ent, result.get().min_rank(), result.get().count() >= ent.count);
				}
			}
		}
	}

	public void tick(SlotContext context) {
		LivingEntity e = context.entity();
		if (e instanceof Player player) {
			Optional<SetContext> result = getCount(context);
			if (result.isPresent() && result.get().current_index() == 0) {
				ArtifactSetConfig config = ArtifactSetConfig.getInstance();
				ArrayList<ArtifactSetConfig.Entry> list = config.map.get(this);
				for (ArtifactSetConfig.Entry ent : list) {
					ent.effect.tick(player, ent, result.get().min_rank(), result.get().count() >= ent.count);
				}
			}
		}
	}

	public List<MutableComponent> getAllDescs(ItemStack stack, TooltipFlag flag) {
		List<MutableComponent> ans = new ArrayList<>();
		BaseArtifact artifact = (BaseArtifact) stack.getItem();
		if (Proxy.getPlayer() != null) {
			Optional<SetContext> opt = getCount(null);
			if (opt.isPresent()) {
				SetContext ctx = opt.get();
				ArtifactSetConfig config = ArtifactSetConfig.getInstance();
				ArrayList<ArtifactSetConfig.Entry> list = config.map.get(this);
				for (ArtifactSetConfig.Entry ent : list) {
					ChatFormatting color_count = ctx.count() < ent.count ?
							ChatFormatting.GRAY : ChatFormatting.GREEN;
					ChatFormatting color_title = ctx.count() < ent.count || ctx.min_rank() < artifact.rank ?
							ChatFormatting.GRAY : ChatFormatting.GREEN;
					ChatFormatting color_desc = ctx.count() < ent.count || ctx.min_rank() < artifact.rank ?
							ChatFormatting.DARK_GRAY : ChatFormatting.DARK_GREEN;
					ans.add(getCountDesc(ent.count).withStyle(color_count).append(ent.effect.getDesc().withStyle(color_title)));
					List<MutableComponent> desc = ent.effect.getDetailedDescription(artifact);
					for (MutableComponent comp : desc) {
						ans.add(comp.withStyle(color_desc));
					}
				}
			}
		}
		return ans;
	}

}
