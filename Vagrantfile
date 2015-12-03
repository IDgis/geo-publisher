# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure(2) do |config|
	# Use Ubuntu 14.10 as a base:
	config.vm.box = "ubuntu/vivid64"

	# Provision using a shell script:
	config.vm.provision :shell, path: "scripts/vagrant-provision.sh"
	
	# Forward the PostgreSQL port:
	config.vm.network "forwarded_port", guest: 5432, host: 5432
	
	# Forward the geoserver port:
	config.vm.network "forwarded_port", guest: 8080, host: 8080
	
	# Forward the zookeeper port:
	config.vm.network "forwarded_port", guest: 2181, host: 2181
	
	# Forward the exhibitor port:
	config.vm.network "forwarded_port", guest:8081, host: 8081

	# Configure VirtualBox:
  	config.vm.provider "virtualbox" do |vb|
		# Customize the amount of memory on the VM:
		vb.memory = "1024"
	end
end
