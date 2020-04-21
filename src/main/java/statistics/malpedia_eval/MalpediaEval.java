package statistics.malpedia_eval;

public class MalpediaEval {
	
	/*
	 * False Positives:
	 * These entries are parsed from the JSON document
	 * In our first approach we will only blacklist all the
	 * instruction opcodes that lead to false positives.
	 */
	public FalsePositives fps;
	
	/*
	 * False Negatives:
	 * These are samples we discovered running YARA that were not covered by their family rule.
	 * We will add YARA sequences for some instructions of the affected samples
	 * to these rules to detect them in a later run.
	 */
	public FalseNegatives fns;
	
	// Not used but works
	public FpSequenceStats fp_sequence_stats;
	
	// Not used but works
	public Statistics statistics;
	
	public String toString() {
		return "===== FPs =====\n" + fps.toString()
				+ "\n===== FNs =====\n" + fns.toString()
				+ "\n===== stats =====\n" + statistics.toString()
				+ "\n===== fp_seq_stats =====\n" + fp_sequence_stats.toString();
	}

	public void setFpSequenceStats(FpSequenceStats fp_sequence_stats) {
		this.fp_sequence_stats = fp_sequence_stats;
	}
}
