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
    if (args) {
      // support --version
      if (args[0] == '--version') {
        args[0] = 'version'
      }
    }
    
    def context = new ContextImpl(
      baseCommand: "hale")
    
    run(args as List, context)
  }

}
