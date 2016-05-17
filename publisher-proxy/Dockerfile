FROM eboraas/apache

COPY admin.conf /etc/apache2/sites-available

RUN a2enmod proxy_http && a2enmod headers && a2ensite admin