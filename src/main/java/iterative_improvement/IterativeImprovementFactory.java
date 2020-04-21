package iterative_improvement;

public class IterativeImprovementFactory {

	public IterativeImprovementOperator getOperator(String function) throws Exception {
		if(function.equalsIgnoreCase("nextgenDummy")) {
			return new IterativeImprovementOperatorDummy();
		} else if(function.equalsIgnoreCase("clusteringInsideFamily")) {
			return new IterativeImprovementOperatorClusteringInsideFamily();
		} else if(function.equalsIgnoreCase("CandidateOne")) {
			return new IterativeImprovementOperatorCandidateOne(7);
		} else if(function.equalsIgnoreCase("ParseMalpediaEval")) {
			return new IterativeImprovementOperatorParseMalpediaEval(7);
		} else if(function.equalsIgnoreCase("7_of_7")) {
			return new IterativeImprovementOperatorConservative(7,7);
		} else {
			throw new UnsupportedOperationException();
		}
	}
	
}
