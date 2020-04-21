package aggregation_filter;

import java.util.List;

import converters.ngrams.Ngram;

public class FilterDummy {
	public List<Ngram> apply(List<Ngram> ngrams) {
		return ngrams;
	}
}
