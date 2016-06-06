#!/bin/bash

echo $PG_HOST:$PG_PORT:$PG_DBNAME:$PG_USER:$PG_PASSWORD > ~/.pgpass
chmod 0600 ~/.pgpass

exec_sql() {
	psql -q -h $PG_HOST -p $PG_PORT -U $PG_USER -c "$*" $PG_DBNAME 
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

echo initializing database...

while ! exec_sql select 1 > /dev/null 2>&1; do
	echo database not yet available
	sleep 1
done

echo database available

ensure_environment geoserver-public "Public services" false "http://${DOMAIN_PREFIX}services${DOMAIN_SUFFIX}/geoserver/"
ensure_environment geoserver-secure "Secure services" true "http://${DOMAIN_PREFIX}secure-services${DOMAIN_SUFFIX}/geoserver/"

ensure_datasource overijssel-gisbasip "Overijssel vectordata"
ensure_datasource overijssel-raster "Overijssel rasterdata"
