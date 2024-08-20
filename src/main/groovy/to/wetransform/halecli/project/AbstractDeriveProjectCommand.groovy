
/*
 * Copyright (c) 2016 wetransform GmbH
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution. If not, see <http://www.gnu.org/licenses/>.
 */
package to.wetransform.halecli.project

import eu.esdihumboldt.hale.common.align.model.Alignment
import eu.esdihumboldt.hale.common.cli.project.AbstractProjectEnvironmentCommand
import eu.esdihumboldt.hale.common.core.io.project.ComplexConfigurationService
import eu.esdihumboldt.hale.common.core.io.project.ProjectIO
import eu.esdihumboldt.hale.common.core.io.project.model.Project
import eu.esdihumboldt.hale.common.core.io.supplier.FileIOSupplier
import eu.esdihumboldt.hale.common.core.report.ReportHandler
import eu.esdihumboldt.hale.common.headless.impl.ProjectTransformationEnvironment
import eu.esdihumboldt.util.cli.CommandContext
import groovy.cli.picocli.CliBuilder
import groovy.cli.picocli.OptionAccessor

/**
 * Base class for command creating derived projects.
 *
 * @author Simon Templer
 */
abstract class AbstractDeriveProjectCommand extends AbstractProjectEnvironmentCommand {

  static class DeriveProjectResult {
    Alignment alignment
    Project project
  }

  @Override
  void setupOptions(CliBuilder cli) {
    super.setupOptions(cli)

    cli._(longOpt: 'name', args: 1, argName: 'variant-name', 'Set the name of project variant')
  }

  abstract DeriveProjectResult deriveProject(ProjectTransformationEnvironment projectEnv,
  OptionAccessor options)

  @Override
  boolean runForProject(ProjectTransformationEnvironment projectEnv, URI projectLocation,
    OptionAccessor options, CommandContext context, ReportHandler reports) {

    def variant = options.name
    if (!variant) {
      variant = 'variant'
    }

    ComplexConfigurationService orgConf = ProjectIO.createProjectConfigService(projectEnv.project)
    if (orgConf.getBoolean('derivedProject', false)) {
      println 'Skipping derived project'
      return true
    }

    DeriveProjectResult result = deriveProject(projectEnv, options)
    if (!result) {
      return true
    }

    Project project = result.project
    ComplexConfigurationService conf = ProjectIO.createProjectConfigService(project)
    conf.setBoolean('derivedProject', true)

    //XXX only supported for files right now
    File projectFile = new File(projectLocation)

    String fileName = projectFile.name
    String extension = 'halex'
    int dotIndex = fileName.lastIndexOf('.')
    if (dotIndex > 0) {
      String ext = fileName[(dotIndex + 1)..-1]
      if (ext) extension = ext
      fileName = fileName[0..(dotIndex - 1)] + "-${variant}.$extension"
    }

    File projectOut = new File(projectFile.parentFile, fileName)
    def output = new FileIOSupplier(projectOut)

    ProjectHelper.saveProject(project, result.alignment, projectEnv.sourceSchema,
      projectEnv.targetSchema, output, reports, extension, projectLocation)

    true
  }
}
