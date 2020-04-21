package prefiltering;

import converters.ngrams.Ngram;

public abstract class Prefilter {
	abstract public Ngram calc(Ngram ngram);
}
