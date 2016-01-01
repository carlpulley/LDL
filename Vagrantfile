# -*- mode: ruby -*-
# vi: set ft=ruby :

# This Vagrantfile is used to provision a development environment with support for the following libraries:
#   - CVC4 (see http://cvc4.cs.nyu.edu/web/)
#   - Z3 (see https://github.com/Z3Prover/z3)
#
# It can be used to run all tests within a development environment (e.g. via `cd /vagrant; sbt test`)

# If no CVC4 commit value/version is specified, we build `master` - NOTE: needs to be full commit ID!
CVC4_VERSION = ENV["CVC4_VERSION"] || "master"

# If no Z3 commit value/version is specified, we build `master` - NOTE: needs to be full commit ID!
Z3_VERSION = ENV["Z3_VERSION"] || "master"

# By default, we launch the `development` environment - specify `build` if you wish to update CVC4 and Z3 Debian packages
ENVIRONMENT = ENV["ENVIRONMENT"] || "development"

Vagrant.configure(2) do |config|
  config.vm.box = "ubuntu-vivid64"
  config.vm.box_url = "https://cloud-images.ubuntu.com/vagrant/vivid/current/vivid-server-cloudimg-amd64-vagrant-disk1.box"

  config.vm.provider "virtualbox" do |vb|
    # Customize the amount of memory on the VM:
    vb.memory = "4096"
  end

  if ENVIRONMENT == "build" then
    puts "Vagrant build environment: building Debian packages for CVC4 (commit #{CVC4_VERSION}) and Z3 (commit #{Z3_VERSION})"
    CVC4_DEBVERSION = "1.4-#{CVC4_VERSION}"
    Z3_DEBVERSION = "1.4-#{Z3_VERSION}"
    # For a `build` environment, ensure that the image is provisioned with:
    #   - JDK 8
    #   - SBT
    #   - CVC4 (with Java JNI bindings)
    #   - Z3 (with Java JNI bindings)
    #   - tools/packages necessary to build CVC4, Z3 and Debian packages
    config.vm.provision "shell", inline: <<-SHELL
      echo "deb http://dl.bintray.com/sbt/debian /" | sudo tee /etc/apt/sources.list.d/sbt.list
      sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 642AC823
      sudo apt-get -y update

      sudo apt-get install -y openjdk-7-jdk openjdk-8-jdk

      sudo apt-get install -y git autoconf libtool build-essential devscripts debhelper
      sudo apt-get install -y libgmp-dev antlr3 libantlr3c-dev libboost-dev libboost-thread-dev swig2.0 libcln-dev
      sudo apt-get install -y --force-yes sbt cxxtest

      sudo update-java-alternatives -s java-1.7.0-openjdk-amd64
      mkdir /tmp/cvc4
      cd /tmp/cvc4
      wget https://github.com/CVC4/CVC4/archive/#{CVC4_VERSION}.tar.gz
      tar -xf #{CVC4_VERSION}.tar.gz
      mv CVC4-#{CVC4_VERSION} cvc4-1.4-#{CVC4_VERSION}
      tar -czvf cvc4_#{CVC4_DEBVERSION}.orig.tar.gz cvc4-#{CVC4_DEBVERSION}
      cd cvc4-#{CVC4_DEBVERSION}
      cp -a /vagrant/debs/cvc4-debian debian
      cat > debian/changelog <<EOF
cvc4 (#{CVC4_DEBVERSION}-1) unstable; urgency=low

  * Automated Vagrant build of Debian package from Github sources.

 -- Carl Pulley <carlp@cakesolutions.net>  `date -R`
EOF
      debuild -j2 -us -uc

      cp ./builds/*/production-proof/src/bindings/CVC4.jar /vagrant/core/lib
      cp ../cvc4_*.deb /vagrant/debs
      cp ../libcvc4-3_*.deb /vagrant/debs
      cp ../libcvc4bindings-java3_*.deb /vagrant/debs
      cp ../libcvc4parser3_*.deb /vagrant/debs

      sudo update-java-alternatives -s java-1.8.0-openjdk-amd64
      mkdir /tmp/z3
      cd /tmp/z3
      wget https://github.com/Z3Prover/z3/archive/#{Z3_VERSION}.tar.gz
      tar -xf #{Z3_VERSION}.tar.gz
      mv z3-#{Z3_VERSION} z3-1.4-#{Z3_VERSION}
      tar -czvf z3_#{Z3_DEBVERSION}.orig.tar.gz z3-#{Z3_DEBVERSION}
      cd z3-#{Z3_DEBVERSION}
      cp -a /vagrant/debs/z3-debian debian
      cat > debian/changelog <<EOF
z3 (#{Z3_DEBVERSION}-1) unstable; urgency=low

  * Automated Vagrant build of Debian package from Github sources.

 -- Carl Pulley <carlp@cakesolutions.net>  `date -R`
EOF
      debuild -j2 -us -uc

      cp ./build/com.microsoft.z3.jar /vagrant/core/lib/
      cp ../z3_*.deb /vagrant/debs
      cp ../z3-java_*.deb /vagrant/debs
      cp ../python-z3_*.deb /vagrant/debs
      cp ../python3-z3_*.deb /vagrant/debs
      cp ../libz3_*.deb /vagrant/debs
      cp ../libz3-dev_*.deb /vagrant/debs

    SHELL
  else
    puts "Vagrant development environment"
    # For a `development` environment, ensure that the image is provisioned with:
    #   - JDK 8
    #   - SBT
    #   - CVC4 (with Java JNI bindings)
    #   - Z3 (with Java JNI bindings)
    #
    # NOTE: Debian packages (i.e. `cvc4`, `libcvc4bindings-java3`, `z3` and `z3-java`) in the directory `/vagrant/debs`
    # are (currently) built in the `build` environment using the Github repositories:
    #   https://github.com/CVC4/CVC4
    #   and https://github.com/Z3Prover/z3
    config.vm.provision "shell", inline: <<-SHELL
      echo "deb http://dl.bintray.com/sbt/debian /" | sudo tee /etc/apt/sources.list.d/sbt.list
      sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 642AC823
      sudo apt-get -y update

      sudo apt-get install -y openjdk-8-jdk
      sudo update-java-alternatives -s java-1.8.0-openjdk-amd64

      sudo apt-get install -y git
      sudo apt-get install -y --force-yes sbt libantlr3c-3.2-0
      sudo dpkg -R -i /vagrant/debs
    SHELL
  end
end
