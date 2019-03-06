package mongodb;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import javax.naming.OperationNotSupportedException;
import javax.xml.bind.DataBindingException;

import org.bson.BSONObject;
import org.bson.BsonArray;
import org.bson.Document;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.AggregationOptions;
import com.mongodb.AggregationOptions.Builder;
import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.Cursor;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UnwindOptions;
import com.mongodb.operation.AggregateOperation;
import com.mongodb.util.JSON;

import converters.ConverterFactory;
import converters.ngrams.Ngram;
import db.entites.FamiliesEntity;
import db.entites.NgramEntity;
import db.entites.NgramUniqueEntity;
import db.entites.SampleEntity;
import db.entites.UniqueSamplesAggregationEntity;
import json.Generator;
import main.Config;
import main.Main;
import smtx_handler.Instruction;
import smtx_handler.SMDA;
import statistics.ngram_results.NgramResults;

import static com.mongodb.client.model.Aggregates.*;

@Deprecated
public class MongoHandler {

	private static final Logger logger = LoggerFactory.getLogger(MongoHandler.class);
	
	@Deprecated
	private static synchronized void insertOneSmdaElement(Config config, File[] allSmdaFiles, long minInstructions, boolean firstInsertion,
			boolean atLeastOneElementInNgramCollection, int i) throws IllegalStateException {
		SMDA smda = new Generator().generateSMDA(allSmdaFiles[i].getAbsolutePath());	
		smda.setFamily(smda.getMeta().getFamily());
		
		/*
		 * Step 0
		 * Sanitize the input:
		 */
		if(smda == null || smda.getFilename() == null || smda.getFilename().isEmpty()) {
			logger.info("null pointer in smda creation, no valid file");
		} else if(smda.getSummary() == null) {
			logger.info("CONTINUE: NO SUMMARY DETECTED in " + smda.getFamily() + " - " + smda.getFilename() );
			return;
		} else if(smda.getXcfg() == null) {
			logger.info("CONTINUE: NO CFG DETECTED in " + smda.getFamily() + " - " + smda.getFilename() );
			return;
		} else if(smda.getXcfg().getFunctions() == null) {
			logger.info("CONTINUE: NO FUNCTIONS DETECTED in " + smda.getFamily() + " - " + smda.getFilename() );
			return;
		} else if(smda.getSummary().getNum_instructions() < minInstructions) {
			logger.info("CONTINUE: NOT ENOUGH INSTRUCTIONS FOUND in " + smda.getFamily() + " - " + smda.getFilename() + " - " + smda.getSummary().getNum_instructions() + "/" + minInstructions);
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
		
		//logger.info("start n");
		for(int n : allN) {
			//logger.info("current n: " + n + "  size: " + config.getNs().size() + "  -  n to string: " + config.getNs().toString());
			
			
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
			//logger.info("family covered: " + familyCovered);
			
			sampleCovered = new MongoHandler().isSampleAlreadyInDatabase(MongoDataAdapter.INSTANCE.generateSampleEntity(smda));
			//logger.info("sample covered: " + sampleCovered);
			
			if(firstInsertion == false) {
				ngramsDetected = new MongoHandler().areNgramsAlreadyInDatabase(MongoDataAdapter.INSTANCE.generateNgramEntity(smda, n, null));
			}
			//logger.info("ngrams covered: " + ngramsDetected);
			
			
			
			if(ngramsDetected == false || firstInsertion == true) {
				ngrams = new ConverterFactory().calculateNgrams("createWithoutOverlappingCodeCaves", linearized, n);
				if(ngrams.isEmpty()) {
					logger.info("no ngrams detected, got null in " + smda.getFilename());
					continue;
				} else {
					new MongoHandler().writeNgramsToDatabase(smda, ngrams, n, config.mongoQueryBufferSize);
					if(familyCovered == false) {
						new MongoHandler().writeFamilyToDatabase(smda);
					}
					
					if(sampleCovered == false) {
						new MongoHandler().writeSampleToDatabase(smda);
					}
					if(atLeastOneElementInNgramCollection == false && firstInsertion == false) {
						buildIndexesForCachingDB();
						atLeastOneElementInNgramCollection = true;
					}
				}
			} else {
				throw new IllegalStateException("This section should never be reached.");
				//logger.error("NEVER REACHED");
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
		//logger.info("finish n");
		
		logger.info("Progress: " + (int)(( (float) (i + 1) / allSmdaFiles.length)*100.0) + "% - " + "Step: " + (i + 1) + "/" + allSmdaFiles.length +
				" - Sample: " + smda.getFamily() +
				" " + smda.getFilename() + " " + smda.getArchitecture() + " " + smda.getBitness());

		
	}
	
	@Deprecated	
	private static synchronized void buildIndexesForCachingDB() {
		logger.info("Building now indexes for caching_db!");
		logger.info("For a proper view of the progress, watching mongodb.log is recommended:");
		logger.info("tail -F /var/log/mongodb/mongodb.log");
		DBObject indexOptions = new BasicDBObject();
		
		/* already build, uncomment me
		indexOptions.put("n", 1);
		indexOptions.put("hash", 1);
		indexOptions.put("filename", 1);
		MongoConnection.INSTANCE.mongoClient.getDB("caching_db").getCollection("ngrams").createIndex(indexOptions);
		*/
		
		/*
		 * index for aggragation query which builds the unique_samples db, this one is very important as it saves multiple hours!		
		 */
		indexOptions = new BasicDBObject();
		indexOptions.put("family", 1);
		indexOptions.put("filename", 1);
		indexOptions.put("hash", 1);
		indexOptions.put("concat", 1);
		indexOptions.put("n", 1);
		MongoConnection.INSTANCE.mongoClient.getDB("caching_db").getCollection("ngrams").createIndex(indexOptions);

		/*
		indexOptions = new BasicDBObject();
		indexOptions.put("family", 1);
		indexOptions.put("filename", 1);
		indexOptions.put("hash", 1);
		MongoConnection.INSTANCE.mongoClient.getDB("caching_db").getCollection("ngrams").createIndex(indexOptions);
		 */
		
		/*
		//
		// Maybe enable the three later?
		//
		indexOptions = new BasicDBObject();		
		indexOptions.put("n", 1);
		indexOptions.put("concat", 1);
		MongoConnection.INSTANCE.mongoClient.getDB("caching_db").getCollection("ngrams").createIndex(indexOptions);

		indexOptions = new BasicDBObject();		
		indexOptions.put("concat", 1);
		indexOptions.put("n", 1);
		MongoConnection.INSTANCE.mongoClient.getDB("caching_db").getCollection("ngrams").createIndex(indexOptions);

		indexOptions = new BasicDBObject();
		indexOptions.put("family", 1);
		indexOptions.put("n", 1);
		indexOptions.put("concat", 1);
		MongoConnection.INSTANCE.mongoClient.getDB("caching_db").getCollection("ngrams").createIndex(indexOptions);
		
		indexOptions = new BasicDBObject();
		indexOptions.put("concat", 1);
		indexOptions.put("family", 1);
		indexOptions.put("n", 1);
		MongoConnection.INSTANCE.mongoClient.getDB("caching_db").getCollection("ngrams").createIndex(indexOptions);
		 */


		indexOptions = new BasicDBObject();
		indexOptions.put("filename", 1);
		indexOptions.put("hash", 1);
		MongoConnection.INSTANCE.mongoClient.getDB("caching_db").getCollection("samples").createIndex(indexOptions);
		
		indexOptions = new BasicDBObject();
		indexOptions.put("family", 1);
		MongoConnection.INSTANCE.mongoClient.getDB("caching_db").getCollection("families").createIndex(indexOptions);
		
		
	}

	
	public void andLogicalComparison_Example(DBCollection collection) {
	    BasicDBObject andQuery = new BasicDBObject();
	    List<BasicDBObject> obj = new ArrayList<BasicDBObject>();
	    obj.add(new BasicDBObject("employeeId", 2));
	    obj.add(new BasicDBObject("employeeName", "TestEmployee_2"));
	    andQuery.put("$and", obj);
	 
	    //System.out.println(andQuery.toString());
	 
	    DBCursor cursor = collection.find(andQuery);
	    while (cursor.hasNext()) {
	        System.out.println(cursor.next());
	    }
	}
	
	public int getSizeOfUniqueNgramCollection(String database, String family) {
		DBCollection collection = MongoConnection.INSTANCE.mongoClient.getDB(database).getCollection(family);
		DBCursor result = collection.find();
		return result.count();
	}
	
	public synchronized boolean isFamilyAlreadyInDatabase(FamiliesEntity f) {
	    boolean ret = false;
	    
	    DBCollection collection = MongoConnection.INSTANCE.mongoClient.getDB("caching_db").getCollection("families");
		BasicDBObject andQuery = new BasicDBObject();
		andQuery.put("family", f.family);
		//System.out.println(andQuery.toString());
	 
	    DBCursor cursor = collection.find(andQuery);
	    while (cursor.hasNext()) {
	        //System.out.println(cursor.next());
	        ret = true;
	        return ret;
	    }
	    
	    return ret;
	}
	
	public boolean isSampleAlreadyInDatabase(SampleEntity s) {
	    boolean ret = false;
	    
	    DBCollection collection = MongoConnection.INSTANCE.mongoClient.getDB("caching_db").getCollection("samples");
	    
	    BasicDBObject andQuery = new BasicDBObject();
	    List<BasicDBObject> obj = new ArrayList<BasicDBObject>();
	    obj.add(new BasicDBObject("filename", s.filename));
	    obj.add(new BasicDBObject("hash", s.hash));
	    //obj.add(new BasicDBObject("base_addr", s.base_addr));
	    andQuery.put("$and", obj);
	 
	    //System.out.println(andQuery.toString());
	    
	    DBCursor cursor = collection.find(andQuery);
	    while (cursor.hasNext()) {
	        //System.out.println(cursor.next());
	        ret = true;
	        return ret;
	    }
	    
	    return ret;
	}

	public boolean areNgramsAlreadyInDatabase(NgramEntity ngramEntity) {
		boolean ret = false;
	    
	    DBCollection collection = MongoConnection.INSTANCE.mongoClient.getDB("caching_db").getCollection("ngrams");
	    
	    BasicDBObject andQuery = new BasicDBObject();
	    List<BasicDBObject> obj = new ArrayList<BasicDBObject>();
	    obj.add(new BasicDBObject("n", ngramEntity.n));
	    obj.add(new BasicDBObject("hash", ngramEntity.hash));
	    obj.add(new BasicDBObject("filename", ngramEntity.filename));
	    //obj.add(new BasicDBObject("ngram", ngramEntity.ngram));
	    //obj.add(new BasicDBObject("base_addr", s.base_addr));
	    andQuery.put("$and", obj);
	 
	    //System.out.println(andQuery.toString());
	    
	    DBCursor cursor = collection.find(andQuery);
	    if(cursor.hasNext()) { // && element.containsField("ngram")) {
	    	//System.out.println(element.toString());
	        ret = true;
	        return ret;
	    }
	    
	    return ret;
	}
	
	public void writeFamilyToDatabase(SMDA smda) {
		String json = MongoDataAdapter.INSTANCE.generateFamilyEntity(smda).toJson();
		DBCollection collection = MongoConnection.INSTANCE.mongoClient.getDB("caching_db").getCollection("families");
		DBObject dbObject = (DBObject)JSON.parse(json);
		collection.insert(dbObject);
	}

	public void writeSampleToDatabase(SMDA smda) {
		String json = MongoDataAdapter.INSTANCE.generateSampleEntity(smda).toJson();
		DBCollection collection = MongoConnection.INSTANCE.mongoClient.getDB("caching_db").getCollection("samples");
		DBObject dbObject = (DBObject)JSON.parse(json);
		collection.insert(dbObject);
	}

	public void writeNgramsToDatabase(SMDA smda, List<Ngram> ngrams, int n, int limit) {
		assert(ngrams != null);
		
		List<DBObject> allNgramEntites = new ArrayList<DBObject>();
		//System.out.println("size to commit: " + ngrams.size());
		//for(Ngram ng : ngrams) {
		for(int i=0; i<ngrams.size(); i++) {
			String json = MongoDataAdapter.INSTANCE.generateNgramEntity(smda, n, ngrams.get(i)).toJson();
			DBCollection collection = MongoConnection.INSTANCE.mongoClient.getDB("caching_db").getCollection("ngrams");
			allNgramEntites.add((DBObject)JSON.parse(json));
			
			if(!allNgramEntites.isEmpty() && (i != 0) && (i % limit == 0)) {
				collection.insert(allNgramEntites);
				allNgramEntites  = new ArrayList<DBObject>();
				//System.out.println("commited " + (i+1) + "/" + ngrams.size());
				
			} else if(i == ngrams.size()-1) {
				collection.insert(allNgramEntites);
				//System.out.println("commited " + (i+1) + "/" + ngrams.size());
			}
			
			/*
			else if (!allNgramEntites.isEmpty() && ngrams.size() - counter < limit) {
				collection.insert(allNgramEntites);
				allNgramEntites  = new ArrayList<DBObject>();
				System.out.println("commit out of limit: " + limit + " but reached: " + (ngrams.size() - counter) + " allNgramEntites.size() - counter < limit");
			} else {
				System.out.println("counter: " + counter + " - size: " + ngrams.size() + " - limit: " + limit + " - diff: " + (ngrams.size() - counter) );
			}
			*/
		}
	}	

	public List<String> getFamilies() {
		DBCollection familyCollection = MongoConnection.INSTANCE.mongoClient.getDB("caching_db").getCollection("families");
		BasicDBObject familyQuery = new BasicDBObject();
		List<BasicDBObject> familyResult = new ArrayList<>();
		DBCursor familyCursor = familyCollection.find();
		List<String> ret = new ArrayList<>();
		
		while(familyCursor.hasNext()) {
			DBObject db = familyCursor.next();
			if(db.containsField("family")) {
				ret.add((String) db.toMap().get("family"));
				//System.out.println(db.toMap().get("family"));
			} else {
				throw new UnsupportedOperationException();
			}
		}
		
		return ret;
	}
	
	public int countSamplesPerFamily(String family) {
		DBCollection sampleCollection = MongoConnection.INSTANCE.mongoClient.getDB("caching_db").getCollection("samples");
		DBObject sampleQuery = new QueryBuilder().start().put("family").is(family).get();
		DBCursor result = sampleCollection.find(sampleQuery);
		return result.count();
	}
	
	public List<String> getSamplesPerFamily(String family) {
		DBCollection sampleCollection = MongoConnection.INSTANCE.mongoClient.getDB("caching_db").getCollection("samples");
		DBObject sampleQuery = new QueryBuilder().start().put("family").is(family).get();
		DBCursor result = sampleCollection.find(sampleQuery);
		List<String> ret = new ArrayList<String>();
		while(result.hasNext()) {
			DBObject o = result.next();
			if(o.containsField("filename")) {
				ret.add((String) o.toMap().get("filename"));
			}
		}
		return ret;
	}
	
	public void filterNgramsIntoUniquenessCollection(String database, String collection) {
		/*
		 * ToDo: Execute the aggregation query:
		 * 
		 * collection is set at $out, database is currently caching_db, set in DB request handling
		 * 
		
		db.ngrams.aggregate( [ {$limit: 10000},
        { $project: { family: "$family", filename: "$filename", hash: "$hash",concatedOpcodes: { $reduce: {input: {$concatArrays: ["$ngram.ngramInstructions.opcodes"]}, 
            initialValue: "", in: { $concat : ["$$value", "#", "$$this"] }  } }, n: "$ngram.n" } }, 
        {$group: { _id : {concat: "$concatedOpcodes"}, families: { $addToSet: "$family" }, samples: { $addToSet: "$filename" }, ngramsCounted: {$sum:1}, n: {$first: "$n"} } }, 
        {$project: {_id:1,"concat": 1, "families": 1, "families_size": { $size: "$families"}, "samples": 1, "samples_size": {$size: "$samples"}, ngramsCounted: 1, n: 1, score: null} },
        {$out : "unique_ngrams_testing"} ], {allowDiskUse:true})
		
		 */
		
		DB db = MongoConnection.INSTANCE.mongoClient.getDB(database);
		DBCollection c = db.getCollection("ngrams");
		Builder builder = AggregationOptions.builder().allowDiskUse(true);
		
		/*
		 * Monster query
		 */
		List<DBObject> aggQuery = Arrays.<DBObject>asList(
				new BasicDBObject("$project", new BasicDBObject("family", "$family")
						.append("filename","$filename")
						.append("hash","$hash")
						.append("concat", "$concat")
						/*
						 * changed ngrams table, this step is now unnecessary and therefore the complete project before group
						 * 
						.append("concatedOpcodes", 
								new BasicDBObject("$reduce",
										new BasicDBObject("input",
												new BasicDBObject("$concatArrays", Arrays.asList("$ngram.ngramInstructions.opcodes"))
												)
										.append("initialValue", "")
										.append("in",
												new BasicDBObject("$concat", Arrays.asList("$$value", "#", "$$this"))
										)
								)
						)
						*/
						.append("n", "$n")
				),
				new BasicDBObject("$group",
						new BasicDBObject("_id",
								new BasicDBObject("concat", "$concat")
						)
						.append("families", new BasicDBObject("$addToSet", "$family"))
						//.append("families_unique_size", new BasicDBObject("$size", 1))
						.append("samples", new BasicDBObject("$addToSet", "$filename"))
						.append("ngramsCounted", new BasicDBObject("$sum", 1))
						.append("n", new BasicDBObject("$first", "$n"))
				),
				new BasicDBObject("$project",
						new BasicDBObject("_id", 1)
						.append("concat", 1)
						.append("families", 1)
						.append("families_size", new BasicDBObject("$size", "$families"))
						.append("samples", 1)
						.append("samples_size", new BasicDBObject("$size", "$samples"))
						.append("ngramsCounted", 1)
						.append("n", 1)
						.append("score", null)
				),
				new BasicDBObject("$out", collection)
		);
		
		//Execute the query, should take like 2h
		System.out.println(builder.toString());
	    System.out.println(aggQuery.toString());
	    Cursor cursor = c.aggregate(aggQuery, builder.build());
	    if(cursor.hasNext()) {
	    	DBObject i = cursor.next();
	    	System.out.println( i.toMap().toString() );
	    }
	    
		DBObject indexOptions = new BasicDBObject();
		indexOptions.put("families_size", 1);
		indexOptions.put("families", 1);
		indexOptions.put("samples_size", -1);
	    db.getCollection(collection).createIndex(indexOptions);
	    
	    indexOptions = new BasicDBObject();
		indexOptions.put("families_size", 1);
		indexOptions.put("samples_size", -1);
	    db.getCollection(collection).createIndex(indexOptions);
	}
	
	@Deprecated
	public void filterNgramsForUniqueness() {
		DBCollection familyCollection = MongoConnection.INSTANCE.mongoClient.getDB("caching_db").getCollection("families");
		
		List<String> allFamilies = getFamilies();
		boolean barrier = false;
		
		for(String family : allFamilies) {
			barrier = checkIfCollectionExists("caching_db", family.replace('.', '_'));
			if(barrier == true) {
				continue;
			}
			long samplesPerFamily = countSamplesPerFamily(family);
			
			/*
			 * Warning: SQL INJECTION STRAIGHT AHEAD - - - FIXME!
			 */
			String insertionPerCommand = "db.ngrams.aggregate( [ {$match: { family: \"" + family + "\" } }, { $project: { family: \"$family\", filename: \"$filename\", hash: \"$hash\", concatedOpcodes: { $reduce: {input: {$concatArrays: [\"$ngram.ngramInstructions.opcodes\"]}, initialValue: \"\", in: { $concat : [\"$$value\", \"$$this\"] }  } }, ngram: \"$ngram\" } }, {$group: { _id : \"$concatedOpcodes\", ngramsCounted: {$sum:1}, detectedNgrams: { $push: \"$$ROOT\" } }}, {$match: {\"ngramsCounted\" : " + samplesPerFamily + " } }, {$out : \"" + family.replace('.', '_') + "\"} ], {allowDiskUse:true})";

			System.out.println(insertionPerCommand);
			MongoConnection.INSTANCE.mongoClient.getDB("caching_db").eval(insertionPerCommand);
		}
	}

	public List<Ngram> getNgramsOrdered(String database, String collection, String family, int limit) {
		/*
		 * The actual used method
		 */
		List<Ngram> l = new ArrayList<>();
		
		DBCollection familyCollection = MongoConnection.INSTANCE.mongoClient.getDB(database).getCollection(collection);
		DBCursor cursor = familyCollection.find(new BasicDBObject("families_size", 1).append("families", Arrays.asList(family.toString())))
				.sort(new BasicDBObject("samples_size", -1))
				.limit(limit);
		System.out.println(cursor.getQuery().toString());
		while(cursor.hasNext()) {
			DBObject result = cursor.next();
			int n = (int)result.toMap().get("n");
			int score = 0;
			if(result.toMap().get("score") != null) {
				score = Integer.parseInt(result.toMap().get("score").toString());
			} else {
				score = Integer.parseInt(result.toMap().get("samples_size").toString()) * 1000;
			}
			Ngram ngram = new Ngram(n);
			ngram.score = score;
			String ngramString = ((DBObject)result.toMap().get("_id")).get("concat").toString();
			
			List<Instruction> ngramInstructions = new ArrayList<>();
			
			String[] opcodes = ngramString.split("#");

			for(String opcode: opcodes) {
				if(opcode.equals("")) continue;
				Instruction ins = new Instruction();
				ins.setOpcodes(opcode);
				//System.out.println(opcodes[i]);
				ngramInstructions.add(ins);
			}
			ngram.setNgramInstructions(ngramInstructions);
			l.add(ngram);
			//System.out.println(ngram.toString());
		}
		
		return l;
	}
	
	public boolean checkIfCollectionExists(String database, String collection) {
		DB db = MongoConnection.INSTANCE.mongoClient.getDB(database);
		return db.collectionExists(collection);
	}

	public List<String> getSamples() {
		DBCollection sampleCollection = MongoConnection.INSTANCE.mongoClient.getDB("caching_db").getCollection("samples");
		BasicDBObject sampleQuery = new BasicDBObject();
		List<BasicDBObject> sampleResult = new ArrayList<>();
		DBCursor sampleCursor = sampleCollection.find();
		List<String> ret = new ArrayList<>();
		
		while(sampleCursor.hasNext()) {
			DBObject db = sampleCursor.next();
			if(db.containsField("filename")) {
				ret.add((String) db.toMap().get("filename"));
				//System.out.println(db.get("family"));
			} else {
				throw new UnsupportedOperationException();
			}
		}
		
		return ret;
	}

	public List<Ngram> getInstructionWithoutLookupTable(List<Ngram> ngramsRO) throws OperationNotSupportedException {
		List<Ngram> ngrams = new ArrayList<>();
		DBCollection ngramsCollection = MongoConnection.INSTANCE.mongoClient.getDB("caching_db").getCollection("ngrams");
				
		for(Ngram ngram: ngramsRO) {
			Ngram ngramRet = new Ngram(ngram.n);
			ngramRet.score = ngram.score;

			String concat = "";
			for(Instruction ins : ngram.getNgramInstructions()) {
				concat = concat.concat("#" + ins.getOpcodes());
			}
			DBObject sampleQuery = QueryBuilder.start().put("concat").is(concat).get();
			DBObject o  = ngramsCollection.findOne(sampleQuery);

			if(o == null) {
				throw new IllegalStateException("The ngram could not be found in ngrams collection. This should not happen...");
			}
			
			for(BasicDBObject bson: (List<BasicDBObject>)((DBObject) o.toMap().get("ngram")).toMap().get("ngramInstructions") ) {
				Instruction ins = new Instruction();
				ins.setMnemonics(new ArrayList<String>());
				try {
					ins.setOffset(((Number)bson.get("offset")).longValue());
				} catch(java.lang.ClassCastException e) {
					System.err.println("REAL PROBLEM OCCURED");
					e.printStackTrace();
				}
				ins.setOpcodes((String) bson.get("opcodes"));
				BasicBSONList mnemonics = (BasicBSONList) bson.get("mnemonics");
				for(Object mnemonic : mnemonics) {
					ins.getMnemonics().add((String) mnemonic);
				}
				ngramRet.getNgramInstructions().add(ins);
			}
			ngrams.add(ngramRet);
		}
		
		return ngrams;
	}

	
	@Deprecated
	public List<Ngram> getInstruction(List<Ngram> ngramsRO) throws OperationNotSupportedException {
		List<Ngram> ngrams = new ArrayList<>();
		DBCollection lookupCollection4 = MongoConnection.INSTANCE.mongoClient.getDB("ranking_db").getCollection("lookup_4");
		DBCollection lookupCollection5 = MongoConnection.INSTANCE.mongoClient.getDB("ranking_db").getCollection("lookup_5");
		DBCollection lookupCollection6 = MongoConnection.INSTANCE.mongoClient.getDB("ranking_db").getCollection("lookup_6");
		DBCollection lookupCollection7 = MongoConnection.INSTANCE.mongoClient.getDB("ranking_db").getCollection("lookup_7");
		
		List<Ngram> ngrams4 = new ArrayList<>();
		List<Ngram> ngrams5 = new ArrayList<>();
		List<Ngram> ngrams6 = new ArrayList<>();
		List<Ngram> ngrams7 = new ArrayList<>();
		
		for(Ngram ngramRO : ngramsRO) {
			switch (ngramRO.n) {
			case 4:
				ngrams4.add(ngramRO);
				break;
			case 5:
				ngrams5.add(ngramRO);
				break;
			case 6:
				ngrams6.add(ngramRO);
				break;
			case 7:
				ngrams7.add(ngramRO);
				break;

			default:
				throw new OperationNotSupportedException();
			}
		}
		
		for(Ngram ngram: ngrams4) {
			Ngram ngramRet = new Ngram(ngram.n);
			ngramRet.score = ngram.score;

			String concat = "";
			for(Instruction ins : ngram.getNgramInstructions()) {
				concat = concat.concat("#" + ins.getOpcodes());
			}
			DBObject sampleQuery = QueryBuilder.start().put("concat").is(concat).get();
			DBObject o  = lookupCollection4.findOne(sampleQuery);

			if(o == null) {
				throw new IllegalStateException("The ngram could not be found in ranking_db lookup table. Initialization error?");
			}
			
			for(BasicDBObject bson: (List<BasicDBObject>) o.toMap().get("ngrams")) {
				Instruction ins = new Instruction();
				ins.setMnemonics(new ArrayList<String>());
				try {
					ins.setOffset(((Number)bson.get("offset")).longValue());
				} catch(java.lang.ClassCastException e) {
					System.err.println("REAL PROBLEM OCCURED");
					e.printStackTrace();
				}
				ins.setOpcodes((String) bson.get("opcodes"));
				BasicBSONList mnemonics = (BasicBSONList) bson.get("mnemonics");
				for(Object mnemonic : mnemonics) {
					ins.getMnemonics().add((String) mnemonic);
				}
				ngramRet.getNgramInstructions().add(ins);
			}
			ngrams.add(ngramRet);
		}
		
		for(Ngram ngram: ngrams5) {
			Ngram ngramRet = new Ngram(ngram.n);
			ngramRet.score = ngram.score;

			String concat = "";
			for(Instruction ins : ngram.getNgramInstructions()) {
				concat = concat.concat("#" + ins.getOpcodes());
			}
			DBObject sampleQuery = QueryBuilder.start().put("concat").is(concat).get();
			DBObject o  = lookupCollection5.findOne(sampleQuery);
			for(BasicDBObject bson: (List<BasicDBObject>) o.toMap().get("ngrams")) {
				Instruction ins = new Instruction();
				ins.setMnemonics(new ArrayList<String>());
				try {
					ins.setOffset(((Number)bson.get("offset")).longValue());
				} catch(java.lang.ClassCastException e) {
					System.err.println("REAL PROBLEM OCCURED");
					e.printStackTrace();
				}
				ins.setOpcodes((String) bson.get("opcodes"));
				BasicBSONList mnemonics = (BasicBSONList) bson.get("mnemonics");
				for(Object mnemonic : mnemonics) {
					ins.getMnemonics().add((String) mnemonic);
				}
				ngramRet.getNgramInstructions().add(ins);
			}
			ngrams.add(ngramRet);
		}
		
		for(Ngram ngram: ngrams6) {
			Ngram ngramRet = new Ngram(ngram.n);
			ngramRet.score = ngram.score;

			String concat = "";
			for(Instruction ins : ngram.getNgramInstructions()) {
				concat = concat.concat("#" + ins.getOpcodes());
			}
			DBObject sampleQuery = QueryBuilder.start().put("concat").is(concat).get();
			DBObject o  = lookupCollection6.findOne(sampleQuery);
			for(BasicDBObject bson: (List<BasicDBObject>) o.toMap().get("ngrams")) {
				Instruction ins = new Instruction();
				ins.setMnemonics(new ArrayList<String>());
				try {
					ins.setOffset(((Number)bson.get("offset")).longValue());
				} catch(java.lang.ClassCastException e) {
					System.err.println("REAL PROBLEM OCCURED");
					e.printStackTrace();
				}
				ins.setOpcodes((String) bson.get("opcodes"));
				BasicBSONList mnemonics = (BasicBSONList) bson.get("mnemonics");
				for(Object mnemonic : mnemonics) {
					ins.getMnemonics().add((String) mnemonic);
				}
				ngramRet.getNgramInstructions().add(ins);
			}
			ngrams.add(ngramRet);
		}
		
		for(Ngram ngram: ngrams7) {
			Ngram ngramRet = new Ngram(ngram.n);
			ngramRet.score = ngram.score;

			String concat = "";
			for(Instruction ins : ngram.getNgramInstructions()) {
				concat = concat.concat("#" + ins.getOpcodes());
			}
			DBObject sampleQuery = QueryBuilder.start().put("concat").is(concat).get();
			DBObject o  = lookupCollection7.findOne(sampleQuery);
			for(BasicDBObject bson: (List<BasicDBObject>) o.toMap().get("ngrams")) {
				Instruction ins = new Instruction();
				ins.setMnemonics(new ArrayList<String>());
				try {
					ins.setOffset(((Number)bson.get("offset")).longValue());
				} catch(java.lang.ClassCastException e) {
					System.err.println("REAL PROBLEM OCCURED");
					e.printStackTrace();
				}
				ins.setOpcodes((String) bson.get("opcodes"));
				BasicBSONList mnemonics = (BasicBSONList) bson.get("mnemonics");
				for(Object mnemonic : mnemonics) {
					ins.getMnemonics().add((String) mnemonic);
				}
				ngramRet.getNgramInstructions().add(ins);
			}
			ngrams.add(ngramRet);
		}
		return ngrams;
	}

	@Deprecated
	public void filterNgramsIntoUniquenessCollectionV2(String database, String collection) {
		/*
		 * MongoDB is too slow when it comes to aggregation
		 * it takes so long that I decided to write a new logic for our problem in JAVA instead _one_ query:
		 * 
		 * 2018-12-17T15:16:09.234+0100 I COMMAND  [conn1] command caching_db.ngrams appName: "MongoDB Shell" command:
		 * aggregate { aggregate: "ngrams", pipeline: [ { $sort: { concat: 1.0 } }, { $project: { concat: 1.0 } } ], cursor: {}, $db: "caching_db" }
		 * planSummary: IXSCAN { concat: 1 } cursorid:9119997247359216231 keysExamined:15625 docsExamined:15625 numYields:6930 nreturned:101 reslen:7605
		 * locks:{ Global: { acquireCount: { r: 13864 } }, Database: { acquireCount: { r: 6932 } }, Collection: { acquireCount: { r: 6932 } } }
		 * protocol:op_msg 188540ms
		 * 
		 * So what do we see: 188540ms instead of 4h for the database. Using the sorted derived collection, we cursor until we find a different 
		 * 
		 * 
		 * 
		 */
		
		DB db = MongoConnection.INSTANCE.mongoClient.getDB(database);
		DBCollection c = db.getCollection("ngrams");
/*
		Builder builder = AggregationOptions.builder().allowDiskUse(true);
		
		List<DBObject> aggQuery = Arrays.<DBObject>asList(
				new BasicDBObject("$sort", new BasicDBObject("concat", 1)
						
				),
				new BasicDBObject("$project",
						new BasicDBObject("concat", 1)
						.append("n", 1)
						.append("family", 1)
						.append("filename", 1)
				)
		);
		
		//Execute the query, should take like 2h
		System.out.println(builder.toString());
	    System.out.println(aggQuery.toString());
	    Cursor cursor = c.aggregate(aggQuery, builder.build());
*/
		
		Cursor cursor = c.find();//.sort(new BasicDBObject("concat", 1));
		/*

	      				new BasicDBObject("$group",
						new BasicDBObject("_id",
								new BasicDBObject("concat", "$concat")
						)
						.append("families", new BasicDBObject("$addToSet", "$family"))
						.append("samples", new BasicDBObject("$addToSet", "$filename"))
						.append("ngramsCounted", new BasicDBObject("$sum", 1))
						.append("n", new BasicDBObject("$first", "$n"))
				),
	      				new BasicDBObject("$project",
						new BasicDBObject("_id", 1)
						.append("concat", 1)
						.append("families", 1)
						.append("families_size", new BasicDBObject("$size", "$families"))
						.append("samples", 1)
						.append("samples_size", new BasicDBObject("$size", "$samples"))
						.append("ngramsCounted", 1)
						.append("n", 1)
						.append("score", null)
				),
				new BasicDBObject("$out", collection)
	     */
	    
	    ArrayList<UniqueSamplesAggregationEntity> newEntryList = new ArrayList<>();
	    ArrayList<NgramUniqueEntity> uniqueSamplesContent = new ArrayList<>();
	    
	    
	    Gson gson = new GsonBuilder().setPrettyPrinting().create();
	    
	    int cursorCounter = 0;
	    
	    while(cursor.hasNext()) {
	    	cursorCounter++;
	    	
	    	DBObject i = cursor.next();
	    	 
	  /*
	    	
	    	//System.out.println( i.toMap().toString() );
	    	
	    	UniqueSamplesAggregationEntity newEntry = new UniqueSamplesAggregationEntity();
	    	newEntry.concat = (String) i.toMap().get("concat");
	    	newEntry.n = (int) i.toMap().get("n");
	    	newEntry.filename = (String) i.toMap().get("filename");
	    	newEntry.family = (String) i.toMap().get("family");
	    	newEntryList.add(newEntry);

	    	int listSize = newEntryList.size();
	    	if(listSize>=2 && !newEntryList.get(listSize-1).concat.equalsIgnoreCase(newEntryList.get(listSize-2).concat)) {
	    		//The last concat equals not the one before, so we have a new unique concat/ngram:
	    		
	    		NgramUniqueEntity ngram = new NgramUniqueEntity();
	    		ngram.families = new HashSet<>();
	    		ngram.samples = new HashSet<>();
	    		int ngramsCounted = 0;
	    		
	    		UniqueSamplesAggregationEntity lastOneInList = newEntryList.get(listSize-1);
	    		newEntryList.remove(listSize-1);
	    		
	    		for(UniqueSamplesAggregationEntity entity: newEntryList) {
	    			ngramsCounted++;
	    			//System.out.println(ngram.concat);
		    		ngram.concat = entity.concat;
		    		ngram.families.add(entity.family);
		    		ngram.families_size = ngram.families.size();
		    		ngram.samples.add(entity.filename);
		    		ngram.samples_size = ngram.samples.size();
		    		ngram.ngramsCounted = ngramsCounted;
		    		ngram.n = entity.n;
		    		ngram.score = 0;
	    		}
	    		
	    		//insert the entryList or batch query it:
	    		uniqueSamplesContent.add(ngram);
	    		if(uniqueSamplesContent.size() == 5000) {
	    			System.out.println("Writing into DB... cursorCounter: " + cursorCounter);
	    			DBCollection insertHere = MongoConnection.INSTANCE.mongoClient.getDB(database).getCollection(collection);
	    			
	    			List<DBObject> documents = new ArrayList<>();
	    			for(NgramUniqueEntity entry:  uniqueSamplesContent) {
	    				documents.add( (DBObject)JSON.parse(gson.toJson(entry, NgramUniqueEntity.class)));
	    			}
					insertHere.insert(documents );
	    			
	    			uniqueSamplesContent = new ArrayList<>();
	    		}
	    		
	    		newEntryList = new ArrayList<>();
	    		newEntryList.add(lastOneInList);
	    		
	    		//System.out.println(gson.toJson(ngram, NgramUniqueEntity.class));
	    	}

	  */
	    	
	    	if(cursorCounter%100000==0)System.out.println("cursorCounter: " + cursorCounter);
	    }
	    
	    /*
	     * insert the remaining uniqueSamplesContent:
	     */
		DBCollection insertHere = MongoConnection.INSTANCE.mongoClient.getDB(database).getCollection(collection);
		
		List<DBObject> documents = new ArrayList<>();
		for(NgramUniqueEntity entry:  uniqueSamplesContent) {
			documents.add( (DBObject)JSON.parse(gson.toJson(entry, NgramUniqueEntity.class)));
		}
		insertHere.insert(documents );

	    
		DBObject indexOptions = new BasicDBObject();
		indexOptions.put("families_size", 1);
		indexOptions.put("families", 1);
		indexOptions.put("samples_size", -1);
	    db.getCollection(collection).createIndex(indexOptions);
	    
	    indexOptions = new BasicDBObject();
		indexOptions.put("families_size", 1);
		indexOptions.put("samples_size", -1);
	    db.getCollection(collection).createIndex(indexOptions);
	}
}
