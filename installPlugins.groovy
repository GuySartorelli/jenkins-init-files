#!groovy

import jenkins.model.*
import java.util.logging.Logger

def plugins = [
  "ws-cleanup",
  "timestamper",
  "credentials-binding",
  "build-timeout",
  "antisamy-markup-formatter",
  "cloudbees-folder",
  "pipeline-stage-view",
  "pipeline-github-lib",
  "github-branch-source",
  "workflow-aggregator",
  "gradle",
  "ant",
  "mailer",
  "email-ext",
  "ldap",
  "pam-auth",
  "matrix-auth",
  "ssh-slaves",
  "github",
  "git"
]

def logger = Logger.getLogger("")
def instance = Jenkins.getInstance()
def pm = instance.getPluginManager()
def uc = instance.getUpdateCenter()

// Get updated plugin information
uc.updateAllSites()

// Install all plugins
plugins.each {
  if (!pm.getPlugin(it)) {
    logger.info("Checking UpdateCenter for " + it)
    def plugin = uc.getPlugin(it)
    if (plugin) {
      logger.info("Installing " + it)
        def installFuture = plugin.deploy()
      while(!installFuture.isDone() && !installFuture.isCancelled()) {
        sleep(3000)
      }
      def job = installFuture.get()
      if (job.getErrorMessage()) {
        logger.error(job.getErrorMessage())
      } else {
        logger.info(it + " installed.")
      }
    }
  } else {
    logger.info(it + " already installed. Skipping.")
  }
}

instance.save()
logger.info("Plugins installed.")
  
