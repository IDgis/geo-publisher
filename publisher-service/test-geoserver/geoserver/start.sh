#!/bin/bash

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
"</Context>" > /usr/local/tomcat/conf/Catalina/localhost/geoserver.xml

exec /usr/local/tomcat/bin/catalina.sh run
