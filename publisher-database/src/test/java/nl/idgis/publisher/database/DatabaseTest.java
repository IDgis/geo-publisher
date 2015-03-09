package nl.idgis.publisher.database;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.SQLTemplates;

import static nl.idgis.publisher.database.QVersion.version;

public class DatabaseTest {
	
	Connection connection;
	
	@Before
	public void getConnection() throws Exception {
		Properties properties = new Properties();
		properties.load(new FileInputStream("target/database/publisher.properties"));
		
		Class.forName((String)properties.get("database.driver"));
		
		String url = (String)properties.get("database.url");
		String user = (String)properties.get("database.user");
		String password = (String)properties.get("database.password");
		
		connection = DriverManager.getConnection(url, user, password);		
	}
	
	@After
	public void closeConnection() throws Exception {
		connection.close();
	}

	@Test
	public void testVersion() throws Exception {		
		SQLTemplates templates = new ExtendedPostgresTemplates();
		SQLQuery query = new SQLQuery(connection, templates);
		for(Tuple tuple : query.from(version).list(version.id, version.createTime)) {
			Integer id = tuple.get(version.id);
			Timestamp createTime = tuple.get(version.createTime);
			
			System.out.println("id: " + id + " createTime: " + createTime);
		}
	}
	
	@Test
	public void testWithRecursive() throws Exception {
		Statement stmt = connection.createStatement();
		
		stmt.execute("drop table if exists content");
		
		stmt.execute("create table content (parent varchar(80), child varchar(80))");		
		
		stmt.executeUpdate("insert into content(parent, child) values "
				+ "('root', 'group-a'), "
				+ "('root', 'group-b'), "
				+ "('group-a', 'group-b'), "
				+ "('group-b', 'group-a'), "
				+ "('group-a', 'layer'), "
				+ "('group-b', 'layer'), "
				+ "('unrelated', 'also-unrelated')");
		
		ResultSet rs = stmt.executeQuery("with recursive structure(parent, child, root, path, cycle) as ("
				+ "select "
					+ "parent, "
					+ "child, parent root, "
					+ "'(' || child || ',' || parent || ')', "
					+ "false "
				+ "from content "
				+ "union all "
				+ "select "
					+ "c.parent, "
					+ "c.child, "
					+ "s.parent root, "
					+ "path || '(' || c.child || ',' || s.parent || ')', "
					+ "path like '%(' || c.child || ',' || s.parent || ')%' "
				+ "from content c "
				+ "join structure s on s.child = c.parent and not cycle) "
				+ "select * from structure");
		
		int rowNum = 1;
		while(rs.next()) {
			System.out.printf("%d: %s, %s, %s\n", rowNum++, rs.getString(1), rs.getString(2), rs.getString(3));
		}
		
		rs.close();
		
		stmt.close();
	}
}
