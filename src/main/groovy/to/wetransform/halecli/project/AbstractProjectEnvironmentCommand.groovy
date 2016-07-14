package to.wetransform.halecli.project

import java.net.URI

import eu.esdihumboldt.hale.common.core.io.supplier.DefaultInputSupplier;
import eu.esdihumboldt.hale.common.core.report.ReportHandler
import eu.esdihumboldt.hale.common.headless.impl.ProjectTransformationEnvironment
import groovy.transform.CompileStatic;
import groovy.util.OptionAccessor;
import to.wetransform.halecli.CommandContext;

@CompileStatic
abstract class AbstractProjectEnvironmentCommand extends AbstractProjectCommand<ProjectTransformationEnvironment> {

  @Override
  ProjectTransformationEnvironment loadProject(URI location, ReportHandler reports) {
    new ProjectTransformationEnvironment(null, new DefaultInputSupplier(
      location), reports)
  }

  @Override
  String getProjectName(ProjectTransformationEnvironment project) {
    project?.project?.name
  }

}
