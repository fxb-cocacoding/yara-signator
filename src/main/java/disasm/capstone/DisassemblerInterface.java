package disasm.capstone;

import java.util.List;

public interface DisassemblerInterface {

	/*
	 * Every Disassembler has to fulfill these requirements.
	 */
	
	public void createHandle(String ip, int port) throws Exception;
	public void closeHandle() throws Exception;
	public List<String> getDisassembly(int architecture, int bitness, byte[] opcodes, String host, int port) throws Exception;
	
}
