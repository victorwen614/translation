package dev.xkmc.l2artifacts.content.upgrades;

import dev.xkmc.l2artifacts.content.core.ArtifactStats;
import dev.xkmc.l2artifacts.content.core.StatEntry;
import dev.xkmc.l2artifacts.init.data.ModConfig;
import net.minecraft.util.RandomSource;

import javax.annotation.Nullable;

public class ArtifactUpgradeManager {

	public static int getExpForLevel(int rank, int level) {
		double rank_factor = ModConfig.COMMON.expConsumptionRankFactor.get();
		double level_factor = ModConfig.COMMON.expLevelFactor.get();
		double base = ModConfig.COMMON.baseExpConsumption.get();
		return (int) Math.round(base * Math.pow(level_factor, level) * Math.pow(rank_factor, rank - 1));
	}

	public static int getExpForConversion(int rank, @Nullable ArtifactStats stat) {
		int base = ModConfig.COMMON.baseExpConversion.get();
		double base_factor = ModConfig.COMMON.expConversionRankFactor.get();
		double retention = ModConfig.COMMON.expRetention.get();
		double base_exp = base * Math.pow(base_factor, rank - 1);
		if (stat == null) {
			return (int) Math.round(base_exp);
		}
		double used_exp = stat.exp;
		for (int i = 0; i < stat.level; i++) {
			used_exp += getExpForLevel(stat.rank, stat.level);
		}
		return (int) Math.round(base_exp + used_exp * retention);
	}

	public static int getMaxLevel(int rank) {
		return rank * ModConfig.COMMON.maxLevelPerRank.get();
	}

	public static void onUpgrade(ArtifactStats stats, int lv, Upgrade upgrade, RandomSource random) {
		int gate = ModConfig.COMMON.levelPerSubStat.get();
		stats.add(stats.main_stat.type, stats.main_stat.type.getMainValue(stats.rank, random, upgrade.removeMain()));
		if (lv % gate == 0 && stats.sub_stats.size() > 0) {
			StatEntry substat = null;
			if (upgrade.stats.size() > 0) {
				for (StatEntry entry : stats.sub_stats) {
					if (entry.type == upgrade.stats.get(0)) {
						upgrade.stats.remove(0);
						substat = entry;
						break;
					}
				}
			}
			if (substat == null) {
				substat = stats.sub_stats.get(random.nextInt(stats.sub_stats.size()));
			}
			stats.add(substat.type, substat.type.getSubValue(stats.rank, random, upgrade.removeSub()));
		}
	}
}
