package to.wetransform.halecli

import to.wetransform.halecli.transform.TransformCLI;

class CLI {
  
  static Map CLI_MODULES = [
    transform: TransformCLI.&main
  ].asImmutable()

  static main(args) {
    // delegate to CLI modules
    
    def run
    if (args) {
      run = CLI_MODULES[args[0]]
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
