package nl.idgis.publisher.provider.sde;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

import nl.idgis.publisher.provider.database.DatabaseType;
import nl.idgis.publisher.provider.database.messages.*;
import nl.idgis.publisher.provider.protocol.Record;
import nl.idgis.publisher.provider.protocol.Records;

final class SDEUtils {

	private final DatabaseType databaseVendor;

	private final String metadataTable;

	private final String columnType;

	private final String columnUUID;

	private final String columnPhysicalName;

	private final String columnDocumentation;

	private final String columnTypeDataType;

	private final String columnUUIDDataType;

	private final String columnPhysicalNameDataType;

	private final String columnDocumentationDataType;

	SDEUtils(Config databaseConfig) throws ConfigException {



		if (databaseConfig.hasPath("vendor")) {
			try {
				this.databaseVendor = DatabaseType.valueOf(databaseConfig.getString("vendor").toUpperCase());
			} catch(IllegalArgumentException iae) {
				throw new ConfigException.BadValue("vendor", "Invalid vendor supplied in config");
			}
		} else {
			this.databaseVendor = DatabaseType.ORACLE;
		}

		if (databaseVendor == DatabaseType.POSTGRES) {
			this.metadataTable = "gdb_items";
			this.columnType = "type";
			this.columnUUID = "uuid";
			this.columnPhysicalName = "physicalname";
			this.columnDocumentation = "documentation";
			this.columnTypeDataType = "character varying(38)";
			this.columnUUIDDataType = "character varying(38)";
			this.columnPhysicalNameDataType = "character varying(257)";
			this.columnDocumentationDataType = "xml";
		} else {
			this.metadataTable = "GDB_ITEMS_VW";
			this.columnType = "TYPE";
			this.columnUUID = "UUID";
			this.columnPhysicalName = "PHYSICALNAME";
			this.columnDocumentation = "DOCUMENTATION";
			this.columnTypeDataType = "CHAR";
			this.columnUUIDDataType = "CHAR";
			this.columnPhysicalNameDataType = "CHAR";
			this.columnDocumentationDataType = "CLOB";
		}

	}

	Filter getItemsFilter() {

		return new CompoundFilter(
			"AND",
			new ColumnFilter(
					FactoryDatabaseColumnInfo.getDatabaseColumnInfo(
					columnPhysicalName,
					columnPhysicalNameDataType,
							databaseVendor),
				"IS NOT NULL"),
			new CompoundFilter(
				"OR",
				Stream.of(SDEItemInfoType.values())
					.map(itemInfoType ->
						new ColumnFilter(
								FactoryDatabaseColumnInfo.getDatabaseColumnInfo(
								columnType,
										columnTypeDataType,
										databaseVendor),
							"=", 
							itemInfoType.getUuid()))
					.toArray(length -> new ColumnFilter[length])));
	}
	
	Filter getItemsFilter(String uuid) {
		return new CompoundFilter(
			"AND",
			getItemsFilter(),
			new ColumnFilter(
				FactoryDatabaseColumnInfo.getDatabaseColumnInfo(
						columnUUID,
						columnUUIDDataType,
						databaseVendor),
					"=",
					Objects.requireNonNull(uuid, "uuid should not be null")));
	}
	
	FetchTable getFetchTable(Filter filter, String databaseScheme) {

		List<AbstractDatabaseColumnInfo> columns = new ArrayList<>();
		columns.add(FactoryDatabaseColumnInfo.getDatabaseColumnInfo(columnUUID, columnUUIDDataType, databaseVendor));
		columns.add(FactoryDatabaseColumnInfo.getDatabaseColumnInfo(columnType, columnTypeDataType, databaseVendor));
		columns.add(FactoryDatabaseColumnInfo.getDatabaseColumnInfo(columnPhysicalName, columnPhysicalNameDataType, databaseVendor));
		columns.add(FactoryDatabaseColumnInfo.getDatabaseColumnInfo(columnDocumentation, columnDocumentationDataType, databaseVendor));

		return new FetchTable(
			databaseScheme + "." + metadataTable,
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
