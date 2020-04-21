package iterative_improvement;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import main.Config;
import postgres.PostgresRequestUtils;
import statistics.yara_results.YaraBashExecutor;

public class IterativeImprovementGenerator {
	
	private Config config;
	private String cmd_yarac;
	private String cmd_yara;
	private HashMap<String, Set<String>> yaraOutput;
	private List<String> familiesInDatabase;
	private HashMap<String, List<String>> samplesForfamilies;
		
	public IterativeImprovementGenerator(Config config, String cmd_yarac, String cmd_yara) {
		//this.dtf = dtf;
		//this.now = now;
		this.config = config;
		this.cmd_yarac = cmd_yarac;
		this.cmd_yara = cmd_yara;
	}
	
	public void init() throws SQLException {
		this.yaraOutput = new YaraBashExecutor().runYara(config, cmd_yarac, cmd_yara);
		this.familiesInDatabase = new PostgresRequestUtils().getFamilies();
		this.samplesForfamilies = new PostgresRequestUtils().getSamplesForFamilies();
	}
	
}
