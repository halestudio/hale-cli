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

import static eu.esdihumboldt.hale.app.transform.ExecUtil.fail;

import java.io.InputStream

import eu.esdihumboldt.hale.common.cli.HaleCLIUtil;
import eu.esdihumboldt.hale.common.core.io.HaleIO
import eu.esdihumboldt.hale.common.core.io.project.model.IOConfiguration;
import eu.esdihumboldt.hale.common.core.io.report.IOReport;
import eu.esdihumboldt.hale.common.core.io.supplier.DefaultInputSupplier;
import eu.esdihumboldt.hale.common.core.io.supplier.LocatableInputSupplier
import eu.esdihumboldt.hale.common.core.report.ReportHandler;
import eu.esdihumboldt.hale.common.headless.impl.ProjectTransformationEnvironment;
import eu.esdihumboldt.hale.common.instance.io.InstanceReader
import eu.esdihumboldt.hale.common.schema.io.SchemaIO;
import eu.esdihumboldt.hale.common.schema.io.SchemaReader;
import eu.esdihumboldt.hale.common.schema.model.Schema
import eu.esdihumboldt.util.Pair;
import eu.esdihumboldt.util.cli.CLIUtil
import groovy.transform.CompileStatic;;

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
      return loadProject(loc)
    }
    else {
      return null
    }
  }

  @CompileStatic
  static ProjectTransformationEnvironment loadProject(URI loc) {
    ReportHandler reports = HaleCLIUtil.createReportHandler()
    new ProjectTransformationEnvironment(null, new DefaultInputSupplier(
      loc), reports)
  }

}
