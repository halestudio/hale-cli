package to.wetransform.halecli.project

import to.wetransform.halecli.Command
import to.wetransform.halecli.DelegatingCommand
import to.wetransform.halecli.project.commands.*

class ProjectCommands extends DelegatingCommand {

  final Map<String, Command> subCommands = [
    'mapping-table': new MappingTableCommand()
  ].asImmutable()

  final String shortDescription = 'Various utility commands working on hale projects'  
  
}
