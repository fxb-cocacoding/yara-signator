package prefiltering;

import java.util.List;

import converters.ngrams.Ngram;
import smtx_handler.Instruction;

public class PrefilterMaskCallsAndJumps extends Prefilter {

	@Override
	public Ngram calc(Ngram ngram) {
		Ngram ret = ngram;
		List<Instruction> ins = ret.getNgramInstructions();
		for(int i=0; i<ins.size(); i++) {
			String opcode = ins.get(i).getOpcodes();
			int opcode_size = opcode.length();
			StringBuilder sb = new StringBuilder();
			sb.append(opcode.charAt(0)).append(opcode.charAt(1));
			
			String cmp = sb.toString().toUpperCase();
			
			switch(cmp) {
			
				case "E8":
				case "E9":
				case "EA":
				case "EB":
					for(int j=2; j<opcode_size; j++) {
						sb.append('?');
					}
					break;
			}
			
			if(opcode.length() > 4 && (opcode.charAt(3) != '?' || opcode.charAt(4) != '?')) {
				sb.append(opcode.charAt(3)).append(opcode.charAt(4));
				
				cmp = sb.toString().toUpperCase();
				switch(cmp) {
					case "FF15":
					case "FF25":
					case "FF35":
						for(int j=4; j<opcode_size; j++) {
							sb.append('?');
						}
						System.out.println("ALARM");
						break;
				}
			}
			
			
			//cleanup:
			if(sb.toString().length()<opcode_size) {
				for(int j=sb.toString().length(); j<opcode_size; j++) {
					sb.append(opcode.charAt(j));
				}
			}
			
			ins.get(i).setOpcodes(sb.toString());
		}
		return ngram;
	}

}
