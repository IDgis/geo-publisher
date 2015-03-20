#!/bin/bash

GEOSERVER_VERSION=2.6.1
EXHIBITOR_VERSION=1.5.5

set -e

# Install dependencies:
apt-get -qy update
apt-get -qy install \
	postgresql-9.4 \
	postgresql-client-9.4 \
	postgresql-9.4-postgis-2.1 \
	openjdk-8-jre-headless \
	tomcat7 \
	fontconfig \
	fonts-liberation \
	unzip \
	wget \
	zookeeper \
	zookeeper-bin \
	zookeeperd \
	maven
	
apt-get -qy upgrade
	
# Setup the database:
if [[ ! -e /provision-setup ]]; then
	echo "host all all 0.0.0.0/0 md5" >> /etc/postgresql/9.4/main/pg_hba.conf
	echo "listen_addresses='*'" >> /etc/postgresql/9.4/main/postgresql.conf
	
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
		echo "" >> /populate.sql
		cat $f >> /populate.sql
		echo "" >> /populate.sql
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

# Setup geoserver:
if [[ ! -e /var/lib/geoserver ]]; then
	echo "Setting up geoserver ..."

	service tomcat7 stop
		
	# Setup tomcat:
	cat > /etc/default/tomcat7 <<-EOT
		TOMCAT7_USER=tomcat7
		TOMCAT7_GROUP=tomcat7
		JAVA_OPTS="-Djava.awt.headless=true -Xmx512m -XX:MaxPermSize=256m -XX:+UseConcMarkSweepGC"
		GEOSERVER_DATA_DIR=/var/lib/geoserver
EOT
	cp /vagrant/scripts/server.xml /var/lib/tomcat7/conf/server.xml

	# Download Geoserver:
	GEOSERVER_ZIP_PATH="http://ares.boundlessgeo.com/geoserver/release/$GEOSERVER_VERSION/geoserver-$GEOSERVER_VERSION-war.zip"
	
	echo "Downloading $GEOSERVER_ZIP_PATH"
	wget -q -O /geoserver.zip "$GEOSERVER_ZIP_PATH"
	
	unzip -o -d /var/lib/tomcat7/webapps /geoserver.zip geoserver.war
	
	# Create the geoserver data dir:
	mkdir -p /var/lib/geoserver
	chown tomcat7:tomcat7 /var/lib/geoserver

	# Setup the geoserver password:	
	echo "Generating standard geoserver password ..."
	mkdir -p /var/lib/geoserver/security/
	echo "admin=geoserver,ROLE_ADMINISTRATOR" > /var/lib/geoserver/security/users.properties
	chown tomcat7:tomcat7 /var/lib/geoserver/security
	chown tomcat7:tomcat7 /var/lib/geoserver/security/users.properties
	
	# Restart tomcat:
	service tomcat7 start
fi

if [[ ! -e /opt/exhibitor ]]; then

	# Package exhibitor:
	cd /opt
	mkdir exhibitor
	cd exhibitor
	wget -q https://raw.github.com/Netflix/exhibitor/master/exhibitor-standalone/src/main/resources/buildscripts/standalone/maven/pom.xml
	mvn package
	cd target
	cp exhibitor*.jar /opt/exhibitor.jar 
	
	cat > /etc/init/exhibitor.conf <<-EOT
		description "Exhibitor"
		
		start on vagrant-mounted
		stop on runlevel [!2345]
		
		expect fork
		
		respawn
		respawn limit 0 5
		
		script
			cd /opt
			java -jar exhibitor.jar --port 8081 -c file > /var/log/exhibitor.log 2>&1
			emit exhibitor-running
		end script	
EOT

	initctl reload-configuration
	service exhibitor start

fi