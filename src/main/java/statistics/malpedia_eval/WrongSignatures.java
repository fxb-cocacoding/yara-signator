package statistics.malpedia_eval;

import java.util.List;

public class WrongSignatures {
	public String signatureName;
	public List<WrongSignature> wrongsignature;
	
	public String toString() {
		return "\n\nsignature for family " + signatureName + " is wrong:\n"
				+ wrongsignature.toString();
	}
}
