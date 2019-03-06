package db.entites;

import converters.ngrams.Ngram;

@Deprecated
public class NgramCandidateEntity {
	public String family;
	public String filename;
	public String hash;
	public String concatedOpcodes;
	
	public Ngram ngram;
}
