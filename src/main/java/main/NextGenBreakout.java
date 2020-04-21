package main;

public class NextGenBreakout {
	public String score = "";
	public double score_limit = 0;
	public boolean FPs_allowed = false;
	
	@Override
	public String toString() {
		return "NextGenBreakout [score=" + score + ", score_limit=" + score_limit + ", FPs_allowed=" + FPs_allowed
				+ "]";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (FPs_allowed ? 1231 : 1237);
		result = prime * result + ((score == null) ? 0 : score.hashCode());
		long temp;
		temp = Double.doubleToLongBits(score_limit);
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
		if (!(obj instanceof NextGenBreakout)) {
			return false;
		}
		NextGenBreakout other = (NextGenBreakout) obj;
		if (FPs_allowed != other.FPs_allowed) {
			return false;
		}
		if (score == null) {
			if (other.score != null) {
				return false;
			}
		} else if (!score.equals(other.score)) {
			return false;
		}
		if (Double.doubleToLongBits(score_limit) != Double.doubleToLongBits(other.score_limit)) {
			return false;
		}
		return true;
	}
}
