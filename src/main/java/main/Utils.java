package main;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;

import converters.ngrams.Ngram;

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

	public List<Ngram> removeDuplicatesLosingOrder(List<Ngram> input) {
		HashSet<Ngram> tmp = new HashSet<>();
		tmp.addAll(input);
		List<Ngram> ret = new ArrayList<Ngram>();
		ret.addAll(tmp);
		return ret;
	}
	
	public void removeStuffFromListUntilBarrier(List<Ngram> input, int barrier) {
		if(input.size() >= barrier) {
			input.subList(barrier, input.size()).clear();
		}
	}
	
}
