package dev.xkmc.l2artifacts.content.search.upgrade;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.xkmc.l2artifacts.content.core.ArtifactStats;
import dev.xkmc.l2artifacts.content.core.BaseArtifact;
import dev.xkmc.l2artifacts.content.core.StatEntry;
import dev.xkmc.l2artifacts.content.search.filter.FilterScreen;
import dev.xkmc.l2artifacts.content.search.recycle.RecycleMenuScreen;
import dev.xkmc.l2artifacts.content.search.tabs.FilterTabManager;
import dev.xkmc.l2artifacts.content.search.tabs.IFilterScreen;
import dev.xkmc.l2artifacts.init.data.LangData;
import dev.xkmc.l2library.base.menu.BaseContainerScreen;
import dev.xkmc.l2library.base.menu.stacked.StackedRenderHandle;
import dev.xkmc.l2library.util.Proxy;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class UpgradeMenuScreen extends BaseContainerScreen<UpgradeMenu> implements IFilterScreen {

	private static int lerpColor(float perc, int fg, int bg) {
		int c0 = Math.round(Mth.lerp(perc, fg & 0xFF, bg & 0xFF));
		fg >>= 8;
		bg >>= 8;
		int c1 = Math.round(Mth.lerp(perc, fg & 0xFF, bg & 0xFF));
		fg >>= 8;
		bg >>= 8;
		int c2 = Math.round(Mth.lerp(perc, fg & 0xFF, bg & 0xFF));
		return c2 << 16 | c1 << 8 | c0;
	}

	private static final int MAX_TIME = 60;
	private static final ChatFormatting MAIN = ChatFormatting.DARK_GREEN;
	private static final ChatFormatting SUB = ChatFormatting.DARK_BLUE;

	private boolean pressed = false;

	private float time = 0;
	private ArtifactStats old = null, current = null;

	public UpgradeMenuScreen(UpgradeMenu cont, Inventory plInv, Component title) {
		super(cont, plInv, LangData.TAB_UPGRADE.get());
	}

	@Override
	protected final void init() {
		super.init();
		new FilterTabManager(this, menu.token).init(this::addRenderableWidget, FilterTabManager.UPGRADE);
	}

	@Override
	protected void renderBg(PoseStack pose, float pTick, int mx, int my) {
		var sr = menu.sprite.getRenderer(this);
		sr.start(pose);
		var rect = menu.sprite.getComp("upgrade");
		if (isHovering(rect.x, rect.y, rect.w, rect.h, mx, my)) {
			if (pressed) {
				sr.draw(pose, "upgrade", "upgrade_on");
			}
			FilterScreen.renderHighlight(pose, leftPos + rect.x, topPos + rect.y, rect.w, rect.h, getBlitOffset(), -2130706433);
		}
		if (time > 0) {
			time -= Minecraft.getInstance().getDeltaFrameTime();
			if (time <= 0) {
				time = 0;
				old = null;
			}
		}
	}

	@Override
	public boolean mouseClicked(double mx, double my, int btn) {
		pressed = true;
		return super.mouseClicked(mx, my, btn);
	}

	@Override
	public boolean mouseReleased(double mx, double my, int btn) {
		pressed = false;
		var rect = menu.sprite.getComp("upgrade");
		if (isHovering(rect.x, rect.y, rect.w, rect.h, mx, my)) {
			old = current;
			time = MAX_TIME;
			click(0);
		}
		return super.mouseReleased(mx, my, btn);
	}

	@Override
	protected void renderLabels(PoseStack pose, int mx, int my) {
		super.renderLabels(pose, mx, my);
		pose.pushPose();
		pose.translate(0, 45, 0);
		StackedRenderHandle handle = new StackedRenderHandle(this, pose, menu.sprite, 0);
		int exp = menu.experience.get();
		int cost = menu.exp_cost.get();
		int plexp = menu.player_cost.get();
		if (cost > 0) {
			String str = RecycleMenuScreen.formatNumber(cost) + "/" + RecycleMenuScreen.formatNumber(exp);
			handle.drawText(LangData.TAB_INFO_EXP_COST.get(Component.literal(str)
					.withStyle(cost <= exp ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED)));
			var player = Proxy.getClientPlayer();
			boolean canUpgrade = player.getAbilities().instabuild || plexp <= player.experienceLevel;
			handle.drawText(Component.translatable("container.repair.cost", Component.literal("" + plexp)
					.withStyle(canUpgrade ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED)));
			ItemStack stack = menu.container.getItem(0);
			var opt = BaseArtifact.getStats(stack);
			if (opt.isPresent()) {
				ArtifactStats stat = opt.get();
				List<Component[]> table = new ArrayList<>();
				table.add(addEntry(true, stat.main_stat,
						old == null ? null : old.main_stat));
				for (int i = 0; i < stat.sub_stats.size(); i++) {
					table.add(addEntry(false, stat.sub_stats.get(i),
							old == null ? null : old.sub_stats.get(i)));
				}
				handle.drawTable(table.toArray(Component[][]::new), imageWidth);
				current = stat;
			}
		} else {
			handle.drawText(LangData.TAB_INFO_EXP.get(exp));
		}
		handle.flushText();
		pose.popPose();
	}

	private Component[] addEntry(boolean main, StatEntry entry, @Nullable StatEntry old) {
		Component[] ans = new Component[3];
		ans[0] = Component.translatable(entry.type.attr.get().getDescriptionId()).withStyle(main ? MAIN : SUB);
		ans[1] = entry.type.getValueText(entry.value).withStyle(main ? MAIN : SUB);
		if (old != null) {
			double diff = entry.value - old.value;
			if (diff > 1e-3) {
				Integer fg = ChatFormatting.DARK_PURPLE.getColor();
				assert fg != null;
				float perc = 1 - time / MAX_TIME;
				int c = lerpColor(perc, fg, 0xC6C6C6);
				ans[2] = entry.type.getValueText(diff).withStyle(Style.EMPTY.withColor(c));
			} else {
				ans[2] = Component.empty();
			}
		} else {
			ans[2] = Component.empty();
		}
		entry.getTooltip();
		return ans;
	}

	@Override
	public int screenWidth() {
		return width;
	}

	@Override
	public int screenHeight() {
		return height;
	}

}
