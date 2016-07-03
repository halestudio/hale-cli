package to.wetransform.halecli

import java.util.List

abstract class DelegatingCommand implements Command {
  
  abstract Map<String, Command> getSubCommands()

  @Override
  public int run(List<String> args, CommandContext context) {
    if (args.size() == 0) {
      // usage - list sub-commands
      Util.printUsage(context, subCommands)
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
        Util.printUsage(context, subCommands)
        1
      }
    }
  }
  
  String bashCompletion(List<String> args) {
    if (args.size() > 1) {
      // delegate to command
      String commandName = args[0]
      Command command = subCommands[commandName]
      if (command) {
        command.bashCompletion(args[1..-1])
      }
      else {
        null
      }
    }
    else {
      // complete subcommand
      'compgen -W "' + subCommands.keySet().join(' ') + '" -- ' + args[-1]
    }
  }

}
