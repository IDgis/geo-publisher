# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure(2) do |config|
	# Use Ubuntu 14.04 as a base:
	config.vm.box = "ubuntu/trusty64"

	# Provision using a shell script:
	config.vm.provision :shell, path: "scripts/vagrant-db.sh"
	
	# Forward the PostgreSQL port:
	config.vm.network "forwarded_port", guest: 5432, host: 5432

	# Configure VirtualBox:
  	config.vm.provider "virtualbox" do |vb|
		# Customize the amount of memory on the VM:
		vb.memory = "1024"
	end
end
