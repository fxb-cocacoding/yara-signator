package ranking_system;

import java.util.List;

import converters.ngrams.Ngram;

public class RankingDummy extends Calculator {
	public int calc(Ngram ngram, List<Ngram> ngramList) {
		return 1;
	}
}
