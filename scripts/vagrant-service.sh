#!/bin/bash

set -e

# Install Java:
if [[ ! -e /provision-service ]]; then
	# Install Java:
	apt-get -qy install openjdk-8-jre-headless
	
	# Create a service script:
	cat > /etc/init/publisher-service.conf <<EOT
description "Publisher service"

start on vagrant-mounted
stop on runlevel [!2345]

expect fork

respawn
respawn limit 0 5

script
	cd /vagrant/publisher-service/target
	java -jar publisher-service-0.0.1-SNAPSHOT.jar > /var/log/publisher-service.log 2>&1
	emit publisher-service-running
end script
EOT

	initctl reload-configuration
	
	# touch /provision-service
fi

service publisher-service restart