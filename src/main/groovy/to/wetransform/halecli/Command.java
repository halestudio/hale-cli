package to.wetransform.halecli;

import java.util.List;

import javax.annotation.Nullable;

public interface Command {
  
  /**
   * Run the command.
   * 
   * @param args the list of arguments
   * @return
   */
  int run(List<String> args, CommandContext context);
  
  /**
   * Get a short description describing the command.
   * 
   * @return a short command description
   */
  @Nullable
  String getShortDescription();
  
  /**
   * Return a Unix command to use to generate bash completions.
   * 
   * @param args the list of arguments (ending with the proposal to complete)
   * @return the Unix command or <code>null</code>
   */
  @Nullable
  default String bashCompletion(List<String> args) {
    return null;
  }

}
