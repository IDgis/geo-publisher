package nl.idgis.publisher.provider;

import java.sql.ResultSet;
import java.sql.SQLException;

import nl.idgis.publisher.protocol.database.Record;
import nl.idgis.publisher.protocol.database.SpecialValue;
import nl.idgis.publisher.protocol.stream.SyncStreamCursor;

public class DatabaseCursor extends SyncStreamCursor<ResultSet, Record> {

	protected DatabaseCursor(ResultSet t) {
		super(t);
	}

	@Override
	protected Record syncNext() throws Exception {
		Object[] values = new Object[t.getMetaData().getColumnCount()];
		for(int i = 0; i < values.length; i++) {
			Object o = t.getObject(i + 1);
			if(o instanceof Number || o instanceof String) {
				values[i] = o;
			} else {
				values[i] = new SpecialValue();
			}
		}		

		return new Record(values);
	}

	@Override
	protected boolean hasNext() throws Exception {		
		return t.next();
	}
	
	@Override
	public void postStop() throws SQLException {
		t.getStatement().close();
		t.close();
	}
}
