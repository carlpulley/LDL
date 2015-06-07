# -*- mode: ruby -*-
# vi: set ft=ruby :

# This Vagrantfile is used to provision a development environment with support for the following libraries:
#   - CVC4 (see http://cvc4.cs.nyu.edu/web/)
#
# It can be used to run all tests within a development environment (e.g. via `cd /vagrant; sbt test`)

# If no CVC4 commit value/version is specified, we build `master` - NOTE: needs to be full commit ID!
VERSION = ENV["VERSION"] || "master"

# By default, we launch the `development` environment - specify `build` if you wish to update CVC4 Debian packages
ENVIRONMENT = ENV["ENVIRONMENT"] || "development"

Vagrant.configure(2) do |config|
  config.vm.box = "ubuntu-trusty64"
  config.vm.box_url = "https://cloud-images.ubuntu.com/vagrant/trusty/current/trusty-server-cloudimg-amd64-vagrant-disk1.box"

  config.vm.provider "virtualbox" do |vb|
    # Customize the amount of memory on the VM:
    vb.memory = "4096"
  end

  if ENVIRONMENT == "build" then
    puts "Vagrant build environment: building Debian CVC4 packages for commit #{VERSION}"
    DEBVERSION = "1.4-#{VERSION}"
    # For a `build` environment, ensure that the image is provisioned with:
    #   - JDK 7 (current CVC4 JNI binding requirement)
    #   - SBT
    #   - CVC4 (with Java JNI bindings)
    #   - tools/packages necessary to build CVC4 and Debian packages
    config.vm.provision "shell", inline: <<-SHELL
      echo "deb http://dl.bintray.com/sbt/debian /" | sudo tee /etc/apt/sources.list.d/sbt.list
      sudo apt-get -y update
      sudo apt-get install -y git autoconf libtool build-essential devscripts debhelper
      sudo apt-get install -y libgmp-dev antlr3 libantlr3c-dev libboost-dev libboost-thread-dev swig2.0 libcln-dev openjdk-7-jdk
      sudo apt-get install -y --force-yes sbt cxxtest

      mkdir /tmp/cvc4
      cd /tmp/cvc4
      wget https://github.com/CVC4/CVC4/archive/#{VERSION}.tar.gz
      tar -xf #{VERSION}.tar.gz
      mv CVC4-#{VERSION} cvc4-1.4-#{VERSION}
      tar -czvf cvc4_#{DEBVERSION}.orig.tar.gz cvc4-#{DEBVERSION}
      cd cvc4-#{DEBVERSION}
      cp -a /vagrant/debs/cvc4-debian debian
      cat > debian/changelog <<EOF
cvc4 (#{DEBVERSION}-1) unstable; urgency=low

  * Automated Vagrant build of Debian package from Github sources.

 -- Carl Pulley <carlp@cakesolutions.net>  `date -R`
EOF
      debuild -j2 -us -uc

      cp ./builds/*/production-proof/src/bindings/CVC4.jar /vagrant/lib
      cp ../cvc4_*.deb /vagrant/debs
      cp ../libcvc4-3_*.deb /vagrant/debs
      cp ../libcvc4bindings-java3_*.deb /vagrant/debs
      cp ../libcvc4parser3_*.deb /vagrant/debs
    SHELL
  else
    puts "Vagrant development environment"
    # For a `development` environment, ensure that the image is provisioned with:
    #   - JDK 7 (current CVC4 JNI binding requirement)
    #   - SBT
    #   - CVC4 (with Java JNI bindings)
    #
    # NOTE: Debian packages (i.e. `cvc4` and `libcvc4bindings-java3`) in the directory `/vagrant/debs` are (currently)
    # built in the `build` environment using the Github repository:
    #   https://github.com/CVC4/CVC4
    config.vm.provision "shell", inline: <<-SHELL
      echo "deb http://dl.bintray.com/sbt/debian /" | sudo tee /etc/apt/sources.list.d/sbt.list
      sudo apt-get -y update
      sudo apt-get install -y git
      sudo apt-get install -y openjdk-7-jdk
      sudo apt-get install -y --force-yes sbt libantlr3c-3.2-0
      sudo dpkg -R -i /vagrant/debs
    SHELL
  end
end
