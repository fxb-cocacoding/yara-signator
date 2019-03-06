package prefiltering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import converters.ngrams.Ngram;

public class PrefilterSystem {
	
	private List<Ngram> ngramList;
	
	public List<Ngram> getNgramList() {
		return ngramList;
	}

	public void setNgramList(List<Ngram> ngramList) {
		this.ngramList = ngramList;
	}

	private PrefilterFactory rankingSystem = new PrefilterFactory();
	
	public PrefilterSystem(List<Ngram> ngrams) {
		this.ngramList = new ArrayList<>(ngrams);
	}
	
	public List<Ngram> process(String function) {
		Prefilter calc = rankingSystem.getPrefilter(function);
		for(int i=0;i<ngramList.size();i++) {
			Ngram n = calc.calc(ngramList.get(i));
			ngramList.set(i, n);
		}
		return ngramList;
	}
	
	public void randomize(Random rnd) {
		Collections.shuffle(ngramList, rnd);
	}

}
