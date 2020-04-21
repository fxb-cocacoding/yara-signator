package aggregation_filter;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import converters.ngrams.Ngram;
import postgres.PostgresRequestUtils;


public class FilterFactory {
	
	public synchronized void applyFilter(String filter, List<Ngram> ngrams) {
		if(filter.equalsIgnoreCase("procedureStart")) {
			//System.out.println("apply procedure start filter");
			new FilterProcedureStart().apply(ngrams);
		} else if (filter.equalsIgnoreCase("dummyFilter")) {
			//System.out.println("apply dummy filter");
			new FilterDummy().apply(ngrams);
		} else {
			throw new java.lang.UnsupportedOperationException();
		}
	}
	
	public void filterFamiliesUniqueInPostgres(Map<String, Integer> families, List<Integer> allN) throws SQLException {
		new PostgresRequestUtils().dropPartitionedTables(families, allN);
		new PostgresRequestUtils().createPartitionedTables(families, allN);
		for(int n: allN) {
			new PostgresRequestUtils().insertIntoAgregationTablesPartitioned(n);
		}
	}
	
}
