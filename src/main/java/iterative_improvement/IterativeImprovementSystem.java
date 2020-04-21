package iterative_improvement;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import main.Config;
import main.NextGenConfig;

public class IterativeImprovementSystem {

	private IterativeImprovementFactory nextGenFactory = new IterativeImprovementFactory();
	
	public List<String> runner(String function, List<String> fixTheseRules, Config config, NextGenConfig currentNGConfig, String dateFolder, DateTimeFormatter dtf, LocalDateTime now) throws Exception {
		IterativeImprovementOperator ngOperator = nextGenFactory.getOperator(function);
		List<String> ret = ngOperator.operate(fixTheseRules, config, currentNGConfig, dateFolder, dtf, now);
		
		return ret;
	}

	
}
