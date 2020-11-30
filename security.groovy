#!groovy

import jenkins.model.*
import hudson.security.*
import jenkins.security.s2m.AdminWhitelistRule
import hudson.security.csrf.DefaultCrumbIssuer
import jenkins.model.Jenkins
import java.util.logging.Logger

def logger = Logger.getLogger("")
def instance = Jenkins.get()

// Create a default admin user
def hudsonRealm = new HudsonPrivateSecurityRealm(false)
hudsonRealm.createAccount("admin", "admin")
instance.setSecurityRealm(hudsonRealm)
logger.info("Default administrator account created.")

// Don't allow anonymous users to access anything
def strategy = new FullControlOnceLoggedInAuthorizationStrategy()
strategy.setAllowAnonymousRead(false)
instance.setAuthorizationStrategy(strategy)
instance.save()
logger.info("Initial security strategy set.")

//instance.getInjector().getInstance(AdminWhitelistRule.class)

if(instance.getCrumbIssuer() == null) {
    instance.setCrumbIssuer(new DefaultCrumbIssuer(true))
    instance.save()
    logger.info("CSRF Protection configuration has changed.  Enabled CSRF Protection.")
}
else {
    logger.info("CSRF Protection already configured.")
}
