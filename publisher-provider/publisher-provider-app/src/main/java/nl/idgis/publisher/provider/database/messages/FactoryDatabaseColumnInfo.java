package nl.idgis.publisher.provider.database.messages;

import com.typesafe.config.ConfigException;
import nl.idgis.publisher.provider.sde.SDEType;

public class FactoryDatabaseColumnInfo {

    public static AbstractDatabaseColumnInfo getDatabaseColumnInfo(String name, String typeName, SDEType type) {
        switch(type) {
            case ORACLE:
                return new OracleDatabaseColumnInfo(name, typeName);
            case POSTGRES:
                return new PostgresDatabaseColumnInfo(name, typeName);
            default:
                throw new ConfigException.BadValue("database {vendor}", "Invalid vendor supplied in config");
        }
    }
}
