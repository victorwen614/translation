package dev.xkmc.l2artifacts.content.core;

import dev.xkmc.l2artifacts.content.upgrades.ArtifactUpgradeManager;
import dev.xkmc.l2artifacts.content.upgrades.Upgrade;
import dev.xkmc.l2artifacts.init.data.LangData;
import dev.xkmc.l2artifacts.util.ItemCompoundTag;
import dev.xkmc.l2library.serial.codec.TagCodec;
import dev.xkmc.l2library.util.Proxy;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class BaseArtifact extends Item {

	public static final String KEY = "ArtifactData", UPGRADE = "Upgrade";

	protected static Rarity getRarity(int rank) {
		return rank <= 2 ? Rarity.UNCOMMON : rank <= 4 ? Rarity.RARE : Rarity.EPIC;
	}

	public static void upgrade(ItemStack stack, int exp, RandomSource random) {
		ItemCompoundTag tag = ItemCompoundTag.of(stack).getSubTag(KEY);
		if (tag.isPresent()) {
			ArtifactStats stats = TagCodec.fromTag(tag.getOrCreate(), ArtifactStats.class);
			assert stats != null;
			stats.addExp(exp, random);
			CompoundTag newTag = TagCodec.toTag(new CompoundTag(), stats);
			assert newTag != null;
			tag.setTag(newTag);
		}
	}

	public static Optional<ArtifactStats> getStats(ItemStack stack) {
		return CuriosApi.getCuriosHelper().getCurio(stack).filter(e -> e instanceof ArtifactCurioCap)
				.flatMap(e -> ((ArtifactCurioCap) e).getStats());
	}

	public static Optional<Upgrade> getUpgrade(ItemStack stack) {
		ItemCompoundTag tag = ItemCompoundTag.of(stack).getSubTag(UPGRADE);
		if (tag.isPresent()) {
			return Optional.ofNullable(TagCodec.fromTag(tag.getOrCreate(), Upgrade.class));
		}
		return Optional.empty();
	}

	public static ItemStack setUpgrade(ItemStack stack, Upgrade upgrade) {
		CompoundTag tag = TagCodec.toTag(new CompoundTag(), upgrade);
		if (tag != null)
			ItemCompoundTag.of(stack).getSubTag(UPGRADE).setTag(tag);
		return stack;
	}

	public final Supplier<ArtifactSet> set;
	public final Supplier<ArtifactSlot> slot;
	public final int rank;

	public BaseArtifact(Properties properties, Supplier<ArtifactSet> set, Supplier<ArtifactSlot> slot, int rank) {
		super(properties.stacksTo(1).rarity(getRarity(rank)));
		this.set = set;
		this.slot = slot;
		this.rank = rank;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		ItemCompoundTag tag = ItemCompoundTag.of(stack).getSubTag(KEY);
		Upgrade upgrade = getUpgrade(stack).orElse(new Upgrade());
		if (!tag.isPresent()) {
			if (!level.isClientSide()) {
				ArtifactStats stats = new ArtifactStats(slot.get(), rank, upgrade, level.random);
				CompoundTag newTag = TagCodec.toTag(new CompoundTag(), stats);
				assert newTag != null;
				tag.setTag(newTag);
				setUpgrade(stack, upgrade);
			}
			return InteractionResultHolder.success(stack);
		} else {
			Optional<ArtifactStats> opt = getStats(stack);
			if (opt.isPresent()) {
				ArtifactStats stats = opt.get();
				if (stats.level > stats.old_level) {
					if (!level.isClientSide()) {
						for (int i = stats.old_level + 1; i <= stats.level; i++) {
							ArtifactUpgradeManager.onUpgrade(stats, i, upgrade, level.random);
						}
						stats.old_level = stats.level;
						CompoundTag newTag = TagCodec.toTag(new CompoundTag(), stats);
						assert newTag != null;
						tag.setTag(newTag);
						setUpgrade(stack, upgrade);
					}
					return InteractionResultHolder.success(stack);
				}
			}
		}
		return InteractionResultHolder.pass(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
		boolean shift = Screen.hasShiftDown();
		boolean ctrl = Screen.hasControlDown();
		if (Proxy.getPlayer() != null) {
			ItemCompoundTag tag = ItemCompoundTag.of(stack).getSubTag(KEY);
			if (!tag.isPresent()) {
				list.add(LangData.RAW_ARTIFACT.get());
			} else {
				getStats(stack).ifPresent(stats -> {
					boolean max = stats.level == ArtifactUpgradeManager.getMaxLevel(stats.rank);
					list.add(LangData.ARTIFACT_LEVEL.get(stats.level).withStyle(max ? ChatFormatting.GOLD : ChatFormatting.WHITE));
					if (stats.level < ArtifactUpgradeManager.getMaxLevel(stats.rank)) {
						if (shift && !ctrl)
							list.add(LangData.ARTIFACT_EXP.get(stats.exp, ArtifactUpgradeManager.getExpForLevel(stats.rank, stats.level)));
					}
					if (stats.level > stats.old_level) {
						list.add(LangData.UPGRADE.get());
					} else if (!shift && !ctrl) {
						list.add(LangData.MAIN_STAT.get());
						list.add(stats.main_stat.getTooltip());
						if (stats.sub_stats.size() > 0) {
							list.add(LangData.SUB_STAT.get());
							for (StatEntry ent : stats.sub_stats) {
								list.add(ent.getTooltip());
							}
						}
					}
				});
			}
			if (!ctrl)
				list.addAll(set.get().getAllDescs(stack, shift));
			else if (!shift) getUpgrade(stack).ifPresent(e -> e.addTooltips(list));
			if (shift && !ctrl)
				list.add(LangData.EXP_CONVERSION.get(ArtifactUpgradeManager.getExpForConversion(rank, getStats(stack).orElse(null))));
		}
		super.appendHoverText(stack, level, list, flag);
		if (!shift && !ctrl) {
			list.add(LangData.SHIFT_TEXT.get());
			list.add(LangData.CTRL_TEXT.get());
		}
	}
}
