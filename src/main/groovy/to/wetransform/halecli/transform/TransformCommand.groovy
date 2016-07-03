package to.wetransform.halecli.transform

import java.util.List;

import org.eclipse.equinox.app.IApplicationContext;
import to.wetransform.halecli.Command
import to.wetransform.halecli.CommandContext;
import eu.esdihumboldt.hale.app.transform.ExecApplication
import eu.esdihumboldt.hale.common.app.ApplicationUtil;;

class TransformCommand implements Command {

  @Override
  int run(List<String> args, CommandContext context) {
    ExecApplication app = new ExecApplication() {
      protected String getBaseCommand() {
        context.baseCommand
      }
    }
    
    def result = ApplicationUtil.launchSyncApplication(app, args as List)
    
    // interpret result as return code
    int returnCode
    try {
      returnCode = result as int
    } catch (e) {
      returnCode = 0
    }
    returnCode
  }
  
  final String shortDescription = 'Run a transformation based on a hale project'

}
