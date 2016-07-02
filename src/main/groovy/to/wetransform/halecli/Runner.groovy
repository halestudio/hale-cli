package to.wetransform.halecli

import org.eclipse.equinox.nonosgi.registry.RegistryFactoryHelper;

import eu.esdihumboldt.hale.common.core.HalePlatform
import groovy.lang.GroovySystem
import to.wetransform.halecli.internal.ContextImpl
import to.wetransform.halecli.project.ProjectCommands;
import to.wetransform.halecli.transform.TransformCommand;

class Runner {
  
  final Map CLI_MODULES = [
    version: {
      // print hale version
      println HalePlatform.coreVersion
    },
    transform: new TransformCommand(),
    project: new ProjectCommands()
  ].asImmutable()

  int run(String[] args) {
    // delegate to CLI modules
    
    def run
    def commandName
    if (args) {
      commandName = args[0]
      
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
      
      if (run instanceof Command) {
        // run a command
        def context = new ContextImpl(
          baseCommand: "hale $commandName",
          commandName: commandName)
        run.run(args as List, context)
      }
      else { // assume it's a closure
        // run closure as command
        def result = run(args)
        
        if (result instanceof Number) {
          // interpret as return code
          result as int
        }
        else {
          // assume successful execution
          0
        }
      }
    } else {
      println 'usage: hale <command> [<args>]'
      println()
      println 'Supported commands are:'
      CLI_MODULES.keySet().each { command ->
        println "  $command"
      }
      1
    }
  }

}
