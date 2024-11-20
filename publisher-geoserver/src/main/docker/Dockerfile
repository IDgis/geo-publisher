FROM ubuntu:jammy

# Install software:
RUN apt-get update \
	&& apt-get -qy install \
		cabextract \
		fontconfig \
		fonts-liberation \
		openjdk-11-jre-headless \
		tomcat9 \
		unzip \
		wget \
		xml2

ENV SERVICE_IDENTIFICATION="geoserver"
ENV SERVICE_AJP_PORT="8009"
ENV SERVICE_HTTP_PORT="8080"
ENV SERVICE_FORCE_HTTPS="false"

ENV ENABLE_SERVICES="WMS,WFS"

ENV GEOSERVER_DATA_DIR="/var/lib/geo-publisher/geoserver"
ENV CATALINA_BASE="/var/lib/tomcat9"
ENV CATALINA_TMPDIR="/var/lib/tomcat9/tmp"
ENV CATALINA_CONTEXT="/var/lib/tomcat9/conf/Catalina/localhost"
ENV CATALINA_CONF="/etc/tomcat9"
ENV CATALINA_LOGS="/var/log/tomcat9"
ENV CATALINA_CACHE="/var/cache/tomcat9"

# Copy webapps, jars, start script and tomcat configuration:
COPY /webapps/geoserver /var/lib/tomcat9/webapps/geoserver
COPY /geowebcache_empty.xml /var/lib/tomcat9/webapps/geoserver/data/gwc/geowebcache.xml
COPY /webapps/provisioning-registration-war /var/lib/tomcat9/webapps/provisioning-registration-war
COPY /lib/* /var/lib/tomcat9/lib
COPY /start.sh /opt/start.sh
COPY /server.xml /var/lib/tomcat9/conf/server.xml

# Set permissions, create data directory:
RUN chmod +x /opt/start.sh && \
	mkdir -p $CATALINA_TMPDIR && \
	mkdir -p $GEOSERVER_DATA_DIR && \
	mkdir -p $CATALINA_CONTEXT && \
	chown tomcat:tomcat $GEOSERVER_DATA_DIR && \
	chown -R tomcat:tomcat $CATALINA_CONF && \
	chown -R tomcat:tomcat $CATALINA_LOGS && \
	chown -R tomcat:tomcat $CATALINA_CACHE && \
	chown -R tomcat:tomcat /var/lib/tomcat9/
	
# Download and install the core fonts:
ENV MSCOREFONTS_BASE="http://netcologne.dl.sourceforge.net/project/corefonts/the%20fonts/final/"
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
	
# Run the command as the tomcat user:
USER tomcat

# Expose network ports:
EXPOSE 8080
EXPOSE 8009

CMD ["/opt/start.sh"]
