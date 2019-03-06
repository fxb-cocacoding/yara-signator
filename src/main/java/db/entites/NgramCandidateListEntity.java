package db.entites;

import java.util.List;

@Deprecated
public class NgramCandidateListEntity {
	public String _id;
	public int ngramsCounted;
	public List<NgramCandidateEntity> detectedNgrams;
}
