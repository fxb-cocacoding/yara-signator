package statistics.malpedia_eval;

public class Statistics {
	public String clean_rules;
	public String f1_precision;
	public String f1_recall;
	public String f1_score;
	public String false_negatives;
	public String false_positives;
	public String families;
	public String families_covered;
	public String rules_without_fn;
	public String rules_without_fp;
	public String samples_all;
	public String samples_detectable;
	public String true_negatives;
	public String true_positives;
	public String true_positives_bonus;
	
	@Override
	public String toString() {
		return "Statistics [clean_rules=" + clean_rules + ", f1_precision=" + f1_precision + ", f1_recall=" + f1_recall
				+ ", f1_score=" + f1_score + ", false_negatives=" + false_negatives + ", false_positives="
				+ false_positives + ", families=" + families + ", families_covered=" + families_covered
				+ ", rules_without_fn=" + rules_without_fn + ", rules_without_fp=" + rules_without_fp + ", samples_all="
				+ samples_all + ", samples_detectable=" + samples_detectable + ", true_negatives=" + true_negatives
				+ ", true_positives=" + true_positives + ", true_positives_bonus=" + true_positives_bonus + "]";
	}
	
	
}
