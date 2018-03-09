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
 *
 * Contributors:
 *     wetransform GmbH <http://www.wetransform.to>
 */

package to.wetransform.halecli.util

import static eu.esdihumboldt.hale.app.transform.ExecUtil.fail
import to.wetransform.halecli.project.ProjectHelper
import eu.esdihumboldt.hale.common.align.model.Alignment
import eu.esdihumboldt.hale.common.cli.HaleCLIUtil
import eu.esdihumboldt.hale.common.core.io.project.model.Project
import eu.esdihumboldt.hale.common.core.io.supplier.DefaultInputSupplier
import eu.esdihumboldt.hale.common.core.io.supplier.FileIOSupplier
import eu.esdihumboldt.hale.common.core.io.supplier.LocatableOutputSupplier;
import eu.esdihumboldt.hale.common.core.report.ReportHandler
import eu.esdihumboldt.hale.common.headless.impl.ProjectTransformationEnvironment
import eu.esdihumboldt.hale.common.schema.model.SchemaSpace
import eu.esdihumboldt.util.cli.CLIUtil
import groovy.transform.CompileStatic

/**
 * Common utility functions for setting up a CliBuilder for loading and saving a project.
 *
 * @author Simon Templer
 */
class ProjectCLI {

  static void loadProjectOptions(CliBuilder cli, String argName = 'project', String descr = 'Project to load') {
    cli._(longOpt: argName, args:1, argName:'file-or-URL', descr)
  }

  static ProjectTransformationEnvironment loadProject(OptionAccessor options, String argName = 'project') {
    def location = options."$argName"
    if (location) {
      URI loc = CLIUtil.fileOrUri(location)
      return loadProject(loc, HaleCLIUtil.createReportHandler(options))
    }
    else {
      return null
    }
  }

  @CompileStatic
  static ProjectTransformationEnvironment loadProject(URI loc, ReportHandler reports) {
    new ProjectTransformationEnvironment(null, new DefaultInputSupplier(
      loc), reports)
  }

  static void saveProjectOptions(CliBuilder cli, String argName = 'target', String descr = 'Target project file') {
    cli._(longOpt: argName, args:1, argName:'file', descr)
  }

  static void saveProject(OptionAccessor options, Project project,
    Alignment alignment, SchemaSpace sourceSchema, SchemaSpace targetSchema,
    String argName = 'target') {

    def reports = HaleCLIUtil.createReportHandler(options)

    def location = options."$argName"
    if (location) {
      URI loc = CLIUtil.fileOrUri(location)
      saveProject(new File(loc), project, alignment, sourceSchema, targetSchema, reports)
    }
    else {
      //XXX
    }
  }

  @CompileStatic
  static void saveProject(File targetFile, Project project, Alignment alignment,
    SchemaSpace sourceSchema, SchemaSpace targetSchema, ReportHandler reports) {

    String fileName = targetFile.name
    String extension = 'halex'
    int dotIndex = fileName.lastIndexOf('.')
    if (dotIndex > 0) {
      String ext = fileName[(dotIndex + 1)..-1]
      if (ext) extension = ext
    }

    def output = new FileIOSupplier(targetFile)

    ProjectHelper.saveProject(project, alignment, sourceSchema,
      targetSchema, output, reports, extension)

    //TODO feedback?
  }

}
