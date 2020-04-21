package statistics.malpedia_eval;

import java.util.List;

public class WrongSignature {
	public String wrongDetectedFamilyName;
	public List<WrongCoveredSample> wrongSamples;
	
	public String toString() {
		return "\n detected: " + wrongDetectedFamilyName + " as false sample for the rule\n" + wrongSamples.toString();
	}
}
