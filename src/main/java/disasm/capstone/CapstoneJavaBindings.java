package disasm.capstone;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/*
 * To use the native JAVA JNA Bindings for capstone, uncomment this stuff and add the dependencies in pom.xml
 * They are already there, but outcommented.
 */


@Deprecated
public class CapstoneJavaBindings implements DisassemblerInterface {
	
	private static final Logger logger = LoggerFactory.getLogger(CapstoneJavaBindings.class);
	
//	Capstone cs32 = null;
//	Capstone cs64 = null;
//	
//	public CapstoneJavaBindings() {
//		cs32 = new Capstone(Capstone.CS_ARCH_X86, Capstone.CS_MODE_32);
//		cs64 = new Capstone(Capstone.CS_ARCH_X86, Capstone.CS_MODE_64);
//	}
//	
//	public List<String> getMnemonics32(String opcode, int offset) throws DecoderException {
//		byte[] code = Hex.decodeHex(opcode);
//		CsInsn[] csinsn = cs32.disasm(code, offset);
//		List<String> ret = new ArrayList<>(4);
//		
//		for(int i=0; i< csinsn.length; i++) {
//			ret.add(csinsn[i].mnemonic);
//			ret.add(csinsn[i].opStr);
//			
////			if(i==1) {
////				logger.debug("step: " + (i-1) + " opcode: " + opcode + " mnemonic: " + csinsn[i-1].mnemonic + " opStr: " + csinsn[i-1].opStr);
////				logger.debug("step: " + i + " opcode: " + opcode + " mnemonic: " + csinsn[i].mnemonic + " opStr: " + csinsn[i].opStr);
////			}
////			
////			
////			if(i>1) {
////				logger.error("SHOULD NEVER BE REACHED");
////			}
//		}
//		return ret;
//	}
//	
//	public List<String> getMnemonics64(String opcode, int offset) throws DecoderException {
//		byte[] code = Hex.decodeHex(opcode);
//		CsInsn[] csinsn = cs64.disasm(code, offset);
//		List<String> ret = new ArrayList<>(1);
//		
//		for(int i=0; i< csinsn.length; i++) {
//			ret.add(csinsn[i].mnemonic);
//			ret.add(csinsn[i].opStr);
//		}
//		return ret;
//	}
//
	@Override
	public List<String> getDisassembly(int architecture, int bitness, byte[] opcodes, String host, int port) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void createHandle() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void closeHandle() {
		// TODO Auto-generated method stub
		
	}
}
