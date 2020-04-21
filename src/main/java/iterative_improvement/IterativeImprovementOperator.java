package iterative_improvement;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import main.Config;
import main.NextGenConfig;

public abstract class IterativeImprovementOperator {
	public abstract List<String> operate(List<String> fixTheseRules, Config config, NextGenConfig currentNGConfig, String dateFolder, DateTimeFormatter dtf, LocalDateTime now);
}
