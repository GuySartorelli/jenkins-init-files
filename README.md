This is a set of useful scripts for setting up a Jenkins installation on AWS and migrating some level of configuration from an existing server.

To check the progress of setup, run `tail -f /var/log/jenkins/jenkins.log` on the new Jenkins instance.

After the setup has completed, the following needs to be manually run on the jenkins instance:
```bash
rm -r /var/lib/jenkins/init.groovy.d
```
This will remove the below scripts, ensuring they are not re-run if the Jenkins service is restarted - which means you won't get an errouneous admin user setup in the future, or have plugins installed that you intentionally manually uninstalled.

## User data
The userdata.sh file should be passed as user data into the EC2 instance on creation. This will install jenkins and any dependencies it has, including optionally setting a port for HTTP traffic.

It will then fetch the `security.groovy` and `installPlugins.groovy` scripts from this repository and put them into `/var/lib/jenkins/init.groovy.d` to be executed when the Jenkins service starts.

Finally, it will start the Jenkins service.

## security.groovy

- Creates a default admin user (with username and password both `admin`).
- Sets a basic security strategy, which disallows access to unauthenticated users
- Sets a default crumb issuer if one has not already been set

## installPlugins.groovy

- Installs a list of plugins (see [getPluginList.groovy](getpluginlist-groovy)).
- Sets the installed status to `INITIAL_SETUP_COMPLETED` to bypass the setup wizard.