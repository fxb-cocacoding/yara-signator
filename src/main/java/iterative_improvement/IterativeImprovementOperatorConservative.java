package iterative_improvement;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import main.Config;
import main.NextGenConfig;
import postgres.PostgresRequestUtils;

public class IterativeImprovementOperatorConservative extends IterativeImprovementOperator {

	private int i;
	private int j;
	
	public IterativeImprovementOperatorConservative(int i, int j) {
		this.i = i;
		this.j = j;
	}
	
	@Override
	public List<String> operate(List<String> fixTheseRules, Config config, NextGenConfig currentNGConfig, String dateFolder, DateTimeFormatter dtf, LocalDateTime now) {
		List<String> rulesToFix = new ArrayList<String>();
		
		new PostgresRequestUtils().generateYaraRule(dtf, now, config);
		
		return rulesToFix;
	}

}
