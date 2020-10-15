package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;

import converters.ngrams.Ngram;
import utils.MalpediaVersion;

public class Utils {
	private static final Logger logger = LoggerFactory.getLogger(Utils.class);
	
	public Config getConfig(String configFileName) {
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
			logger.error("Your config file could not be processed, is it at the right location? (" + configFileName + ")");
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

	public MalpediaVersion getVersioning(String versioningFile) throws IOException {
		MalpediaVersion mv = new MalpediaVersion();
		File f = new File(versioningFile);
		Path p = f.toPath();
		try (Stream<String> lines = Files.lines(p))
		{
		    for (String line : (Iterable<String>) lines::iterator)
		    {
		        if(line.contains("MALPEDIA_DATE=")) {
		        	mv.date = line.substring( line.indexOf("MALPEDIA_DATE=") + "MALPEDIA_DATE=".length() );
		        } else if (line.contains("MALPEDIA_COMMIT=")) {
		        	mv.commit = line.substring(line.indexOf("MALPEDIA_COMMIT=") + "MALPEDIA_COMMIT=".length() );
		        }
		    }
		}
		return mv;
	}
	
}
