package utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FileFinder {
	public List<String> getMalpediaFiles(String malpediaDirectory) throws IOException {
		ArrayList<Path> allPaths = new ArrayList<Path>();
		Files.walk(Paths.get(malpediaDirectory)).filter(Files::isRegularFile).forEachOrdered(allPaths::add);
		List<String> allFiles = new ArrayList<>();
		for(Path p: allPaths) {
			String s = p.toString();
			/*
			 * We ignore files in malpedia that are no samples.
			 * We hope there are only the following files we want to exclude.
			 */
			if(s.endsWith(".json")) continue;
			if(s.endsWith(".txt")) continue;
			if(s.endsWith(".yar")) continue;
			if(s.endsWith(".yara")) continue;
			//if(s.contains("win.")) continue;
			allFiles.add(s);
		}
		return allFiles;
	}
	
	public List<String> getFiles(String malpediaDirectory) throws IOException {
		ArrayList<Path> allPaths = new ArrayList<Path>();
		Files.walk(Paths.get(malpediaDirectory)).filter(Files::isRegularFile).forEachOrdered(allPaths::add);
		List<String> allFiles = new ArrayList<>();
		for(Path p: allPaths) {
			allFiles.add(p.toString());
		}
		return allFiles;
	}
}
