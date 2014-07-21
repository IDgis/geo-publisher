package nl.idgis.publisher.database;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Timestamp;
import java.util.Properties;

import org.junit.Test;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.SQLTemplates;

import static nl.idgis.publisher.database.QVersion.version;

public class DatabaseTest {

	@Test
	public void testVersion() throws Exception {
		Properties properties = new Properties();
		properties.load(new FileInputStream("target/database/publisher.properties"));
		
		Class.forName((String)properties.get("database.driver"));
		
		String url = (String)properties.get("database.url");
		String user = (String)properties.get("database.user");
		String password = (String)properties.get("database.password");
		Connection connection = DriverManager.getConnection(url, user, password);
		
		SQLTemplates templates = new ExtendedPostgresTemplates();
		SQLQuery query = new SQLQuery(connection, templates);
		for(Tuple tuple : query.from(version).list(version.id, version.createTime)) {
			Integer id = tuple.get(version.id);
			Timestamp createTime = tuple.get(version.createTime);
			
			System.out.println("id: " + id + " createTime: " + createTime);
		}
		
		connection.close();
	}
}
