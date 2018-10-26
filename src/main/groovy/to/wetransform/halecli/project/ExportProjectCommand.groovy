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

package to.wetransform.halecli.project

import java.util.List

import eu.esdihumboldt.hale.common.align.migrate.AlignmentMigration;
import eu.esdihumboldt.hale.common.align.migrate.AlignmentMigrator
import eu.esdihumboldt.hale.common.align.migrate.MigrationOptions;
import eu.esdihumboldt.hale.common.align.migrate.impl.DefaultAlignmentMigrator
import eu.esdihumboldt.hale.common.align.migrate.impl.MigrationOptionsImpl
import eu.esdihumboldt.hale.common.align.migrate.util.EffectiveMapping;
import eu.esdihumboldt.hale.common.align.model.Alignment
import eu.esdihumboldt.hale.common.cli.HaleCLIUtil;
import eu.esdihumboldt.hale.common.core.io.project.model.IOConfiguration;
import eu.esdihumboldt.hale.common.core.io.project.model.Project
import eu.esdihumboldt.hale.common.core.report.SimpleLog;
import eu.esdihumboldt.hale.common.core.service.ServiceProvider;
import eu.esdihumboldt.hale.common.headless.impl.ProjectTransformationEnvironment
import eu.esdihumboldt.hale.common.instance.io.InstanceIO;
import eu.esdihumboldt.hale.common.schema.io.SchemaIO;
import eu.esdihumboldt.hale.common.schema.model.SchemaSpace;
import eu.esdihumboldt.util.cli.Command
import eu.esdihumboldt.util.cli.CommandContext
import groovy.transform.CompileStatic
import to.wetransform.halecli.util.HaleConnectCLI
import to.wetransform.halecli.util.ProjectCLI;;;;

/**
 * Command exporting a project
 *
 * @author Simon Templer
 */
class ExportProjectCommand implements Command {

  @Override
  public int run(List<String> args, CommandContext context) {
    CliBuilder cli = new CliBuilder(usage : "${context.baseCommand} [options] [...]")

    HaleCLIUtil.defaultOptions(cli, true)

    HaleConnectCLI.loginOptions(cli)

    cli._(longOpt: 'help', 'Show this help')

    // options for projects to load
    ProjectCLI.loadProjectOptions(cli)
    // options for project to save
    ProjectCLI.saveProjectOptions(cli)

    OptionAccessor options = cli.parse(args)

    if (options.help) {
      cli.usage()
      return 0
    }

    HaleConnectCLI.login(options)

    def reports = HaleCLIUtil.createReportHandler(options)

    // load project
    println 'Loading source project...'
    ProjectTransformationEnvironment sourceProject = ProjectCLI.loadProject(options)
    assert sourceProject

    // save project
    println 'Exporting project...'
    ProjectCLI.saveProject(options, sourceProject.project, sourceProject.alignment,
      sourceProject.sourceSchema, sourceProject.targetSchema, sourceProject.loadLocation)

    println 'Completed'

    return 0
  }

  final String shortDescription = 'Exports a project with different settings'

  final boolean experimental = true

}
