#!groovy

import jenkins.model.*
import hudson.security.*
import jenkins.security.s2m.AdminWhitelistRule
import hudson.security.csrf.DefaultCrumbIssuer
import jenkins.model.Jenkins

def instance = Jenkins.get()

// Create a default admin user
def hudsonRealm = new HudsonPrivateSecurityRealm(false)
hudsonRealm.createAccount("admin", "admin")
instance.setSecurityRealm(hudsonRealm)

// Don't allow anonymous users to access anything
def strategy = new FullControlOnceLoggedInAuthorizationStrategy()
strategy.setAllowAnonymousRead(false)
instance.setAuthorizationStrategy(strategy)
instance.save()

//instance.getInjector().getInstance(AdminWhitelistRule.class)

if(instance.getCrumbIssuer() == null) {
    instance.setCrumbIssuer(new DefaultCrumbIssuer(true))
    instance.save()
    println 'CSRF Protection configuration has changed.  Enabled CSRF Protection.'
}
else {
    println 'CSRF Protection already configured.'
}
