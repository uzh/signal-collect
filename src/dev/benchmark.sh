#!/bin/bash

working_dir=`mktemp -d`
cd $working_dir

# checkout Signal/Collect
svn checkout http://signal-collect.googlecode.com/svn/trunk/core core

# install maven
wget http://mirror.switch.ch/mirror/apache/dist//maven/binaries/apache-maven-2.2.1-bin.tar.gz
tar xzfv apache-maven-2.2.1-bin.tar.gz
export PATH=$working_dir/apache-maven-2.2.1/bin:$PATH

# build Signal/Collect
cd core
MAVEN_OPTS="-Xms256m -Xmx2048m"
mvn clean package

# remove temporary directory
cd ~
rm -rdf $working_dir