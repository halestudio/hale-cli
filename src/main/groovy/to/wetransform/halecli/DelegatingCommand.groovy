package to.wetransform.halecli

import java.util.List

abstract class DelegatingCommand implements Command {
  
  abstract Map<String, Command> getSubCommands()

  @Override
  public int run(List<String> args, CommandContext context) {
    if (args.size() == 0) {
      // usage - list sub-commands
      usage(context)
      0
    }
    else {
      // select sub-command
      
      Command command
      def commandName
      if (args) {
        commandName = args[0]
        command = subCommands[commandName]
      }
      
      if (command) {
        if (args.size() > 1) {
          args = args[1..-1]
        }
        else {
          args = []
        }
        
        // run a command
        def subContext = new DelegatingContext(context, commandName)
        command.run(args, subContext)
      }
      else {
        usage(context)
        1
      }
    }
  }
  
  void usage(CommandContext context) {
    println "usage: ${context.baseCommand} <command> [<args>]"
    println()
    println 'Supported commands are:'
    subCommands.keySet().each { command ->
      println "  $command"
    }
  }

}
