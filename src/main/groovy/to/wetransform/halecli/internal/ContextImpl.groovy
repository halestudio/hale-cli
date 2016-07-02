package to.wetransform.halecli.internal

import groovy.transform.Immutable;
import to.wetransform.halecli.CommandContext

@Immutable
class ContextImpl implements CommandContext {

  String baseCommand
  String commandName

}
