#!/bin/bash

JAVA_OPTS="$JAVA_OPTS -Djava.awt.headless=true"
JAVA_OPTS="$JAVA_OPTS -Dservice.identification=$SERVICE_IDENTIFICATION"
JAVA_OPTS="$JAVA_OPTS -Dservice.domain=$SERVICE_DOMAIN"
JAVA_OPTS="$JAVA_OPTS -Dservice.ajpPort=$SERVICE_AJP_PORT"
JAVA_OPTS="$JAVA_OPTS -Dservice.httpPort=$SERVICE_HTTP_PORT"
JAVA_OPTS="$JAVA_OPTS -Dservice.path=/geoserver"

# Create jdni resource
echo \
"<Context>"\
	"<Resource "\
		"name=\"jdbc/db\" "\
		"auth=\"Container\" "\
		"type=\"javax.sql.DataSource\" "\
		"driverClassName=\"org.postgresql.Driver\" "\
		"url=\"jdbc:postgresql://${PG_HOST}:5432/test\" "\
		"username=\"postgres\" "\
		"password=\"postgres\" "\
		"defaultAutoCommit=\"false\" "\
		"rollbackOnReturn=\"true\" "\
		"testOnBorrow=\"true\" "\
		"validationInterval=\"0\" "\
	"/>"\
"</Context>" > /var/lib/tomcat9/conf/Catalina/localhost/geoserver.xml

exec /usr/share/tomcat9/bin/catalina.sh run
