package ranking_system;

import java.util.List;

import converters.ngrams.Ngram;

public abstract class Calculator {
	abstract public int calc(Ngram ngram, List<Ngram> ngramList);
}
