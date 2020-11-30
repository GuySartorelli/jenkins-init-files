// Paste this into /script (e.g. http://jenkins.local:8080/script) to get an alphabetically sorted list of installed plugins
def plugins = new ArrayList(Jenkins.instance.pluginManager.plugins);

plugins.sort{it.getShortName()}.each {
  plugin -> 
    println (plugin.getShortName())
    //println ("${plugin.getDisplayName()} (${plugin.getShortName()}): ${plugin.getVersion()}")
}