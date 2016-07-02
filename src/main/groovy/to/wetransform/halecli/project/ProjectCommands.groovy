package to.wetransform.halecli.project

import to.wetransform.halecli.Command
import to.wetransform.halecli.DelegatingCommand

class ProjectCommands extends DelegatingCommand {

  final Map<String, Command> subCommands = [:]

}
