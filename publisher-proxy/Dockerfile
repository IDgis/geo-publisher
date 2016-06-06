FROM ubuntu:xenial

RUN \
	apt-get update \ 
	&& apt-get -qy install apache2

COPY *.conf /etc/apache2/sites-available/

RUN \
	a2enmod \
		proxy_http \
		headers \
	&& a2ensite \
		admin \
		services \
		staging-services \
		metadata
	
CMD ["/usr/sbin/apache2ctl", "-D", "FOREGROUND"]