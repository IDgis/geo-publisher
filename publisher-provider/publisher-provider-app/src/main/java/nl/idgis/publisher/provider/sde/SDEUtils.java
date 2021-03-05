package nl.idgis.publisher.provider.sde;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import nl.idgis.publisher.provider.database.messages.*;
import nl.idgis.publisher.provider.protocol.Record;
import nl.idgis.publisher.provider.protocol.Records;

final class SDEUtils {
	
	private SDEUtils() {}

	static Filter getItemsFilter(String databaseVendor) {
		return new CompoundFilter(
			"AND",
			new ColumnFilter(
					FactoryDatabaseColumnInfo.getDatabaseColumnInfo(
					"PHYSICALNAME",
					"CHAR",
							databaseVendor),
				"IS NOT NULL"),
			new CompoundFilter(
				"OR",
				Stream.of(SDEItemInfoType.values())
					.map(itemInfoType ->
						new ColumnFilter(
								FactoryDatabaseColumnInfo.getDatabaseColumnInfo(
								"TYPE", 
								"CHAR",
										databaseVendor),
							"=", 
							itemInfoType.getUuid()))
					.toArray(length -> new ColumnFilter[length])));
	}
	
	static Filter getItemsFilter(String uuid, String databaseVendor) {
		return new CompoundFilter(
			"AND",
			getItemsFilter(databaseVendor),
			new ColumnFilter(
				FactoryDatabaseColumnInfo.getDatabaseColumnInfo(
						"UUID", 
						"CHAR",
						databaseVendor),
					"=",
					Objects.requireNonNull(uuid, "uuid should not be null")));
	}
	
	static FetchTable getFetchTable(Filter filter, String databaseScheme, String databaseVendor) {

		String mdTable = "oracle".equalsIgnoreCase(databaseVendor) ? ".gdb_items_vw" : ".gdb_items";

		List<AbstractDatabaseColumnInfo> columns = new ArrayList<>();
		columns.add(FactoryDatabaseColumnInfo.getDatabaseColumnInfo("TYPE", "CHAR", databaseVendor));
		columns.add(FactoryDatabaseColumnInfo.getDatabaseColumnInfo("UUID", "CHAR", databaseVendor));
		columns.add(FactoryDatabaseColumnInfo.getDatabaseColumnInfo("PHYSICALNAME", "CHAR", databaseVendor));
		columns.add(FactoryDatabaseColumnInfo.getDatabaseColumnInfo("DOCUMENTATION", "CLOB", databaseVendor));
		
		return new FetchTable(
			databaseScheme + mdTable.toUpperCase(),
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
