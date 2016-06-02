#!/bin/bash

if [ ! -e /var/lib/geo-publisher/geoserver/security ]; then
	echo "Generating standard geoserver password ..."
	mkdir -p /var/lib/geo-publisher/geoserver/security/
	echo "$GEOSERVER_USER=$GEOSERVER_PASSWORD,ROLE_ADMINISTRATOR" > /var/lib/geo-publisher/geoserver/security/users.properties
fi

if [ "$ZOOKEEPER_HOSTS" != "" ]; then
	JAVA_OPTS="$JAVA_OPTS -DzooKeeper.hosts=$ZOOKEEPER_HOSTS"
fi
if [ "$ZOOKEEPER_NAMESPACE" != "" ]; then
	JAVA_OPTS="$JAVA_OPTS -DzooKeeper.namespace=$ZOOKEEPER_NAMESPACE"
fi

JAVA_OPTS="$JAVA_OPTS -Dservice.identification=$SERVICE_IDENTIFICATION"
JAVA_OPTS="$JAVA_OPTS -Dservice.domain=$SERVICE_DOMAIN"
JAVA_OPTS="$JAVA_OPTS -Dservice.ajpPort=$SERVICE_AJP_PORT"
JAVA_OPTS="$JAVA_OPTS -Dservice.httpPort=$SERVICE_HTTP_PORT"
JAVA_OPTS="$JAVA_OPTS -Dservice.path=/geoserver"
JAVA_OPTS="$JAVA_OPTS -Dservice.forceHttps=$SERVICE_FORCE_HTTPS"

# Create jdni resource
echo \
"<Context>"\
	"<Resource "\
		"name=\"jdbc/db\" "\
		"auth=\"Container\" "\
		"type=\"javax.sql.DataSource\" "\
		"driverClassName=\"org.postgresql.Driver\" "\
		"url=\"jdbc:postgresql://${PG_HOST}:${PG_PORT}/${PG_DBNAME}\" "\
		"username=\"$PG_USER\" "\
		"password=\"$PG_PASSWORD\" "\
		"defaultAutoCommit=\"false\" "\
		"rollbackOnReturn=\"true\" "\
		"testOnBorrow=\"true\" "\
		"validationInterval=\"0\" "\
	"/>"\
"</Context>" > /var/lib/tomcat7/conf/Catalina/localhost/geoserver.xml

exec /usr/share/tomcat7/bin/catalina.sh run
