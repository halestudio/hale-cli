package to.wetransform.halecli

import java.util.Map;

import to.wetransform.halecli.internal.ContextImpl
import to.wetransform.halecli.project.ProjectCommands
import to.wetransform.halecli.transform.TransformCommand

class Runner extends DelegatingCommand {
  
  final Map<String, Command> subCommands = [
    version: new VersionCommand(),
    transform: new TransformCommand(),
    project: new ProjectCommands()
  ].asImmutable()

  final String shortDescription = 'hale command line utility'

  int run(String[] args) {
    def context = new ContextImpl(
      baseCommand: "hale")
    
    if (args) {
      // support --version
      if (args[0] == '--version') {
        args[0] = 'version'
      }
      
      // helper for bash completion
      if (args[0] == '--complete') {
        // next arg must be index of word to complete
        int currentWord
        try {
          currentWord = args[1] as int
        } catch (e) {
          return 1
        }
        
        // determine list of words (strip "--complete <index>")
        def words = args.length > 2 ? args[2..-1] as List : []
        
        // if current word is not included, add empty string as argument
        if (words.size() == currentWord) {
          words << ''
        }
        
        // strip first word (which is the base command)
        words = words.size() > 1 ? words[1..-1] : []
        
        String completion = bashCompletion(words)
        if (completion) {
          println completion
          return 0
        }
        else {
          return 1
        }
      }
    }
    
    run(args as List, context)
  }

}
