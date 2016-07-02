package to.wetransform.halecli

import org.eclipse.equinox.nonosgi.registry.RegistryFactoryHelper;

import eu.esdihumboldt.hale.common.core.HalePlatform
import groovy.lang.GroovySystem;
import to.wetransform.halecli.transform.TransformCLI;

class Runner {
  
  final Map CLI_MODULES = [
    version: {
      // print hale version
      println HalePlatform.coreVersion
    },
    transform: TransformCLI.&main
  ].asImmutable()

  def run(String[] args) {
    // delegate to CLI modules
    
    def run
    if (args) {
      def commandName = args[0]
      
      // support --version
      if ('--version' == commandName) {
        commandName = 'version'
      }
      
      run = CLI_MODULES[commandName]
    }
    
    if (run) {
      if (args.length > 1) {
        args = args[1..-1]
      }
      else {
        args = [] as String[]
      }
      run(args)
    } else {
      println 'usage: hale <command> [<args>]'
      println()
      println 'Supported commands are:'
      CLI_MODULES.keySet().each { command ->
        println "  $command"
      }
      System.exit(1)
    }
  }

}
