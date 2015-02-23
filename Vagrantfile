# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure(2) do |config|
	config.vm.define "db", autostart: false do |db|
		# Use Ubuntu 14.10 as a base:
		db.vm.box = "ubuntu/utopic64"
	
		# Provision using a shell script:
		db.vm.provision :shell, path: "scripts/vagrant-db.sh"
		
		# Forward the PostgreSQL port:
		db.vm.network "forwarded_port", guest: 5432, host: 5432
	
		# Configure VirtualBox:
	  	db.vm.provider "virtualbox" do |vb|
			# Customize the amount of memory on the VM:
			vb.memory = "1024"
		end
	end
	
	config.vm.define "service", autostart: false do |service|
		# Use Ubuntu 14.10 as a base:
		service.vm.box = "ubuntu/utopic64"
	
		# Provision using a shell script:
		service.vm.provision :shell, path: "scripts/vagrant-db.sh"
		service.vm.provision :shell, path: "scripts/vagrant-service.sh"
		
		# Create a public network so that the service can connect to the local machine:
		service.vm.network "public_network"
		
		# Forward the PostgreSQL port:
		service.vm.network "forwarded_port", guest: 5432, host: 5432
		service.vm.network "forwarded_port", guest: 2552, host: 2552
	end
end
