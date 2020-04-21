package utils;

public class HexTools {
	public static String swapHexBytes(String input) {
		String output = "";
		int i = input.length()-2;
		
		if(input.length()%2!=0) {
			//System.out.println("This shouldn't have happened.");
			//System.out.println("this was to swap: " + input);
			return "";
		}
		
		while(i>=0) {
			output = output + input.charAt(i) + input.charAt(i+1);
			i = i - 2;
		}
		return output;
	}
}
