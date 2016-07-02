package to.wetransform.halecli

import eu.esdihumboldt.hale.common.core.HalePlatform;
import to.wetransform.halecli.transform.TransformCLI;

class CLI {
  
  static Map CLI_MODULES = [
    version: {
      // print hale version
      println HalePlatform.coreVersion
    },
    transform: TransformCLI.&main
  ].asImmutable()

  static main(args) {
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
    }
  }

}
