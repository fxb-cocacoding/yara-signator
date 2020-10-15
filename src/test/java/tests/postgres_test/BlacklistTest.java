package tests.postgres_test;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import main.Config;
import postgres.HandleStructures;
import postgres.PostgresRequestUtils;
import tests.setup.*;

public class BlacklistTest {
	
	static Config config = null;
	static Connection con = null;
		
	@BeforeClass
	public static void init() {
		System.out.println("init is called");
		try {
			config = ConfigReader.readConfig();
			con = DatabaseSetup.createDBConnection(config);
			new HandleStructures().dropRangePartitionedBlacklistTable("blacklist");
			new HandleStructures().createRangePartitionedBlacklistTable("blacklist");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testInsertIntoBlacklist() {
		List<String> testset = new ArrayList<String>();
		String testString = "aaaaaa";
		testset.add(testString);
		testset.add("faaaaa");
		testset.add("0aaaaa");
		testset.add("2aaaaa");

		try {
			for(String s : testset) {
				new PostgresRequestUtils().writeElementToBlacklist(s);
			}
		} catch(SQLException e) {
			System.out.println(e.getSQLState());
			e.printStackTrace();
		}
		
		/*
		 * Manual test
		 */
		int count = 0;
		try {
			String query = "SELECT COUNT(concat) FROM blacklist;";
			PreparedStatement pst = con.prepareStatement(query);
			pst.execute();
			con.commit();
			ResultSet rs = pst.getResultSet();
			rs.next();
			count = rs.getInt(1);
		} catch(SQLException e) {
			e.printStackTrace();
		}
		assertEquals(count, testset.size());
		
		count = 0;
		/*
		 * Test counted
		 */
		try {
			count = new PostgresRequestUtils().getBlacklistSize();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertEquals(count, 4);
		
		boolean assertMe = false;
		try {
			assertMe = new PostgresRequestUtils().isStringAlreadyInBlacklist(testString);
		} catch(SQLException e) {
			e.printStackTrace();
		}
		assertEquals(assertMe, true);
	}
	
	@AfterClass
	public static void close() throws SQLException {
		System.out.println("close is called, afterclass");
		new HandleStructures().dropRangePartitionedBlacklistTable("blacklist");
	}

}
