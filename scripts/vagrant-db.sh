#!/bin/bash

set -e

# Install PostgreSQL:
apt-get -qy install \
	postgresql-9.3 \
	postgresql-client-9.3 \
	postgresql-9.3-postgis-2.1
	
# Setup the environment:
if [[ ! -e /provision-setup ]]; then
	echo "host all all 0.0.0.0/0 md5" >> /etc/postgresql/9.3/main/pg_hba.conf
	echo "listen_addresses='*'" >> /etc/postgresql/9.3/main/postgresql.conf
	
	sudo -u postgres psql -c "alter user postgres with password 'postgres';"
	sudo -u postgres psql -c "create user publisher with password 'publisher';"
	
	service postgresql restart
	
	# Create the database:
	sudo -u postgres dropdb --if-exists publisher
	sudo -u postgres createdb -l en_US.UTF-8 -O publisher -E UTF8 publisher
	sudo -u postgres psql -d publisher -c "create extension postgis;"
	
	touch /provision-setup
fi

# Create a database script:
echo "begin;" > /populate.sql

for f in /vagrant/publisher-database/src/main/resources/nl/idgis/publisher/database/rev*.sql; do
	if [[ "$f" > "$(cat /last-rev)" ]]; then
		cat $f >> /populate.sql
		echo $f > /last-rev
	fi
done
for f in /vagrant/publisher-database/src/main/resources/nl/idgis/publisher/database/data/rev*.sql; do
	if [[ "$f" > "$(cat /last-data-rev)" ]]; then
		cat $f >> /populate.sql
		echo $f > /last-data-rev
	fi
done

echo "commit;" >> /populate.sql

# Populate the database:
sudo -u postgres psql -d publisher -f /populate.sql