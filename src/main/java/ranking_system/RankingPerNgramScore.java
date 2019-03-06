package ranking_system;

import java.util.List;

import converters.ngrams.Ngram;

public class RankingPerNgramScore extends Calculator {

	@Override
	public int calc(Ngram ngram, List<Ngram> ngramList) {
		// TODO Auto-generated method stub
		return ngram.score;
	}

}
