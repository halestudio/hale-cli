package to.wetransform.halecli

import java.util.List

import eu.esdihumboldt.hale.common.core.HalePlatform

class VersionCommand implements Command {

  @Override
  int run(List<String> args, CommandContext context) {
    // print hale version
    println HalePlatform.coreVersion
    0
  }

  final String shortDescription = 'Print the hale version'

}
