package to.wetransform.halecli;

import to.wetransform.halecli.internal.Init;

public class CLI {

  public static void main(String[] args) {
    // initialize hale in non-OSGi environment
    Init.init();
    
    Runner runner = new Runner();
    int returnCode = runner.run(args);
    
    if (returnCode != 0) {
      System.exit(returnCode);
    }
  }

}
