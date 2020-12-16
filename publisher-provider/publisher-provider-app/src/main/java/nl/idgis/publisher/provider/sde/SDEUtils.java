package nl.idgis.publisher.provider.sde;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import nl.idgis.publisher.provider.database.messages.AbstractDatabaseColumnInfo;
import nl.idgis.publisher.provider.database.messages.ColumnFilter;
import nl.idgis.publisher.provider.database.messages.CompoundFilter;
import nl.idgis.publisher.provider.database.messages.FetchTable;
import nl.idgis.publisher.provider.database.messages.Filter;
import nl.idgis.publisher.provider.database.messages.SDEDatabaseColumnInfo;
import nl.idgis.publisher.provider.protocol.Record;
import nl.idgis.publisher.provider.protocol.Records;

final class SDEUtils {
	
	private SDEUtils() {}

	static Filter getItemsFilter() {
		return new CompoundFilter(
			"AND",
			new ColumnFilter(
				new SDEDatabaseColumnInfo(
					"PHYSICALNAME",
					"CHAR"),
				"IS NOT NULL"),
			new CompoundFilter(
				"OR",
				Stream.of(SDEItemInfoType.values())
					.map(itemInfoType ->
						new ColumnFilter(	
							new SDEDatabaseColumnInfo(
								"TYPE", 
								"CHAR"),
							"=", 
							itemInfoType.getUuid()))
					.toArray(length -> new ColumnFilter[length])));
	}
	
	static Filter getItemsFilter(String uuid) {
		return new CompoundFilter(
			"AND",
			getItemsFilter(),
			new ColumnFilter(
				new SDEDatabaseColumnInfo(
						"UUID", 
						"CHAR"),
					"=",
					Objects.requireNonNull(uuid, "uuid should not be null")));
	}
	
	static FetchTable getFetchTable(Filter filter, String databaseScheme) {
		List<AbstractDatabaseColumnInfo> columns = new ArrayList<>();
		columns.add(new SDEDatabaseColumnInfo("TYPE", "CHAR"));
		columns.add(new SDEDatabaseColumnInfo("UUID", "CHAR"));
		columns.add(new SDEDatabaseColumnInfo("PHYSICALNAME", "CHAR"));
		columns.add(new SDEDatabaseColumnInfo("DOCUMENTATION", "CLOB"));
		
		return new FetchTable(
			databaseScheme + ".GDB_ITEMS_VW", 
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
		
		// Note: parameter order should match column order in 'getFetchTable'
		return new SDEItemInfo(
			valueItr.next(), // uuid
			SDEItemInfoType.fromUuid(valueItr.next()), // type 
			valueItr.next(), // physicalname
			valueItr.next()); // documentation
	}
}
