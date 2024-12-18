FROM kartoza/geoserver:2.25.4

ENV GEOSERVER_DATA_DIR="/var/lib/geo-publisher/geoserver"
ENV GEOWEBCACHE_CACHE_DIR="/usr/local/tomcat/webapps/geoserver/data/gwc/"
ENV RASTER_DATA_DIR="/var/lib/geo-publisher/raster"
ENV CATALINA_CONTEXT="/usr/local/tomcat/conf/Catalina/localhost"

COPY /start.sh /opt/start.sh
COPY /tomcat-libs/* /usr/local/tomcat/lib

RUN chmod +x /opt/start.sh && \
	mkdir -p $GEOSERVER_DATA_DIR && \
	mkdir -p $RASTER_DATA_DIR && \
	mkdir -p $CATALINA_CONTEXT

COPY *.tif $RASTER_DATA_DIR

EXPOSE 8080

ENTRYPOINT ["/opt/start.sh"]
