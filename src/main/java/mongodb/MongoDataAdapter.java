package mongodb;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import javax.naming.SizeLimitExceededException;

import org.bson.Document;
import org.bson.json.JsonParseException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import converters.ngrams.Ngram;
import db.entites.FamiliesEntity;
import db.entites.NgramCandidateEntity;
import db.entites.NgramCandidateListEntity;
import db.entites.NgramEntity;
import db.entites.SampleEntity;
import smtx_handler.Instruction;
import smtx_handler.SMDA;
@Deprecated
public enum MongoDataAdapter {
	INSTANCE;

	public FamiliesEntity generateFamilyEntity(SMDA smda) {
		FamiliesEntity f = new FamiliesEntity();
		f.family = smda.getFamily();
		return f;
	}

	public SampleEntity generateSampleEntity(SMDA smda) {
		SampleEntity s = new SampleEntity();
		s.architecture = smda.getArchitecture();
		s.base_addr = Long.toString(smda.getBase_addr());
		s.family = smda.getFamily();
		s.filename = smda.getFilename();
		s.hash = smda.getSha256();
		s.status = smda.getStatus();
		s.summary = smda.getSummary();
		s.timestamp = smda.getTimestamp();
		return s;
	}

	public NgramEntity generateNgramEntity(SMDA smda, int n, Ngram ngram) {
		NgramEntity ne = new NgramEntity();
		ne.family = smda.getFamily();
		ne.filename = smda.getFilename();
		ne.hash = smda.getSha256();
		ne.n = n;
		ne.ngram = ngram;
		if(ngram != null) {
			ne.concat = "";
			for(Instruction i: ngram.getNgramInstructions()) {
				ne.concat = ne.concat + "#" + i.getOpcodes();
			}
		}
		return ne;
	}
	
	@Deprecated
	public List<NgramCandidateListEntity> generateNgramCandidateListEntityWithLimit(String database, String collection, int limit) {
		DB db = MongoConnection.INSTANCE.mongoClient.getDB(database);
		DBCollection co = db.getCollection(collection);
		DBCursor cursor = co.find().limit(limit);
		
		List<NgramCandidateListEntity> ngramList = new ArrayList<>();
		while(cursor.hasNext()) {
			//System.out.println("next found: ");
			DBObject doc = cursor.next();

			String json = doc.toString();
			
			//System.out.println(json);
			
			GsonBuilder gsonBuilder = new GsonBuilder();
			Gson gson = gsonBuilder.create();
			
			/*
			 * ugly workaround, has to be implemented properly!
			 */
			
			try {
				NgramCandidateListEntity ncle = gson.fromJson(json, NgramCandidateListEntity.class);
				ngramList.add(ncle);
			} catch(JsonSyntaxException e) {
				NgramCandidateListEntity ncle = new NgramCandidateListEntity();
				ncle._id = (String) doc.get("_id");
				ncle.ngramsCounted = (int) Double.parseDouble(doc.get("ngramsCounted").toString());
				ncle.detectedNgrams = (List<NgramCandidateEntity>) doc.get("detectedNgrams");
				//System.out.println("family: " + collection + " sample: " + ncle.detectedNgrams.get(0).filename + "ngram: " + ncle.detectedNgrams.get(0).ngram.toString());

				/*
				for(NgramCandidateEntity i : ncle.detectedNgrams) {
					i.
				}
				*/
			}
		}
		return ngramList;
	}
	
	@Deprecated
	public List<NgramCandidateListEntity> generateNgramCandidateListEntity(String database, String collection) {
		DB db = MongoConnection.INSTANCE.mongoClient.getDB(database);
		DBCollection co = db.getCollection(collection);
		DBCursor cursor = co.find();
		
		List<NgramCandidateListEntity> ngramList = new ArrayList<>();
		while(cursor.hasNext()) {
			//System.out.println("next found: ");
			DBObject doc = cursor.next();

			String json = doc.toString();
			
			//System.out.println(json);
			
			GsonBuilder gsonBuilder = new GsonBuilder();
			Gson gson = gsonBuilder.create();
			
			/*
			 * ugly workaround, has to be implemented properly!
			 */
			
			try {
				NgramCandidateListEntity ncle = gson.fromJson(json, NgramCandidateListEntity.class);
				ngramList.add(ncle);
			} catch(JsonSyntaxException e) {
				NgramCandidateListEntity ncle = new NgramCandidateListEntity();
				ncle._id = (String) doc.get("_id");
				ncle.ngramsCounted = (int) Double.parseDouble(doc.get("ngramsCounted").toString());
				ncle.detectedNgrams = (List<NgramCandidateEntity>) doc.get("detectedNgrams");
				//System.out.println("family: " + collection + " sample: " + ncle.detectedNgrams.get(0).filename + "ngram: " + ncle.detectedNgrams.get(0).ngram.toString());

				/*
				for(NgramCandidateEntity i : ncle.detectedNgrams) {
					i.
				}
				*/
			}
		}
		return ngramList;
	}
	
