package to.wetransform.halecli

import eu.esdihumboldt.hale.common.core.HalePlatform
import eu.esdihumboldt.util.cli.Command
import eu.esdihumboldt.util.cli.CommandContext

class VersionCommand implements Command {

  @Override
  int run(List<String> args, CommandContext context) {
    // print hale version
    println HalePlatform.coreVersion
    0
  }

  final String shortDescription = 'Print the hale version'

}
