package statistics.malpedia_eval;

import java.util.List;

public class WrongCoveredSample {
	public String samples_name;
	public List<Sequence> sequences;
	
	public String toString() {
		return "  sample " + samples_name + " \n" + sequences.toString();
	}
}


