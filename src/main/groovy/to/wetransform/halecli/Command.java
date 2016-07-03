package to.wetransform.halecli;

import java.util.List;

import javax.annotation.Nullable;

public interface Command {
  
  /**
   * Run the command.
   * 
   * @param args the list or arguments
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

}
