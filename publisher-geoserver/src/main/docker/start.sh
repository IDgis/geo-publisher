#!/bin/bash

if [ ! -e /var/lib/geo-publisher/geoserver/security ]; then
	echo "Generating standard geoserver password ..."
	mkdir -p /var/lib/geo-publisher/geoserver/security/
	echo "$PUBLISHER_GEOSERVER_USER=$PUBLISHER_GEOSERVER_PASSWORD,ROLE_ADMINISTRATOR" > /var/lib/geo-publisher/geoserver/security/users.properties
fi

JAVA_OPTS="$JAVA_OPTS -Dservice.identification=$SERVICE_IDENTIFICATION"
JAVA_OPTS="$JAVA_OPTS -DzooKeeper.hosts=$ZOOKEEPER_HOSTS"
if [ "$ZOOKEEPER_NAMESPACE" != "" ]; then
	JAVA_OPTS="$JAVA_OPTS -DzooKeeper.namespace=$ZOOKEEPER_NAMESPACE"
fi
JAVA_OPTS="$JAVA_OPTS -Dservice.domain=$PUBLISHER_GEOSERVER_DOMAIN"
JAVA_OPTS="$JAVA_OPTS -Dservice.ajpPort=$SERVICE_AJP_PORT"
JAVA_OPTS="$JAVA_OPTS -Dservice.httpPort=$SERVICE_HTTP_PORT"
if [ "$SERVICE_IP" != "" ]; then
	JAVA_OPTS="$JAVA_OPTS -Dservice.ip=$SERVICE_IP"
fi
if [ "$PUBLISHER_GEOSERVER_ALLOW_FROM" != "" ]; then
	JAVA_OPTS="$JAVA_OPTS -Dservice.allowFrom=$PUBLISHER_GEOSERVER_ALLOW_FROM"
fi
JAVA_OPTS="$JAVA_OPTS -Dservice.path=/$PUBLISHER_GEOSERVER_NAME"
JAVA_OPTS="$JAVA_OPTS -Dservice.forceHttps=$SERVICE_FORCE_HTTPS"
JAVA_OPTS="$JAVA_OPTS -Duser.timezone=$PUBLISHER_TIMEZONE"

# Create jdni resource
echo \
"<Context>"\
	"<Resource "\
		"name=\"jdbc/db\" "\
		"auth=\"Container\" "\
		"type=\"javax.sql.DataSource\" "\
		"driverClassName=\"org.postgresql.Driver\" "\
		"url=\"jdbc:postgresql://db:5432/publisher\" "\
		"username=\"$PG_USER\" "\
		"password=\"$PG_PASSWORD\" "\
		"defaultAutoCommit=\"false\" "\
		"rollbackOnReturn=\"true\" "\
		"testOnBorrow=\"true\" "\
		"validationInterval=\"0\" "\
	"/>"\
"</Context>" > /var/lib/tomcat7/conf/Catalina/localhost/$PUBLISHER_GEOSERVER_NAME.xml

exec /usr/share/tomcat7/bin/catalina.sh run
