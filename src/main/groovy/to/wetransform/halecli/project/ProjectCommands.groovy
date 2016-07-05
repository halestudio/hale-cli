package to.wetransform.halecli.project

import to.wetransform.halecli.Command
import to.wetransform.halecli.DelegatingCommand
import to.wetransform.halecli.project.alignment.AlignmentCommands;
import to.wetransform.halecli.project.commands.*

class ProjectCommands extends DelegatingCommand {

  final Map<String, Command> subCommands = [
    'alignment': new AlignmentCommands(),
    'list': new ListProjectsCommand()
  ].asImmutable()

  final String shortDescription = 'Various utility commands working on hale projects'  
  
}
