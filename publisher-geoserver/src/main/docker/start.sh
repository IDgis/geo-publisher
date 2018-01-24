#!/bin/bash

if [ "$(ls -A $GEOSERVER_DATA_DIR)" ]; then
	echo "GeoServer data directory already exists, keep using existing configuration..."
else
	echo "Building initial GeoServer data directory..."
	cp -R /var/lib/tomcat7/webapps/geoserver/data/* $GEOSERVER_DATA_DIR
	chown -R tomcat7:tomcat7 $GEOSERVER_DATA_DIR
	
	echo "Configuring GeoServer password..."
	mkdir -p $GEOSERVER_DATA_DIR/security/
	echo "$GEOSERVER_USER=$GEOSERVER_PASSWORD,ROLE_ADMINISTRATOR" > $GEOSERVER_DATA_DIR/security/users.properties
	
	# Disable all services
	for SERVICE_FILE in $(grep -l enabled $GEOSERVER_DATA_DIR/*.xml)
	do
		NEW_CONTENT=$(cat $SERVICE_FILE | xml2 | sed -r "s/(w?s\/enabled=).*/\1false/g" | 2xml)
		echo $NEW_CONTENT > $SERVICE_FILE
	done
	
	# Re-enable some services
	for ENABLE_SERVICE in $(echo $ENABLE_SERVICES | tr "," "\n")
	do 
	 	SERVICE_FILE=$GEOSERVER_DATA_DIR/$(echo $ENABLE_SERVICE | tr '[:upper:]' '[:lower:]').xml
	 	if [ -f $SERVICE_FILE ]; then
	 		echo Enabling $ENABLE_SERVICE service...
	 		NEW_CONTENT=$(cat $SERVICE_FILE | xml2 | sed -r "s/(w?s\/enabled=).*/\1true/g" | 2xml)
			echo $NEW_CONTENT > $SERVICE_FILE
		else
			echo Failed to enable $ENABLE_SERVICE service: no configuration file for service found
	 	fi
	done
	
	# Enable scale hints for wms services
	WMS_CONFIG_FILE=$GEOSERVER_DATA_DIR/wms.xml
	if [ -f $WMS_CONFIG_FILE ]; then
		echo Setting WMS scale hints config...
		NEW_CONTENT=$(cat $WMS_CONFIG_FILE | xml2 | sed "/wms\/verbose=false/a \/wms\/metadata\/entry" | sed "/wms\/verbose=false/a \/wms\/metadata\/entry=$ENABLE_SCALE_HINTS" | sed "/wms\/verbose=false/a \/wms\/metadata\/entry\/@key=scalehintMapunitsPixel" | 2xml)
		echo $NEW_CONTENT > $WMS_CONFIG_FILE
	else
		echo Failed to find $WMS_CONFIG_FILE
	fi
fi

if [ "$ZOOKEEPER_HOSTS" != "" ]; then
	JAVA_OPTS="$JAVA_OPTS -DzooKeeper.hosts=$ZOOKEEPER_HOSTS"
fi
if [ "$ZOOKEEPER_NAMESPACE" != "" ]; then
	JAVA_OPTS="$JAVA_OPTS -DzooKeeper.namespace=$ZOOKEEPER_NAMESPACE"
fi

JAVA_OPTS="$JAVA_OPTS -Djava.awt.headless=true"
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
