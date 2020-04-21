package statistics.yara_results;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map.Entry;

import main.Config;

public class YaraBashExecutor {
	public synchronized HashMap<String, Set<String>> runYara(Config config, String cmd_yarac, String cmd_yara) {
		StringBuffer output = new StringBuffer();
		Process p;
		int returnCode = 0;
		
		HashMap<String, Set<String>> hm = new HashMap<>();

		try {
			//p = Runtime.getRuntime().exec(cmd_yarac);
			p = new ProcessBuilder("/bin/bash", "-c", cmd_yarac).redirectErrorStream(true).start();
			
			try {
				p.waitFor();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
		} catch(IOException e) {
			System.out.println(e.toString());
		}
		
		System.out.println("/bin/bash -c " + cmd_yarac);
		
		try {
			p = new ProcessBuilder("/bin/bash", "-c", cmd_yara).start();
			
			try {
				//p.waitFor();
				//returnCode = p.exitValue();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			System.out.println("/bin/bash -c " + cmd_yara);
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = "";
			while ((line = reader.readLine())!= null) {
				// too verbose
				//System.out.println(line);
				String[] parts = line.split(" ");
				
				if(hm.containsKey(parts[0])) {					
					Set<String> s  = new HashSet<>();
					s.addAll(hm.get(parts[0]));
					s.add(parts[1]);
					hm.put(parts[0], s);
				} else {
					Set<String> s = new HashSet<>();
					s.add(parts[1]);
					hm.put(parts[0], s);
				}
				output.append(line + "\n");
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		HashMap<String, Set<String>> ret = new HashMap<>();
		
		for(Entry<String, Set<String>> e : hm.entrySet()) {
			ret.put(e.getKey().replace("_auto", ""), e.getValue());
		}

		/*
		for(Entry<String, Set<String>> e : ret.entrySet()) {
			for(String i: e.getValue()) {
				System.out.println(e.getKey() + " " + i);
			}
		}
		 */
		
		return ret;
	}

}
