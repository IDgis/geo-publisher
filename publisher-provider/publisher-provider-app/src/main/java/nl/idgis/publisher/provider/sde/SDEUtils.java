package nl.idgis.publisher.provider.sde;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import nl.idgis.publisher.provider.database.messages.ColumnFilter;
import nl.idgis.publisher.provider.database.messages.CompoundFilter;
import nl.idgis.publisher.provider.database.messages.DatabaseColumnInfo;
import nl.idgis.publisher.provider.database.messages.FetchTable;
import nl.idgis.publisher.provider.database.messages.Filter;
import nl.idgis.publisher.provider.protocol.Record;
import nl.idgis.publisher.provider.protocol.Records;

final class SDEUtils {

	private SDEUtils() {}
	
	static Filter getItemsFilter() {
		return new CompoundFilter(
			"AND",
			new ColumnFilter(
				new DatabaseColumnInfo(
					"PHYSICALNAME",
					"CHAR"),
				"IS NOT NULL"),
			new CompoundFilter(
				"OR",
				new ColumnFilter(
					new DatabaseColumnInfo(
						"TYPE", 
						"CHAR"),
					"=",
					"{74737149-DCB5-4257-8904-B9724E32A530}" /* NAME = Feature Dataset */),
				new ColumnFilter(
					new DatabaseColumnInfo(
						"TYPE", 
						"CHAR"),
					"=", 
					"{CD06BC3B-789D-4C51-AAFA-A467912B8965}" /* NAME = Table */)));
	}
	
	static Filter getItemsFilter(String uuid) {
		return new CompoundFilter(
			"AND",
			getItemsFilter(),
			new ColumnFilter(
				new DatabaseColumnInfo(
						"UUID", 
						"CHAR"),
					"=",
					Objects.requireNonNull(uuid, "uuid should not be null")));
	}
	
	static FetchTable getFetchTable(Filter filter) {
		List<DatabaseColumnInfo> columns = new ArrayList<>();
		columns.add(new DatabaseColumnInfo("UUID", "CHAR"));
		columns.add(new DatabaseColumnInfo("PHYSICALNAME", "CHAR"));
		columns.add(new DatabaseColumnInfo("DOCUMENTATION", "CLOB"));
		
		return new FetchTable(
			"SDE.GDB_ITEMS_VW", 
			columns, 
			1 /* messageSize */, 
			Objects.requireNonNull(filter, "filter should not be null"));
	}
	
	static SDEItemInfo toItemInfo(Records records) {
		Record record = records.getRecords().get(0);
		List<Object> values = record.getValues();
		
		Iterator<String> valueItr = values.stream()
			.map(Optional::ofNullable)
			.map(value -> value.map(Object::toString))
			.map(value -> value.orElse(null))
			.iterator();
		
		return new SDEItemInfo(valueItr.next(), valueItr.next(), valueItr.next());
	}
}
