package to.wetransform.halecli.transform

import org.eclipse.equinox.app.IApplicationContext;

import eu.esdihumboldt.hale.app.transform.ExecApplication
import eu.esdihumboldt.hale.common.app.ApplicationUtil;;

class TransformCLI {

  static main(args) {
    ExecApplication app = new ExecApplication() {
      protected String getBaseCommand() {
        'hale transform'
      }
    }
    def returnCode = ApplicationUtil.launchSyncApplication(app, args as List)
    if (returnCode && returnCode != 0) {
      System.exit(returnCode)
    }
  }

}
