FROM ubuntu:xenial

# Install software:
RUN apt-get update \
	&& apt-get -qy install \
		cabextract \
		fontconfig \
		fonts-liberation \
		openjdk-8-jre-headless \
		tomcat7 \
		unzip \
		wget

ENV SERVICE_IDENTIFICATION="geoserver"
ENV SERVICE_AJP_PORT="8009"
ENV SERVICE_HTTP_PORT="8080"
ENV SERVICE_FORCE_HTTPS="false"		

ENV GEOSERVER_DATA_DIR="/var/lib/geo-publisher/geoserver"
ENV CATALINA_BASE="/var/lib/tomcat7"
ENV CATALINA_HOME="/usr/share/tomcat7"
ENV CATALINA_TMPDIR="/var/lib/tomcat7/tmp"
ENV PATH="$CATALINA_HOME/bin:$PATH"
ENV JAVA_OPTS="-Djava.awt.headless=true -Xmx512m -XX:MaxPermSize=256m"

# Copy webapps, jars, start script and tomcat configuration:
COPY /webapps/geoserver /var/lib/tomcat7/webapps/geoserver
COPY /webapps/provisioning-registration-war /var/lib/tomcat7/webapps/provisioning-registration-war
COPY /lib/* /usr/share/tomcat7/lib
COPY /start.sh /opt/start.sh
COPY /server.xml /var/lib/tomcat7/conf/server.xml

# Set permissions, create data directory
RUN chmod +x /opt/start.sh && \
	mkdir -p $CATALINA_TMPDIR && \
	chown tomcat7:tomcat7 $CATALINA_TMPDIR && \ 
	mkdir -p $GEOSERVER_DATA_DIR && \
	chown tomcat7:tomcat7 $GEOSERVER_DATA_DIR && \
	mkdir -p $GEOSERVER_DATA_DIR/styles && \
	chown tomcat7:tomcat7 $GEOSERVER_DATA_DIR/styles && \
	chown -R tomcat7:tomcat7 /var/lib/tomcat7/webapps/
	
# Download and install the core fonts:
ENV MSCOREFONTS_BASE="http://kent.dl.sourceforge.net/project/corefonts/the%20fonts/final/"
RUN wget -q -O /tmp/andale32.exe "${MSCOREFONTS_BASE}andale32.exe" && \
	wget -q -O /tmp/arial32.exe "${MSCOREFONTS_BASE}arial32.exe" && \
	wget -q -O /tmp/arialb32.exe "${MSCOREFONTS_BASE}arialb32.exe" && \
	wget -q -O /tmp/comic32.exe "${MSCOREFONTS_BASE}comic32.exe" && \
	wget -q -O /tmp/courie32.exe "${MSCOREFONTS_BASE}courie32.exe" && \
	wget -q -O /tmp/georgi32.exe "${MSCOREFONTS_BASE}georgi32.exe" && \
	wget -q -O /tmp/impact32.exe "${MSCOREFONTS_BASE}impact32.exe" && \
	wget -q -O /tmp/times32.exe "${MSCOREFONTS_BASE}times32.exe" && \
	wget -q -O /tmp/trebuc32.exe "${MSCOREFONTS_BASE}trebuc32.exe" && \
	wget -q -O /tmp/webdin32.exe "${MSCOREFONTS_BASE}webdin32.exe" && \
	wget -q -O /tmp/verdan32.exe "${MSCOREFONTS_BASE}verdan32.exe" && \
	mkdir /tmp/mscorefonts && \
	cabextract -L -d /tmp/mscorefonts /tmp/*.exe && \
	mkdir /usr/share/fonts/truetype/mscorefonts && \
	cp /tmp/mscorefonts/*.ttf /usr/share/fonts/truetype/mscorefonts && \
	fc-cache -fv && \
	fc-list
	
# Run the command as the geoserver user:
USER tomcat7

# Expose network ports:
EXPOSE 8080
EXPOSE 8009

CMD ["/opt/start.sh"]
