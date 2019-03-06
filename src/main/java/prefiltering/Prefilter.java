package prefiltering;

import java.util.List;

import converters.ngrams.Ngram;

public abstract class Prefilter {
	abstract public Ngram calc(Ngram ngram);
}
