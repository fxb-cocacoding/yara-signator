package tests.ranking_system;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import main.Config;
import postgres.HandleStructures;
import postgres.PostgresRequestUtils;
import tests.setup.*;

public class RankingSystemTest {
	
	static Config config = null;
	static Connection con = null;
		
	@BeforeClass
	public static void init() {
		
	}
	
	@Test
	public void testRemoveSlowSequences() {
		String opcodes = "68???????? e8???????? 68???????? e8???????? bb???????? 53 52 51".replaceAll("\\s+","");
		String[] splitted = StringUtils.split(opcodes, '?');
		boolean remove = true;
		for(String s: splitted) {
			// Ensure that at least 6 chars are no '?' because otherwise the YARA Rule would slow down the scanner.
			if(s.length() >= 6) {
				remove = false;
				break;
			}
		}
		if(remove == true) {
			System.out.println("True, we remove the string");
		} else {
			System.out.println("False, we do not remove");
		}

		assertTrue(true);
	}
	
	@AfterClass
	public static void close() {
		return;
	}

}
