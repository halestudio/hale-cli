/*
 * Copyright (c) 2018 wetransform GmbH
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

package to.wetransform.halecli.project.alignment

import java.net.URI

import eu.esdihumboldt.hale.common.cli.project.AbstractProjectEnvironmentCommand
import eu.esdihumboldt.hale.common.core.report.ReportHandler
import eu.esdihumboldt.hale.common.headless.impl.ProjectTransformationEnvironment
import eu.esdihumboldt.util.cli.CommandContext
import groovy.util.OptionAccessor
import to.wetransform.halecli.util.AlignmentCLI

/**
 * Command to export an alignment.
 *
 * @author Simon Templer
 */
class ExportAlignmentCommand extends AbstractProjectEnvironmentCommand {

  void setupOptions(CliBuilder cli) {
    super.setupOptions(cli)

    // options for saving alignment
    AlignmentCLI.saveAlignmentOptions(cli)
  }

  @Override
  public boolean runForProject(ProjectTransformationEnvironment project, URI projectLocation, OptionAccessor options,
      CommandContext context, ReportHandler reports) {
    //FIXME restrict to use with single project?! Or support multi-project execution?

    AlignmentCLI.saveAlignment(project.alignment, project.sourceSchema, project.targetSchema, options)

    return true
  }

  final String shortDescription = 'Export a project alignment with specific settings'

  final boolean experimental = true

}
