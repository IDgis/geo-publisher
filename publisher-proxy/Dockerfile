FROM eboraas/apache

COPY *.conf /etc/apache2/sites-available/

RUN a2enmod proxy_http headers \
	&& a2ensite admin services staging-services secure-services guaranteed-services