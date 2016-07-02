package to.wetransform.halecli

class DelegatingContext implements CommandContext {

  private final CommandContext delegate
  private final String subCommand
  
  public DelegatingContext(CommandContext delegate, String subCommand) {
    super()
    this.delegate = delegate
    this.subCommand = subCommand
  }

  @Override
  public String getBaseCommand() {
    delegate.baseCommand + ' ' + subCommand
  }

  @Override
  public String getCommandName() {
    subCommand
  }

}
