package statistics.yara_results;

import java.util.ArrayList;
import java.util.List;

public class YaraStatsCSV {
	
	public YaraStatsCSV(int samplesCount, int coveredCount, int diff, int falsePositiveCount, int alternative_counter, List<String> alternative_entries) {
		this.samplesCount = samplesCount;
		this.coveredCount = coveredCount;
		this.diff = diff;
		this.falsePositiveCount = falsePositiveCount;
		this.alternative_counter = alternative_counter;
		
		if(alternative_entries == null) {
			this.alternative_entries = new ArrayList<String>();
		} else {
			this.alternative_entries = alternative_entries;

		}
	}

	public int samplesCount;
	public int coveredCount;
	public int diff;
	public int falsePositiveCount;
	public int alternative_counter;
	public List<String> alternative_entries;
	

	public String toCSV() {
		StringBuilder sb = new StringBuilder();
		
		for(String s : alternative_entries) {
			sb.append(s);
			sb.append(",");
		}
		
		//Remove last char:
		if(sb.length()>1) {
			sb.setLength(sb.length()-1);
		}
		
		return samplesCount + ";" + coveredCount + ";" + diff + ";" + falsePositiveCount + ";" + alternative_counter + ";" + sb.toString();
	}


	@Override
	public String toString() {
		return "RuleStats [samplesCount=" + samplesCount + ", coveredCount=" + coveredCount + ", diff=" + diff
				+ ", falsePositiveCount=" + falsePositiveCount + ", alternative_counter=" + alternative_counter
				+ ", alternative_entries=" + alternative_entries.toString() + "]";
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + alternative_counter;
		result = prime * result + ((alternative_entries == null) ? 0 : alternative_entries.hashCode());
		result = prime * result + coveredCount;
		result = prime * result + diff;
		result = prime * result + falsePositiveCount;
		result = prime * result + samplesCount;
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof YaraStatsCSV)) {
			return false;
		}
		YaraStatsCSV other = (YaraStatsCSV) obj;
		if (alternative_counter != other.alternative_counter) {
			return false;
		}
		if (alternative_entries == null) {
			if (other.alternative_entries != null) {
				return false;
			}
		} else if (!alternative_entries.equals(other.alternative_entries)) {
			return false;
		}
		if (coveredCount != other.coveredCount) {
			return false;
		}
		if (diff != other.diff) {
			return false;
		}
		if (falsePositiveCount != other.falsePositiveCount) {
			return false;
		}
		if (samplesCount != other.samplesCount) {
			return false;
		}
		return true;
	}
}
