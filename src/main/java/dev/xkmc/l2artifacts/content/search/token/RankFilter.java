package dev.xkmc.l2artifacts.content.search.token;

import dev.xkmc.l2artifacts.content.core.BaseArtifact;
import dev.xkmc.l2artifacts.init.data.LangData;
import dev.xkmc.l2library.serial.SerialClass;
import dev.xkmc.l2library.util.code.GenericItemStack;

import java.util.Comparator;

@SerialClass
public class RankFilter extends ArtifactFilter<RankToken> {

	public RankFilter(IArtifactFilter parent, LangData desc) {
		super(parent, desc, RankToken.ALL_RANKS, (item, t) -> item.item().rank == t.rank());
	}

	@Override
	public Comparator<GenericItemStack<BaseArtifact>> getComparator() {
		return Comparator.comparingInt(e -> -e.item().rank);
	}
}
