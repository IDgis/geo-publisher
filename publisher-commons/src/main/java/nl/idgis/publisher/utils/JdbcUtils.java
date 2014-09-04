package nl.idgis.publisher.utils;

import java.io.InputStream;
import java.sql.Statement;

import org.apache.commons.io.IOUtils;

public class JdbcUtils {

	private JdbcUtils() {
		
	}
	
	public static int runRev(Statement stmt, String path) throws Exception {
		return runRev(stmt, path, 0);
	}
	
	public static int runRev(Statement stmt, String path, int start) throws Exception {
		for(int i = start; ; i++) {
			InputStream is = getRev(path, i);
			if(is == null) {
				return i - 1;
			}
			
			run(stmt, is);
		}
	}
	
	public static int maxRev(String path) {
		return maxRev(path, 0);
	}
	
	public static int maxRev(String path, int start) {
		for(int i = start; ; i++) {
			if(getRev(path, i + 1) == null) {
				return i;
			}
		}
	}

	public static InputStream getRev(String path, int i) {
		StringBuilder sb = new StringBuilder(path + "/rev");
		if(i < 100) {
			sb.append("0");
		}
		
		if(i < 10) {
			sb.append("0");
		}
		
		sb.append(i);			
		sb.append(".sql");
		
		return JdbcUtils.class.getClassLoader().getResourceAsStream(sb.toString());		
	}
	
	public static void run(Statement stmt, InputStream is) throws Exception {
		StringBuilder sb = new StringBuilder();
		
		for(String line : IOUtils.readLines(is)) {
			if(line.contains("--")) {
				sb.append(line.substring(0, line.indexOf("--")));
			} else {
				sb.append(line);
			}
			
			sb.append('\n');
		}
			
		for(String sql : sb.toString().split(";")) {
			stmt.execute(sql);
		}
	}
}
