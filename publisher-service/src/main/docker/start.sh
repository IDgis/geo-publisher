#!/bin/bash

echo Checking ssl keys...

if [ -d /etc/geo-publisher/ssl ]; then
	echo Config directory exists
else
	mkdir -p /etc/geo-publisher/ssl
	echo Config directory created
fi

if [ -a /etc/geo-publisher/ssl/private.jks ]; then
	echo Private keystore found
else
	keytool \
		-genkeypair \
		-keyalg RSA \
		-dname 'cn=IDgis Developers, ou=None, L=Rijssen, ST=Overijssel, o=IDgis bv, c=NL' \
		-keystore /etc/geo-publisher/ssl/private.jks \
		-storepass 'harvester' \
		-keypass 'harvester' \
		-alias 'private'
	echo Private keystore created
fi

if [ -a /etc/geo-publisher/ssl/trusted.jks ]; then
	echo Trusted keystore found	
else
	keytool \
		-genkeypair \
		-keyalg RSA \
		-dname 'cn=IDgis Developers, ou=None, L=Rijssen, ST=Overijssel, o=IDgis bv, c=NL' \
		-keystore /etc/geo-publisher/ssl/trusted.jks \
		-storepass 'harvester' \
		-keypass 'harvester' \
		-alias 'trusted'
	echo Trusted keystore created
fi

echo Starting service...

exec /opt/bin/publisher-service