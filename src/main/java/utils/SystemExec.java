package utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

public class SystemExec {
	public synchronized void executeCommand(String[] cmd) throws IOException {
		System.out.println("executeCommand called");
		Process p = null;
		try {
			//p = Runtime.getRuntime().exec(cmd_yarac);
			p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
			
			try {
				p.waitFor();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
		} catch(IOException e) {
			System.out.println(e.toString());
		}
		
		System.out.println(Arrays.toString(cmd));
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line = "";
		while ((line = reader.readLine())!= null) {
			// too verbose
			System.out.println(line);
		}

	}
	
}
