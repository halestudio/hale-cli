package to.wetransform.halecli;

public interface CommandContext {
  
  /**
   * Get the base command line call for use in the command usage.
   * Includes the call to the executable up to selecting this command.
   * 
   * @return the base command line call
   */
  String getBaseCommand();
  
  /**
   * Get the command name.
   * 
   * @return the command name
   */
  String getCommandName();

}
