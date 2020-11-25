#!groovy

import jenkins.model.*
import jenkins.install.InstallState
import java.util.logging.Logger
import java.net.SocketTimeoutException

def logger = Logger.getLogger("")

def plugins = [
  "ace-editor",
  "ant",
  "antisamy-markup-formatter",
  "apache-httpcomponents-client-4-api",
  "authentication-tokens",
  "bootstrap4-api",
  "bouncycastle-api",
  "branch-api",
  "build-timeout",
  "cloudbees-folder",
  "command-launcher",
  "conditional-buildstep",
  "config-file-provider",
  "credentials",
  "credentials-binding",
  "display-url-api",
  "docker-commons",
  "docker-workflow",
  "durable-task",
  "echarts-api",
  "email-ext",
  "email-ext-recipients-column",
  "extended-read-permission",
  "external-monitor-job",
  "font-awesome-api",
  "git",
  "git-client",
  "git-server",
  "github",
  "github-api",
  "github-branch-source",
  "gradle",
  "greenballs",
  "handlebars",
  "htmlpublisher",
  "jackson2-api",
  "javadoc",
  "jdk-tool",
  "jquery-detached",
  "jquery3-api",
  "jsch",
  "junit",
  "ldap",
  "lockable-resources",
  "mailer",
  "mapdb-api",
  "matrix-auth",
  "matrix-project",
  "maven-plugin",
  "momentjs",
  "nodejs",
  "nvm-wrapper",
  "okhttp-api",
  "pam-auth",
  "parameterized-trigger",
  "pipeline-build-step",
  "pipeline-github-lib",
  "pipeline-graph-analysis",
  "pipeline-input-step",
  "pipeline-milestone-step",
  "pipeline-model-api",
  "pipeline-model-declarative-agent",
  "pipeline-model-definition",
  "pipeline-model-extensions",
  "pipeline-rest-api",
  "pipeline-stage-step",
  "pipeline-stage-tags-metadata",
  "pipeline-stage-view",
  "plain-credentials",
  "plugin-util-api",
  "popper-api",
  "preSCMbuildstep",
  "PrioritySorter",
  "rebuild",
  "resource-disposer",
  "role-strategy",
  "run-condition",
  "scm-api",
  "script-security",
  "slack",
  "snakeyaml-api",
  "ssh-credentials",
  "ssh-slaves",
  "structs",
  "subversion",
  "timestamper",
  "token-macro",
  "trilead-api",
  "windows-slaves",
  "workflow-aggregator",
  "workflow-api",
  "workflow-basic-steps",
  "workflow-cps",
  "workflow-cps-global-lib",
  "workflow-durable-task-step",
  "workflow-job",
  "workflow-multibranch",
  "workflow-scm-step",
  "workflow-step-api",
  "workflow-support",
  "ws-cleanup"
]

def installPlugin(plugin, name, logger) {
  def attempts = 0
  def success = false
  def installFuture = null
  while(!success && attempts < 3) {
    try {
      attempts++
      logger.info("Installing " + name)
      installFuture = plugin.deploy()
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

def instance = Jenkins.get()
def pm = instance.getPluginManager()
def uc = instance.getUpdateCenter()

// Don't show the install plugins screen
def state = instance.getInstallState()
if (state == InstallState.NEW || state == InstallState.INITIAL_PLUGINS_INSTALLING) {
  instance.setInstallState(InstallState.INITIAL_SETUP_COMPLETED)
}

// Get updated plugin information
uc.updateAllSites()

// Install all plugins
plugins.each {
  if (!pm.getPlugin(it)) {
    logger.info("Checking UpdateCenter for " + it)
    def plugin = uc.getPlugin(it)
    if (plugin) {
      def installFuture = installPlugin(plugin, it, logger)
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
  
