#!groovy

import jenkins.model.*
import jenkins.install.InstallState
import java.util.logging.Logger
import java.net.SocketTimeoutException

def plugins = [
  "jsch",
  "ws-cleanup",
  "echarts-api",
  "structs",
  "conditional-buildstep",
  "greenballs",
  "apache-httpcomponents-client-4-api",
  "subversion",
  "parameterized-trigger",
  "pipeline-model-extensions",
  "external-monitor-job",
  "workflow-aggregator",
  "mailer",
  "git",
  "bootstrap4-api",
  "jquery3-api",
  "command-launcher",
  "workflow-api",
  "workflow-job",
  "ssh-credentials",
  "authentication-tokens",
  "email-ext-recipients-column",
  "github-branch-source",
  "htmlpublisher",
  "javadoc",
  "workflow-cps-global-lib",
  "jackson2-api",
  "ssh-slaves",
  "docker-workflow",
  "pipeline-stage-tags-metadata",
  "pipeline-milestone-step",
  "credentials",
  "github",
  "lockable-resources",
  "jquery-detached",
  "workflow-scm-step",
  "matrix-auth",
  "matrix-project",
  "pipeline-stage-step",
  "pipeline-build-step",
  "antisamy-markup-formatter",
  "pipeline-input-step",
  "ant",
  "bouncycastle-api",
  "handlebars",
  "pipeline-github-lib",
  "momentjs",
  "plain-credentials",
  "docker-commons",
  "git-client",
  "timestamper",
  "gradle",
  "pipeline-rest-api",
  "workflow-basic-steps",
  "github-api",
  "ldap",
  "maven-plugin",
  "preSCMbuildstep",
  "font-awesome-api",
  "credentials-binding",
  "pipeline-model-definition",
  "config-file-provider",
  "pipeline-stage-view",
  "PrioritySorter",
  "token-macro",
  "slack",
  "workflow-multibranch",
  "script-security",
  "extended-read-permission",
  "git-server",
  "snakeyaml-api",
  "pipeline-model-declarative-agent",
  "workflow-step-api",
  "run-condition",
  "rebuild",
  "okhttp-api",
  "pipeline-graph-analysis",
  "pipeline-model-api",
  "plugin-util-api",
  "windows-slaves",
  "workflow-cps",
  "popper-api",
  "workflow-durable-task-step",
  "email-ext",
  "trilead-api",
  "role-strategy",
  "branch-api",
  "jdk-tool",
  "cloudbees-folder",
  "durable-task",
  "junit",
  "pam-auth",
  "scm-api",
  "ace-editor",
  "display-url-api",
  "workflow-support",
  "resource-disposer",
  "build-timeout",
  "nodejs",
  "mapdb-api",
  "nvm-wrapper"
]

def logger = Logger.getLogger("")
def instance = Jenkins.getInstance()
def pm = instance.getPluginManager()
def uc = instance.getUpdateCenter()

def installPlugin(plugin, name) {
  def attempts = 0
  def success = false
  while(!success && attempts < 3) {
    try {
      attempts++
      logger.info("Installing " + name)
      def installFuture = plugin.deploy()
      while(!installFuture.isDone() && !installFuture.isCancelled()) {
        sleep(3000)
      }
      success = true
    } catch(SocketTimeoutException ex) {
      def retrying = attempts < 3 ? "Retrying." : "Too many attempts."
      logger.info("Timed out while installing " + name + ". " + retrying)
    }
  }
  return installFuture
}

// Don't show the install plugins screen
instance.setInstallState(InstallState.INITIAL_SETUP_COMPLETED)

// Get updated plugin information
uc.updateAllSites()

// Install all plugins
plugins.each {
  if (!pm.getPlugin(it)) {
    logger.info("Checking UpdateCenter for " + it)
    def plugin = uc.getPlugin(it)
    if (plugin) {
      def installFuture = installPlugin(plugin, it)
      def job = installFuture.get()
      if (job.getErrorMessage()) {
        logger.severe(job.getErrorMessage())
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
  
