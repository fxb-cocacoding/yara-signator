package main;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;

public class Utils {
	private static final Logger logger = LoggerFactory.getLogger(Utils.class);
	
	Config getConfig(String configFileName) {
		Reader configReader = null;
		Config config = null;
		try {
			configReader = new FileReader(configFileName);
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			config = gson.fromJson(configReader, Config.class);
			logger.info(gson.toJson(config));
			configReader.close();
		} catch (JsonIOException | IOException e) {
			e.printStackTrace();
			logger.error("Your config file could not be processed, is it stored at " + System.getProperty("user.home") + "/.yarasignator.conf ?");
		} finally {
			try {
				configReader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return config;
    }

}
