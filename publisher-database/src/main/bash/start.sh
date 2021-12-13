#!/bin/bash

sed -i 's/^#\?shared_buffers.*/shared_buffers = 2GB/g' /etc/postgresql/9.5/main/postgresql.conf
sed -i 's/^#\?max_connections.*/max_connections = 300/g' /etc/postgresql/9.5/main/postgresql.conf

set -e

echo "Starting PostgreSQL daemon ..."
/etc/init.d/postgresql start

sleep 5s

# Create the database:
RESULT=$(psql -l | grep "$PG_DATABASE" | wc -l)
if [[ ${RESULT} != 1 ]]; then
	echo "Creating database '$PG_DATABASE' with owner '$PG_USER' ..."
	
	# Create the user:
	psql -c "create user $PG_USER with password '$PG_PASSWORD';"
	psql -c "alter user $PG_USER with superuser;"
	
	# Create the database:
	createdb -O $PG_USER -E UTF8 -T template0 $PG_DATABASE
	psql -c "create extension postgis;" $PG_DATABASE
else
	echo "Database '$PG_DATABASE' exists."
fi

# Determine current PostgreSQL version:
echo "Determining current database revision ..."

TABLE=$(psql -d $PG_DATABASE -At -c "select table_name from information_schema.tables where table_schema = '$PG_VERSION_SCHEMA' and table_name = '$PG_VERSION_TABLE';")
if [[ "${TABLE}" != "$PG_VERSION_TABLE" ]]; then
	echo "Version table $PG_VERSION_SCHEMA.$PG_VERSION_TABLE doesn't exist, starting at rev000 $TABLE"
	VERSION=000
else
	VERSION_RAW=$(psql -d $PG_DATABASE -At -c "select max(id) from $PG_VERSION_SCHEMA.$PG_VERSION_TABLE")
	VERSION_START=$(($VERSION_RAW+1))
	VERSION=$(printf "%03d" $VERSION_START)

	echo "Version table $PG_VERSION_SCHEMA.$PG_VERSION_TABLE exists, starting at rev$VERSION"
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

psql -d $PG_DATABASE -v ON_ERROR_STOP=1 -f /tmp/patch.sql

echo "Stopping PostgreSQL daemon ..."
/etc/init.d/postgresql stop

# Run PostgreSQL:
echo "Starting PostgreSQL in foreground mode ..."
exec /usr/lib/postgresql/9.5/bin/postgres -D /var/lib/postgresql/9.5/main \
    -c config_file=/etc/postgresql/9.5/main/postgresql.conf
