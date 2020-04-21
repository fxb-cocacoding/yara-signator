package statistics.malpedia_eval;


import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class MalpediaEvalDeserializer implements JsonDeserializer<MalpediaEval> {

	@Override
	public MalpediaEval deserialize(JsonElement json, Type arg1, JsonDeserializationContext arg2) throws JsonParseException {
		MalpediaEval malpediaEval = new MalpediaEval();
		
		JsonObject jsonObject = json.getAsJsonObject();
		JsonObject elem = jsonObject.getAsJsonObject();
        Set<Entry<String, JsonElement>> objects =  elem.entrySet();

        // Head entry point of the document
        for (Entry<String, JsonElement> entry : objects) {
        	
            JsonElement jsonElement  = entry.getValue();

            
            if(entry.getKey().equalsIgnoreCase("false_negatives")) {
            	FalseNegatives fns = new FalseNegatives();
            	fns.content = new ArrayList<FalseNegative>();
            
            	for (Entry<String, JsonElement> fn_entry : jsonElement.getAsJsonObject().entrySet()) {
                    String family = fn_entry.getKey();
                    FalseNegative fn = new FalseNegative();
                    fn.familyName = family;
                    fn.sampleNames = new ArrayList<String>();
                    JsonArray l = fn_entry.getValue().getAsJsonArray();

                    for(JsonElement i : l) {
                    	fn.sampleNames.add(i.getAsString());
                    }
                    
                    fns.content.add(fn);
            	}
            	malpediaEval.fns = fns;
            }
            
            
            
            
            if(entry.getKey().equalsIgnoreCase("false_positives")) {
            	FalsePositives fps = new FalsePositives();
            	fps.wrongSignatures = new ArrayList<WrongSignatures>();
            	
            	for (Entry<String, JsonElement> fp_entry : jsonElement.getAsJsonObject().entrySet()) {
            		WrongSignatures ws = new WrongSignatures();
            		String false_rule = fp_entry.getKey();
                    ws.signatureName = false_rule;
                    ws.wrongsignature = new ArrayList<WrongSignature>();

                    for(Entry<String, JsonElement> i: fp_entry.getValue().getAsJsonObject().entrySet()) {
                    	WrongSignature wrongSignature = new WrongSignature();
                    	String detectsTheseFamiliesWrong = i.getKey().toString();
                    	wrongSignature.wrongDetectedFamilyName = detectsTheseFamiliesWrong;
                    	wrongSignature.wrongSamples = new ArrayList<WrongCoveredSample>();
                    	for(Entry<String, JsonElement> j : i.getValue().getAsJsonObject().entrySet()) {
                    		
                    		String wrongCoveredSample = j.getKey();
                    		WrongCoveredSample w = new WrongCoveredSample();
                    		w.samples_name = wrongCoveredSample;
                    		w.sequences = new ArrayList<>();
                    		
                    		for (JsonElement k : j.getValue().getAsJsonArray()) {
                    			Sequence s = new Sequence();
                    			
                    			if(k.getAsJsonArray().size() == 3) {
	                    			int counter = 1;
                    				for(JsonElement l : k.getAsJsonArray()) {
                    					if(counter == 1) s.name = (l.getAsString());
                    					if(counter == 2) s.address = (l.getAsString());
                    					if(counter == 3) s.pattern = (l.getAsString());
                    					counter++;
	                    			}
                    			}
                    			w.sequences.add(s);
                    		}
                    		wrongSignature.wrongSamples.add(w);
                    	}
                    	ws.wrongsignature.add(wrongSignature);
                    }
                    fps.wrongSignatures.add(ws);
            	}
            	
            	malpediaEval.fps = fps;
            }
            
            if(entry.getKey().equalsIgnoreCase("fp_sequence_stats")) {
            	malpediaEval.fp_sequence_stats = new FpSequenceStats();
            	malpediaEval.fp_sequence_stats.content = new HashMap<String, Integer>();
            	for(Entry<String, JsonElement> i: jsonElement.getAsJsonObject().entrySet()) {
            		malpediaEval.fp_sequence_stats.content.put(i.getKey(), i.getValue().getAsInt());
            	}
            }

			if(entry.getKey().equalsIgnoreCase("statistics")) {
				malpediaEval.statistics = new Statistics();
				Set<Entry<String, JsonElement>> tmp = jsonElement.getAsJsonObject().entrySet();
				for(Entry<String, JsonElement> i: tmp) {
					if(i.getKey().equalsIgnoreCase("clean_rules")) malpediaEval.statistics.clean_rules = i.getValue().getAsString();
					if(i.getKey().equalsIgnoreCase("f1_precision")) malpediaEval.statistics.f1_precision = i.getValue().getAsString();
					if(i.getKey().equalsIgnoreCase("f1_recall")) malpediaEval.statistics.f1_recall = i.getValue().getAsString();
					if(i.getKey().equalsIgnoreCase("f1_score")) malpediaEval.statistics.f1_score = i.getValue().getAsString();
					if(i.getKey().equalsIgnoreCase("false_negatives")) malpediaEval.statistics.false_negatives = i.getValue().getAsString();
					if(i.getKey().equalsIgnoreCase("false_positives")) malpediaEval.statistics.false_positives = i.getValue().getAsString();
					if(i.getKey().equalsIgnoreCase("families")) malpediaEval.statistics.families = i.getValue().getAsString();
					if(i.getKey().equalsIgnoreCase("families_covered")) malpediaEval.statistics.families_covered = i.getValue().getAsString();
					if(i.getKey().equalsIgnoreCase("rules_without_fn")) malpediaEval.statistics.rules_without_fn = i.getValue().getAsString();
					if(i.getKey().equalsIgnoreCase("rules_without_fp")) malpediaEval.statistics.rules_without_fp = i.getValue().getAsString();
					if(i.getKey().equalsIgnoreCase("samples_all")) malpediaEval.statistics.samples_all = i.getValue().getAsString();
					if(i.getKey().equalsIgnoreCase("samples_detectable")) malpediaEval.statistics.samples_detectable = i.getValue().getAsString();
					if(i.getKey().equalsIgnoreCase("true_negatives")) malpediaEval.statistics.true_negatives = i.getValue().getAsString();
					if(i.getKey().equalsIgnoreCase("true_positives")) malpediaEval.statistics.true_positives = i.getValue().getAsString();
					if(i.getKey().equalsIgnoreCase("true_positives_bonus")) malpediaEval.statistics.true_positives_bonus = i.getValue().getAsString();
				}
			}
        }
           
		return malpediaEval;
	}

}
