package tests.setup;

import java.io.FileNotFoundException;

import main.Config;
import main.Utils;

public class ConfigReader {
	public static Config readConfig() throws FileNotFoundException {
		final String configFileName = System.getProperty("user.home") + "/.yarasignator.conf";
		return new Utils().getConfig(configFileName);
	}

}
