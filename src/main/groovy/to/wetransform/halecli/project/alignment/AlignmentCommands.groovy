package to.wetransform.halecli.project.alignment

import to.wetransform.halecli.Command
import to.wetransform.halecli.DelegatingCommand
import to.wetransform.halecli.project.commands.*

class AlignmentCommands extends DelegatingCommand {

  final Map<String, Command> subCommands = [
    'export-table': new MappingTableCommand(),
    'export-doc': new SvgDocumentationCommand(),
    'export-json': new ExportJsonCommand()
  ].asImmutable()

  final String shortDescription = 'Commands working on hale alignments'  
  
}
