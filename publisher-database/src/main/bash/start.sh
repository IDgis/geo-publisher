#!/bin/bash

set -eu

docker-entrypoint.sh postgres &

until psql -U $POSTGRES_USER -d $POSTGRES_DATABASE -c '\q' > /dev/null 2>&1; do
    echo "Waiting for database to exist..."
    sleep 1
done

# Determine current PostgreSQL version:
echo "Determining current database revision ..."

TABLE=$(psql -U $POSTGRES_USER -d $POSTGRES_DATABASE -At -c "select table_name from information_schema.tables where table_schema = '$POSTGRES_VERSION_SCHEMA' and table_name = '$POSTGRES_VERSION_TABLE';")
if [[ "${TABLE}" != "$POSTGRES_VERSION_TABLE" ]]; then
	echo "Version table $POSTGRES_VERSION_SCHEMA.$POSTGRES_VERSION_TABLE doesn't exist, starting at rev000 $TABLE"
	VERSION=000
else
	VERSION_RAW=$(psql -U $POSTGRES_USER -d $POSTGRES_DATABASE -At -c "select max(id) from $POSTGRES_VERSION_SCHEMA.$POSTGRES_VERSION_TABLE")
	VERSION_START=$(($VERSION_RAW+1))
	VERSION=$(printf "%03d" $VERSION_START)

	echo "Version table $POSTGRES_VERSION_SCHEMA.$POSTGRES_VERSION_TABLE exists, starting at rev$VERSION"
fi


# Patch the database:
echo "Patching the database ..."
echo "begin;" > /tmp/patch.sql
for file in /opt/sql/rev*.sql; do
	if [ "$file" \<  "/opt/sql/rev$VERSION.sql" ]; then
		echo "Skipping file $file, revision already processed."
	else
		echo "Processing file $file ..."
		
		echo "" >> /tmp/patch.sql
		echo "-- Source: $file" >> /tmp/patch.sql
		echo "" >> /tmp/patch.sql
		cat $file >> /tmp/patch.sql
		echo "" >> /tmp/patch.sql
		echo "-- End source: $file" >> /tmp/patch.sql
		echo "" >> /tmp/patch.sql
	fi
done
echo "commit;" >> /tmp/patch.sql

psql -U $POSTGRES_USER -d $POSTGRES_DATABASE -v ON_ERROR_STOP=1 -f /tmp/patch.sql

sed -i "s/^#*max_connections.*/max_connections = 200/" "$PGDATA/postgresql.conf"
sed -i "s/^#*shared_buffers.*/shared_buffers = 2GB/" "$PGDATA/postgresql.conf"

pg_ctl -D "$PGDATA" -m fast stop

exec docker-entrypoint.sh postgres
