#!/bin/bash

echo Checking ssl keys...

if [ -a $SSL_PRIVATE_KEYSTORE ]; then
	echo Private keystore found
else
	mkdir -p $(dirname $SSL_TRUSTED_KEYSTORE)

	keytool \
		-genkeypair \
		-keyalg RSA \
		-dname 'cn=IDgis Developers, ou=None, L=Rijssen, ST=Overijssel, o=IDgis bv, c=NL' \
		-keystore $SSL_PRIVATE_KEYSTORE \
		-storepass '$SSL_PRIVATE_KEYSTORE_PASSWORD' \
		-keypass '$SSL_PRIVATE_KEYSTORE_PASSWORD' \
		-alias 'private'
	echo Private keystore created
fi

if [ -a $SSL_TRUSTED_KEYSTORE ]; then
	echo Trusted keystore found
else
	mkdir -p $(dirname $SSL_TRUSTED_KEYSTORE)

	keytool \
		-genkeypair \
		-keyalg RSA \
		-dname 'cn=IDgis Developers, ou=None, L=Rijssen, ST=Overijssel, o=IDgis bv, c=NL' \
		-keystore $SSL_TRUSTED_KEYSTORE \
		-storepass '$SSL_TRUSTED_KEYSTORE_PASSWORD' \
		-keypass '$SSL_TRUSTED_KEYSTORE_PASSWORD' \
		-alias 'trusted'
	echo Trusted keystore created
fi

echo Starting service...

exec /opt/bin/publisher-service