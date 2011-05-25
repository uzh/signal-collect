#!/bin/bash

working_dir=`mktemp -d`
cd $working_dir

# checkout Signal/Collect
svn checkout --quiet http://signal-collect.googlecode.com/svn/trunk/core core

# install maven
wget --quiet http://mirror.switch.ch/mirror/apache/dist//maven/binaries/apache-maven-2.2.1-bin.tar.gz
tar xzf apache-maven-2.2.1-bin.tar.gz
export PATH=$working_dir/apache-maven-2.2.1/bin:$PATH

# build Signal/Collect
cd core
export MAVEN_OPTS="-Xms256m -Xmx2048m"
export JAVA_HOME=/usr/lib/jvm/java-6-sun
mvn --quiet -Dmaven.test.skip=true clean package

# run benchmark
java -Xmx50000m -Xms50000m -cp target/core-0.0.1-SNAPSHOT-jar-with-dependencies.jar signalcollect.benchmark.Benchmark

# remove temporary directory
cd ~
rm -rdf $working_dir