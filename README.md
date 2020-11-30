_important note: The paths used in these scripts assume a redhat installation_

This is a set of useful scripts for setting up a Jenkins installation on AWS and migrating some level of configuration from an existing server.

To check the progress of setup, run `tail -f /var/log/jenkins/jenkins.log` on the new Jenkins instance.

After the setup has completed, the following needs to be manually run on the jenkins instance:
```bash
rm -r /var/lib/jenkins/init.groovy.d
```
This will remove the below scripts, ensuring they are not re-run if the Jenkins service is restarted - which means you won't get an errouneous admin user setup in the future, or have plugins installed that you intentionally manually uninstalled.

## userdata.sh

In the AWS EC2 instance setup use the following command as the userdata (with SCRIPT_SOURCE set to the appropriate location - it may not necessarily be from _this_ repository):

```bash
#!/bin/bash
SCRIPT_SOURCE=https://raw.githubusercontent.com/GuySartorelli/jenkins-init-files/master
wget -O /tmp/userdata.sh $SCRIPT_SOURCE/userdata.sh
chmod 744 /tmp/userdata.sh
/tmp/userdata.sh $SCRIPT_SOURCE
```
(note: If you want to set a specific incoming http port, the last line should be `/tmp/userdata.sh $SCRIPT_SOURCE $PORT` where `$PORT` is the port number you want to set. The default will be 8080.)

This will install jenkins and any dependencies it has, including optionally setting a port for HTTP traffic.

It will then fetch the `security.groovy` and `installPlugins.groovy` scripts from this repository and put them into `/var/lib/jenkins/init.groovy.d` to be executed when the Jenkins service starts.

Finally, it will start the Jenkins service.

## security.groovy

- Creates a default admin user (with username and password both `admin`).
- Sets a basic security strategy, which disallows access to unauthenticated users
- Sets a default crumb issuer if one has not already been set

## installPlugins.groovy

- Installs a list of plugins (see `getPluginList.groovy`).
- Sets the installed status to `INITIAL_SETUP_COMPLETED` to bypass the setup wizard.

## getPluginList.groovy

As mentioned in the file itself, paste this script into `/script` (e.g. http://jenkins.local:8080/script) to get an alphabetically sorted list of installed plugins, which can be pasted into `installPlugins.groovy`.

## migrate-jobs.sh

Migrates existing jobs from one Jenkins server to another.

You may need to [set a static port for JNLP](https://wiki.jenkins.io/display/JENKINS/Jenkins+CLI#JenkinsCLI-ConfiguringTCP/IPportforCLIandagents.) and allow this port in your AWS user group for the Jenkins instance.

Dependencies:
- Admin credentials for both servers
- The CLI .jar file for both servers (downloaded from /jnlpJars/jenkins-cli.jar)
- User must have rights to write to /tmp on the machine this is executed from, as a temporary file is created there during the migration.
- Java must be installed on the machine this is executed from.

For usage, `run migrate-jobs.sh --help`

If everything has been set up and the new environment is ready to be used, the --enable_nodes argument should be included, but otherwise it should be ommitted.

Note that the script mentioned above _does not_ and _cannot_ migrate job build history or dashboard views.

Build history could be migrated with `scp` or similar, by copying the `builds` directory and `nextBuildNumber` file from each job's directory (i.e. `$JENKINS_HOME/jobs/$JOB_NAME/`).

Dashboard views can be copied from the relevant config files (`$JENKINS_HOME/config.xml` for global views and `$JENKINS_HOME/users/$USER_DIR/config.xml` for user-specific views).
