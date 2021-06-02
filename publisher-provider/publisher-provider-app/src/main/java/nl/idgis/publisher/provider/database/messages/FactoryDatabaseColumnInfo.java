package nl.idgis.publisher.provider.database.messages;

import com.typesafe.config.ConfigException;
import nl.idgis.publisher.provider.database.DatabaseType;

public class FactoryDatabaseColumnInfo {

    public static AbstractDatabaseColumnInfo getDatabaseColumnInfo(String name, String typeName, DatabaseType type) {
        switch(type) {
            case ORACLE:
                return new OracleDatabaseColumnInfo(name, typeName);
            case POSTGRES:
                return new PostgresDatabaseColumnInfo(name, typeName);
            default:
                throw new ConfigException.BadValue("database {vendor}", "Unsupported vendor supplied in config");
        }
    }
}
