package to.wetransform.halecli;

import java.util.List;

public interface Command {
  
  /**
   * Run the command.
   * 
   * @param args the list or arguments
   * @return
   */
  int run(List<String> args, CommandContext context);

}
