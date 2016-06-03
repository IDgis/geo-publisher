FROM ubuntu:xenial

RUN apt-get update && apt-get -qy install postgresql-client-9.5

COPY init.sh /opt/

RUN chmod +x /opt/init.sh

CMD ["/opt/init.sh"]