package prefiltering;

public class PrefilterFactory {

	public Prefilter getPrefilter(String function) {
		if(function.equalsIgnoreCase("dummy")) {
			return new PrefilterDummy();
		} else if(function.equalsIgnoreCase("onlyfirstbyte")) {
			return new PrefilterKeepOnlyFirstByte();
		} else if(function.equalsIgnoreCase("callsandjumps")) {
			return new PrefilterMaskCallsAndJumps();
		} else if(function.equalsIgnoreCase("intrajumps")) {
			return new WildcardIntraJumps();
		} else if(function.equalsIgnoreCase("interjumps")) {
			return new WildcardInterJumps();
		} else if(function.equalsIgnoreCase("datarefs")) {
			return new WildcardDataRefs();
		} else if(function.equalsIgnoreCase("binvalue")) {
			return new WildcardBinaryValue();
		} else {
			throw new UnsupportedOperationException();
		}
	}

}