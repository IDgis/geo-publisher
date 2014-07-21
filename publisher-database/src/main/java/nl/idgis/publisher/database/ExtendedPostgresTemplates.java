package nl.idgis.publisher.database;

import com.mysema.query.sql.PostgresTemplates;

public class ExtendedPostgresTemplates extends PostgresTemplates {

	public ExtendedPostgresTemplates() {
		setPrintSchema(true);
	}
}
