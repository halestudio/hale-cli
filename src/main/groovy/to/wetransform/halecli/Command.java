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
   * @param args the list of arguments
   * @param current the index of the current argument to be completed
   * @return the Unix command or <code>null</code>
   */
  @Nullable
  default String bashCompletion(List<String> args, int current) {
    return null;
  }

}
