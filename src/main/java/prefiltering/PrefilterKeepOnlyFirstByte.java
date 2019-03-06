package prefiltering;

import java.util.List;

import converters.ngrams.Ngram;
import smtx_handler.Instruction;

public class PrefilterKeepOnlyFirstByte extends Prefilter {

	@Override
	public Ngram calc(Ngram ngram) {
		Ngram ret = ngram;
		List<Instruction> ins = ret.getNgramInstructions();
		for(int i=0; i<ins.size(); i++) {
			String opcode = ins.get(i).getOpcodes();
			int opcode_size = opcode.length();
			int start_position = 2;
			StringBuilder sb = new StringBuilder();
			sb.append(opcode.charAt(0)).append(opcode.charAt(1));
			for(int j=start_position; j<opcode_size; j++) {
				sb.append('?');
			}
			ins.get(i).setOpcodes(sb.toString());
		}
		return ngram;
	}

}
