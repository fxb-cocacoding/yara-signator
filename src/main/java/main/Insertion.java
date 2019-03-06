package main;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import converters.ConverterFactory;
import converters.ngrams.Ngram;
import json.Generator;
import mongodb.MongoConnection;
import mongodb.MongoDataAdapter;
import mongodb.MongoHandler;
import smtx_handler.Instruction;
import smtx_handler.SMDA;

@Deprecated
public class Insertion implements Runnable {
	
	Insertion(Config config, File[] allSmdaFiles, long minInstructions, boolean firstInsertion,
			boolean atLeastOneElementInNgramCollection, int i) {
		this.config=config;
		this.i=i;
		this.firstInsertion = firstInsertion;
		this.allSmdaFiles = allSmdaFiles;
		this.atLeastOneElementInNgramCollection = atLeastOneElementInNgramCollection;
		this.minInstructions = minInstructions;
	}
	
	private Config config;
	private File[] allSmdaFiles;
	private long minInstructions;
	private boolean firstInsertion;
	private boolean atLeastOneElementInNgramCollection;
	private int i;
	
	@Deprecated
	private static void insertOneSmdaElement(Config config, File[] allSmdaFiles, long minInstructions, boolean firstInsertion,
			boolean atLeastOneElementInNgramCollection, int i) throws IllegalStateException {
		SMDA smda = new Generator().generateSMDA(allSmdaFiles[i].getAbsolutePath());	
		smda.setFamily(smda.getMeta().getFamily());
		
		/*
		 * Step 0
		 * Sanitize the input:
		 */
		if(smda == null || smda.getFilename() == null || smda.getFilename().isEmpty()) {
			System.out.println("null pointer in smda creation, no valid file");
		} else if(smda.getSummary() == null) {
			System.out.println("CONTINUE: NO SUMMARY DETECTED in " + smda.getFamily() + " - " + smda.getFilename() );
			return;
		} else if(smda.getXcfg() == null) {
			System.out.println("CONTINUE: NO CFG DETECTED in " + smda.getFamily() + " - " + smda.getFilename() );
			return;
		} else if(smda.getXcfg().getFunctions() == null) {
			System.out.println("CONTINUE: NO FUNCTIONS DETECTED in " + smda.getFamily() + " - " + smda.getFilename() );
			return;
		} else if(smda.getSummary().getNum_instructions() < minInstructions) {
			System.out.println("CONTINUE: NOT ENOUGH INSTRUCTIONS FOUND in " + smda.getFamily() + " - " + smda.getFilename() + " - " + smda.getSummary().getNum_instructions() + "/" + minInstructions);
			return;
		}
		
		/*
		 * Step 2
		 * Get the linearized disassembly:
		 */
		List<List<Instruction>> linearized = new ConverterFactory().getLinearized(smda);
		
		
		/*
		 * Step 3
		 * Build the n-grams:
		 */
		ArrayList<Integer> allN = config.getNs();
		List<Ngram> ngrams = null;
		
		//System.out.println("start n");
		for(int n : allN) {
			//System.out.println("current n: " + n + "  size: " + config.getNs().size() + "  -  n to string: " + config.getNs().toString());
			
			
			/*
			 * if we have already ngrams of this kind in the database, continue.
			 * else, skip to save time.
			 */
			
			
			boolean familyCovered = false;
			boolean sampleCovered = false;
			boolean ngramsDetected = false;
			

			/*
			 * Make generateFamilyEntity etc protected and store these calls into mongoHandler, let smda given as argument
			 */
			familyCovered = new MongoHandler().isFamilyAlreadyInDatabase(MongoDataAdapter.INSTANCE.generateFamilyEntity(smda));
			//System.out.println("family covered: " + familyCovered);
			
			sampleCovered = new MongoHandler().isSampleAlreadyInDatabase(MongoDataAdapter.INSTANCE.generateSampleEntity(smda));
			//System.out.println("sample covered: " + sampleCovered);
			
			if(firstInsertion == false) {
				ngramsDetected = new MongoHandler().areNgramsAlreadyInDatabase(MongoDataAdapter.INSTANCE.generateNgramEntity(smda, n, null));
			}
			//System.out.println("ngrams covered: " + ngramsDetected);
			
			
			
			if(ngramsDetected == false || firstInsertion == true) {
				ngrams = new ConverterFactory().calculateNgrams("createWithoutOverlappingCodeCaves", linearized, n);
				if(ngrams.isEmpty()) {
					System.out.println("no ngrams detected, got null in " + smda.getFilename());
					continue;
				} else {
					new MongoHandler().writeNgramsToDatabase(smda, ngrams, n, config.mongoQueryBufferSize);
					if(familyCovered == false) {
						new MongoHandler().writeFamilyToDatabase(smda);
					}
					
					if(sampleCovered == false) {
						new MongoHandler().writeSampleToDatabase(smda);
					}
				}
			} else {
				throw new IllegalStateException("This section should never be reached.");
				//System.out.println("NEVER REACHED");
				/*
				 * 
				 * 
				 * TODO!!!!
				 * 
				 * 
				 */
				//change this to get the resources from database
				//ngrams = new ConverterFactory().calculateNgrams("createWithoutOverlappingCodeCaves", linearized, n);
			}
		}
		//System.out.println("finish n");
		
		System.out.println("[INSERTION_STEP] Progress: " + (int)(( (float) (i + 1) / allSmdaFiles.length)*100.0) + "% - " + "Step: " + (i + 1) + "/" + allSmdaFiles.length +
				" - Sample: " + smda.getFamily() +
				" " + smda.getFilename() + " " + smda.getArchitecture() + " " + smda.getBitness());

		
	}
	
	@Override
	public void run() {
		try {
			insertOneSmdaElement(config, allSmdaFiles, minInstructions, firstInsertion, atLeastOneElementInNgramCollection, i);
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("We have an illegal state exception in the file: " + allSmdaFiles[i].getAbsolutePath());
			System.out.println("Further information could not be retrieved because the file was unparseable for us.");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("We have an illegal state exception in the file: " + allSmdaFiles[i].getAbsolutePath());
			System.out.println("Further information could not be retrieved because the file was unparseable for us.");
		}
	}
	



}
