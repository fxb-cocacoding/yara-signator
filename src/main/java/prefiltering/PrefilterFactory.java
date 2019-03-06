package prefiltering;

public class PrefilterFactory {

	public Prefilter getPrefilter(String function) {
		if(function.equalsIgnoreCase("dummy")) {
			return new PrefilterDummy();
		} else if(function.equalsIgnoreCase("onlyfirstbyte")) {
			return new PrefilterKeepOnlyFirstByte();
		} else if(function.equalsIgnoreCase("maskCallsAndJumps")) {
			return new PrefilterMaskCallsAndJumps();
		} else {
			throw new UnsupportedOperationException();
		}
	}

}