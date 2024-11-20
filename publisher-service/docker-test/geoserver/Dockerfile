FROM ubuntu:bionic

RUN apt-get update \
	&& apt-get -qy install openjdk-11-jre-headless tomcat9

ENV SERVICE_IDENTIFICATION="geoserver"
ENV SERVICE_AJP_PORT="8009"
ENV SERVICE_HTTP_PORT="8080"

ENV GEOSERVER_DATA_DIR="/var/lib/geo-publisher/geoserver"
ENV RASTER_DATA_DIR="/var/lib/geo-publisher/raster"
ENV CATALINA_BASE="/var/lib/tomcat9"
ENV CATALINA_TMPDIR="/var/lib/tomcat9/tmp"
ENV CATALINA_CONTEXT="/var/lib/tomcat9/conf/Catalina/localhost"

COPY app /var/lib/tomcat9/webapps/geoserver
COPY start.sh /opt/start.sh

# Set permissions, create data directory:
RUN chmod +x /opt/start.sh && \
	mkdir -p $CATALINA_TMPDIR && \
	mkdir -p $GEOSERVER_DATA_DIR && \
	mkdir -p $RASTER_DATA_DIR && \
	mkdir -p $CATALINA_CONTEXT

COPY *.tif $RASTER_DATA_DIR

# Expose network ports:
EXPOSE 8080
EXPOSE 8009

CMD ["/opt/start.sh"]
