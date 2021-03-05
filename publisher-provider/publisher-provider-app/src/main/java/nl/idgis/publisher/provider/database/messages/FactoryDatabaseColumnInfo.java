package nl.idgis.publisher.provider.database.messages;

public class FactoryDatabaseColumnInfo {

    public static AbstractDatabaseColumnInfo getDatabaseColumnInfo(String name, String typeName, String type) {
        switch(type) {
            //case "SDE":
            //    return new SDEDatabaseColumnInfo(name, typeName);
            case "oracle":
                return new OracleDatabaseColumnInfo(name, typeName);
            case "postgres":
                return new PostgresDatabaseColumnInfo(name, typeName);
        }
        return null;
    }
}
