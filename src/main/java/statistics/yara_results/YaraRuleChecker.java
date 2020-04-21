package statistics.yara_results;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import main.Config;
import postgres.PostgresRequestUtils;
import utils.FileFinder;

public class YaraRuleChecker {
	
	//DateTimeFormatter dtf;
	//LocalDateTime now;
	Config config;
	String cmd_yarac;
	String cmd_yara;
	HashMap<String, Set<String>> yaraOutput;
	List<String> familiesInDatabase;
	
	
	public YaraRuleChecker(Config config, String cmd_yarac, String cmd_yara) {
		//this.dtf = dtf;
		//this.now = now;
		this.config = config;
		this.cmd_yarac = cmd_yarac;
		this.cmd_yara = cmd_yara;
	}
	
	public void init() throws SQLException {
		this.yaraOutput = new YaraBashExecutor().runYara(config, cmd_yarac, cmd_yara);
		this.familiesInDatabase = new PostgresRequestUtils().getFamilies();
	}
	
	public void init(List<String> families) throws SQLException {
		this.yaraOutput = new YaraBashExecutor().runYara(config, cmd_yarac, cmd_yara);
		this.familiesInDatabase = families;
	}
	
	public String verifyOutputResults() throws SQLException {
		TreeMap<String, TreeSet<String>> yaraStatBase = new TreeMap<String, TreeSet<String>>();
		for(Entry<String, Set<String>> entry: yaraOutput.entrySet()) {
			String rule = entry.getKey();
			Set<String> samples = entry.getValue();
			for(String sample: samples) {
				String tmp = sample.substring(config.malpedia_path.length() + 1);
				//String family = tmp.substring(0, tmp.indexOf("/")).replace('.', '_');
				String family = tmp.substring(0, tmp.indexOf("/"));
				String filename = tmp.substring(tmp.lastIndexOf("/") + 1);
				if(yaraStatBase.containsKey(filename)) {
					TreeSet<String> s = yaraStatBase.get(filename);
					//System.out.println("rule added: " + rule + " to Set: " + s.toString());
					s.add(rule);
					yaraStatBase.put(filename, s);
				} else {
					TreeSet<String> s = new TreeSet<>();
					//System.out.println("new sample, rule added: " + rule + " to Set: " + s.toString());
					s.add(rule);
					yaraStatBase.put(filename, s);
				}
			}
		}

		List<String> malpediaFiles = null;
		try {
			malpediaFiles = new FileFinder().getMalpediaFiles(config.malpedia_path);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new YaraEvaluation().samplesFromMalpediaTest(malpediaFiles, config, yaraStatBase);
	}
	
}
