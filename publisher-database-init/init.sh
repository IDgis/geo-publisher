#!/bin/bash

echo $PG_HOST:$PG_PORT:$PG_DBNAME:$PG_USER:$PG_PASSWORD > ~/.pgpass
chmod 0600 ~/.pgpass

exec_sql() {
	psql -h $PG_HOST -p $PG_PORT -U $PG_USER -c "$*" $PG_DBNAME 
}

ensure_environment() {
	echo ensuring environment: $*
	exec_sql insert into publisher.environment\(identification, name, confidential, url\) \
		values \(\'$1\', \'$2\', $3, \'$4\'\) \
		on conflict\(identification\) do update \
		set name = excluded.name, \
			confidential = excluded.confidential, \
			url = excluded.url
}

ensure_datasource() {
	echo ensuring datasource: $*
	exec_sql insert into publisher.data_source\(identification, name\) \
		values\(\'$1\', \'$2\'\) \
		on conflict\(identification\) do update \
		set name = excluded.name
}

ensure_environment geoserver-public "Publieke services" false "http://${DOMAIN_PREFIX}services${DOMAIN_SUFFIX}/geoserver/"
ensure_datasource overijssel-gisbasip "Overijssel vectordata"
ensure_datasource overijssel-raster "Overijssel rasterdata"
