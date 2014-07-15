package nl.idgis.publisher.provider.database;

import nl.idgis.publisher.protocol.database.WKBGeometry;

import org.deegree.geometry.io.WKBWriter;
import org.deegree.sqldialect.oracle.sdo.SDOGeometryConverter;

import oracle.sql.STRUCT;
import akka.actor.Props;

@SuppressWarnings("deprecation")
public class OracleConverter extends DatabaseConverter {
	
	private final SDOGeometryConverter converter;
	
	public OracleConverter() {
		converter = new SDOGeometryConverter();
	}

	public static Props props() {
		return Props.create(OracleConverter.class);
	}
	
	@Override
	protected Object convert(Object value) throws Exception {
		if(value instanceof STRUCT) {
			return new WKBGeometry(WKBWriter.write(converter.toGeometry((STRUCT) value, null)));
		}
		
		return super.convert(value);
	}
}
