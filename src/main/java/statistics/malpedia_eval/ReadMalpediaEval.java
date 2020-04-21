package statistics.malpedia_eval;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ReadMalpediaEval {
		
	public MalpediaEval getFileContent(String path) throws IOException {
		File file = new File(path);
		Reader r = new FileReader(file);
		Gson gson = new GsonBuilder().setPrettyPrinting().registerTypeAdapter(MalpediaEval.class, new MalpediaEvalDeserializer()).create();
		MalpediaEval m = gson.fromJson(r, MalpediaEval.class);
		return m;
	}
	
}
