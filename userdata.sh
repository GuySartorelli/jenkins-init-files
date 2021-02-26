#!/bin/bash

SCRIPT_SOURCE=$1
HTTP_PORT=$2

# Install java
yum upgrade -y
yum install java-1.8.0-openjdk-devel -y
# set JAVA_HOME env variable and add java to path
echo "export JAVA_HOME=$(dirname $(dirname $(readlink $(readlink $(which java)))))" >> /etc/profile
echo "export JRE_HOME=$JAVA_HOME" >> /etc/profile && source /etc/profile

# Install job dependencies
# install git, unzip
yum install git unzip -y
# install nvm, npm, node, nativescript, angular
wget -qO- https://raw.githubusercontent.com/nvm-sh/nvm/v0.37.2/install.sh | bash
export NVM_DIR="$HOME/.nvm"
[ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"  # This loads nvm
[ -s "$NVM_DIR/bash_completion" ] && \. "$NVM_DIR/bash_completion"  # This loads nvm bash_completion
nvm install 12
npm install -g --silent node@12
npm install -g --silent nativescript@6.3.1
npm install -g @angular/cli@9
# install gradle
wget https://services.gradle.org/distributions/gradle-6.3-bin.zip -P /tmp
unzip -d /opt/gradle /tmp/gradle-*.zip
rm -r /tmp/gradle-*.zip
printf "export GRADLE_HOME=/opt/gradle/gradle-6.3\nexport PATH=\x24{GRADLE_HOME}/bin:\x24{PATH}" >> /etc/profile && source /etc/profile
# install elastic beanstalk
yum group install "Development Tools" -y
yum install \
    zlib-devel openssl-devel ncurses-devel libffi-devel \
    sqlite-devel.x86_64 readline-devel.x86_64 bzip2-devel.x86_64 -y
git clone https://github.com/aws/aws-elastic-beanstalk-cli-setup.git /tmp/elastic-beanstalk
/tmp/elastic-beanstalk/scripts/bundled_installer
rm -r /tmp/elastic-beanstalk
echo 'export PATH="~/.ebcli-virtual-env/executables:$PATH"' >> /etc/profile && source /etc/profile
# install andriod studio
wget https://dl.google.com/android/repository/commandlinetools-linux-6858069_latest.zip -P /tmp/android-studio
unzip -d /opt/android-sdk /tmp/android-studio/*.zip
rm -r /tmp/android-studio
echo "export PATH=/opt/android-sdk/cmdline-tools/bin:\$PATH" >> /etc/profile
# install gem and cocoapods via ruby
wget https://github.com/postmodern/ruby-install/archive/v0.8.1.tar.gz -P /tmp/ruby-install
tar -xf /tmp/ruby-install/*.tar.gz -C /tmp/ruby-install/
/tmp/ruby-install/ruby-install-0.8.1/bin/ruby-install ruby 3
echo 'export PATH="~/.rubies/ruby-3.0.0/bin:$PATH"' >> /etc/profile && source /etc/profile
gem install cocoapods
sudo rm -r /tmp/ruby-install/

#install jenkins
wget -O /etc/yum.repos.d/jenkins.repo \
    https://pkg.jenkins.io/redhat-stable/jenkins.repo
rpm --import https://pkg.jenkins.io/redhat-stable/jenkins.io.key
yum upgrade -y
yum install jenkins -y

# Disable jenkins setup wizard
old_java_options='JENKINS_JAVA_OPTIONS="-Djava.awt.headless=true'
new_java_options="$old_java_options -Djenkins.install.runSetupWizard=false"
sed -i "s/$old_java_options/$new_java_options/" /etc/sysconfig/jenkins
# Set port if passed in
if [[ ! -z $HTTP_PORT ]]; then
  if [[ $HTTP_PORT = 80 ]]; then
    iptables -A PREROUTING -t nat -i eth0 -p tcp --dport 80 -j REDIRECT --to-port 8080
  else
    sed -i "s/JENKINS_PORT=\"8080\"/JENKINS_PORT=\"$HTTP_PORT\"/" /etc/sysconfig/jenkins
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