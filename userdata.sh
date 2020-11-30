#!/bin/bash

SCRIPT_SOURCE=$1
HTTP_PORT=$2

# Install jenkins
wget -O /etc/yum.repos.d/jenkins.repo \
    https://pkg.jenkins.io/redhat-stable/jenkins.repo
rpm --import https://pkg.jenkins.io/redhat-stable/jenkins.io.key
yum upgrade -y
yum install jenkins java-1.8.0-openjdk-devel -y

# Disable setup wizard
old_java_options='JENKINS_JAVA_OPTIONS="-Djava.awt.headless=true'
new_java_options='$old_java_options -Djenkins.install.runSetupWizard=false'
sed -i 's/$old_java_options/$new_java_options/' /etc/sysconfig/jenkins
# Set port if passed in
if [[ ! -z $HTTP_PORT ]]; then
  if [[ $HTTP_PORT = 80 ]]; then
    iptables -A PREROUTING -t nat -i eth0 -p tcp --dport 80 -j REDIRECT --to-port 8080
  else
    sed -i 's/JENKINS_PORT="8080"/JENKINS_PORT="$HTTP_PORT"/' /etc/sysconfig/jenkins
  fi
fi

# Get groovy scripts
groovydir=/var/lib/jenkins/init.groovy.d
mkdir $groovydir
wget -O $groovydir/installPlugins.groovy $SCRIPT_SOURCE/installPlugins.groovy
wget -O $groovydir/security.groovy $SCRIPT_SOURCE/security.groovy
chown -R jenkins:jenkins $groovydir
chmod -R 755 $groovydir

# Start jenkins, which will use the above scripts to perform its initial setup
systemctl daemon-reload
service jenkins start