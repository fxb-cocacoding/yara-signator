package statistics.yara_results;

import java.util.Set;

public class YaraResult {
	public String yaraRule;
	public Set<String> coveredSamples;
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((coveredSamples == null) ? 0 : coveredSamples.hashCode());
		result = prime * result + ((yaraRule == null) ? 0 : yaraRule.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		YaraResult other = (YaraResult) obj;
		if (coveredSamples == null) {
			if (other.coveredSamples != null)
				return false;
		} else if (!coveredSamples.equals(other.coveredSamples))
			return false;
		if (yaraRule == null) {
			if (other.yaraRule != null)
				return false;
		} else if (!yaraRule.equals(other.yaraRule))
			return false;
		return true;
	}
}
