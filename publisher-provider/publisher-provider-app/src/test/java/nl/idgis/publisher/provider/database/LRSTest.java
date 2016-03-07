package nl.idgis.publisher.provider.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.deegree.geometry.Geometry;
import org.deegree.geometry.io.WKTWriter;
import org.deegree.sqldialect.oracle.sdo.SDOGeometryConverter;
import org.junit.Test;

import oracle.sql.STRUCT;

@SuppressWarnings("deprecation")
public class LRSTest {

	// @Test
	public void testLRS() throws Exception {
		SDOGeometryConverter converter = new SDOGeometryConverter();
		
		Class.forName("oracle.jdbc.OracleDriver");		
		Connection c = DriverManager.getConnection("jdbc:oracle:thin:@192.168.122.166:1521:GISBASIP", "raadpleger", "raadpleger");
		
		Statement stmt = c.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT SHAPE FROM B2.DUURZAME_OV_ARC");
		while(rs.next()) {
			STRUCT struct = (STRUCT)rs.getObject(1);
			
			Geometry geometry = converter.toGeometry(struct, null);
			
			System.out.println(WKTWriter.write(geometry));		
		}
		rs.close();
		
		stmt.close();
		
		c.close();
	}
}