	@Deprecated
	public List<Ngram> getNgramsFromDatabaseWithLimitAndSmapleCounter(String database, String collection, int hardlimit, int databaseLimit, int sampleCounter) throws SizeLimitExceededException {
		MongoHandler mh = new MongoHandler();
		int samplesPerFamily = mh.countSamplesPerFamily(collection.replaceFirst("_", "."));
		List<NgramCandidateListEntity> ncle = null;
		int entriesPerFamily = mh.getSizeOfUniqueNgramCollection(database, collection);
		System.out.println("entries in family found: " + entriesPerFamily);
		if(entriesPerFamily > hardlimit) {
			System.out.println("hardlimit: " + hardlimit + " reached with " + entriesPerFamily);
			throw new SizeLimitExceededException("The Family " + collection + " is too big for this filter!");
		}
		
		if(samplesPerFamily < sampleCounter) {
			//this was with limit:
			//ncle = generateNgramCandidateListEntityWithLimit(database, collection, databaseLimit);
			ncle = generateNgramCandidateListEntity(database, collection);
		} else {
			ncle = generateNgramCandidateListEntity(database, collection);
		}
		assert(ncle!=null);
		HashSet<Ngram> ret = new HashSet<>();
		for(NgramCandidateListEntity i : ncle) {
			for(NgramCandidateEntity j : i.detectedNgrams) {
				ret.add(j.ngram);
			}
		}
		
		List<Ngram> list = new ArrayList<Ngram>();
		list.addAll(ret);
		return list;
	}
	
	public List<Ngram> getNgramsFromDatabaseWithLimit(String database, String collection, int limit) {
		List<NgramCandidateListEntity> ncle = generateNgramCandidateListEntityWithLimit(database, collection, limit);
		HashSet<Ngram> ret = new HashSet<>();
		for(NgramCandidateListEntity i : ncle) {
			for(NgramCandidateEntity j : i.detectedNgrams) {
				ret.add(j.ngram);
			}
		}
		
		List<Ngram> list = new ArrayList<Ngram>();
		list.addAll(ret);
		return list;
	}
	
	public List<Ngram> getNgramsFromDatabase(String database, String collection) {
		System.out.println("calculating ngram candidate list for " + collection);
		List<NgramCandidateListEntity> ncle = generateNgramCandidateListEntity(database, collection);
		HashSet<Ngram> ret = new HashSet<>();
		System.out.println("begin getting from database: ");
		int counter = 0;
		for(NgramCandidateListEntity i : ncle) {
			counter++;
			System.out.println("counter: " + counter + " - candidate: " + i._id + " - containing elements: " + i.ngramsCounted);
			for(NgramCandidateEntity j : i.detectedNgrams) {
				ret.add(j.ngram);
			}
		}
		
		List<Ngram> list = new ArrayList<Ngram>();
		System.out.println("adding all...");
		list.addAll(ret);
		return list;
	}

	@Deprecated
	public List<Ngram> getUniqueNgramsFromDatabase(String database, String collection, int hardlimit, int databaseLimit, int sampleCounter) throws SizeLimitExceededException {
		/*
		 * ToDo:
		 * query the new database and collection!
		 */
		
		MongoHandler mh = new MongoHandler();
		int samplesPerFamily = mh.countSamplesPerFamily(collection.replaceFirst("_", "."));
		List<NgramCandidateListEntity> ncle = null;
		int entriesPerFamily = mh.getSizeOfUniqueNgramCollection(database, collection);
		System.out.println("entries in family found: " + entriesPerFamily);
		if(entriesPerFamily > hardlimit) {
			System.out.println("hardlimit: " + hardlimit + " reached with " + entriesPerFamily);
			throw new SizeLimitExceededException("The Family " + collection + " is too big for this filter!");
		}
		
		if(samplesPerFamily < sampleCounter) {
			//this was with limit:
			//ncle = generateNgramCandidateListEntityWithLimit(database, collection, databaseLimit);
			ncle = generateNgramCandidateListEntity(database, collection);
		} else {
			ncle = generateNgramCandidateListEntity(database, collection);
		}
		assert(ncle!=null);
		HashSet<Ngram> ret = new HashSet<>();
		for(NgramCandidateListEntity i : ncle) {
			for(NgramCandidateEntity j : i.detectedNgrams) {
				ret.add(j.ngram);
			}
		}
		
		List<Ngram> list = new ArrayList<Ngram>();
		list.addAll(ret);
		return list;

	}

	public List<Ngram> getUniqueNgramCandidates(String database, String collection, String family, int limit) throws SizeLimitExceededException {
		List<Ngram> ret = new ArrayList<>();
		
		MongoHandler mh = new MongoHandler();

		List<Ngram> candidates = mh.getNgramsOrdered(database, collection, family, limit);
		
		/*
		int entriesPerFamily = mh.getSizeOfUniqueNgramCollection(database, collection);
		System.out.println("entries in family found: " + entriesPerFamily);
		if(entriesPerFamily > hardlimit) {
			System.out.println("hardlimit: " + hardlimit + " reached with " + entriesPerFamily);
			throw new SizeLimitExceededException("The Family " + collection + " is too big for this filter!");
		}
		*/
		
		return candidates;
	}
}
