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

package to.wetransform.halecli.project.migrate

import java.util.List

import eu.esdihumboldt.hale.common.headless.impl.ProjectTransformationEnvironment;
import eu.esdihumboldt.util.cli.Command
import eu.esdihumboldt.util.cli.CommandContext
import groovy.transform.CompileStatic
import to.wetransform.halecli.util.ProjectCLI;;;;

/**
 * Command that migrates a project to a different schema.
 *
 * @author Simon Templer
 */
class MigrateMatchingCommand implements Command {

  @Override
  public int run(List<String> args, CommandContext context) {
    CliBuilder cli = new CliBuilder(usage : "${context.baseCommand} [options] [...]")

    cli._(longOpt: 'help', 'Show this help')

    // options for projects to load
    ProjectCLI.loadProjectOptions(cli, 'source-project', 'The source project to migrate')
    ProjectCLI.loadProjectOptions(cli, 'matching-project', 'The project defining a schema matching')

    OptionAccessor options = cli.parse(args)

    if (options.help) {
      cli.usage()
      return 0
    }

    // load projects
    println 'Loading source project...'
    ProjectTransformationEnvironment sourceProject = ProjectCLI.loadProject(options, 'source-project')
    assert sourceProject
    println 'Loading matching project...'
    ProjectTransformationEnvironment matchProject = ProjectCLI.loadProject(options, 'matching-project')
    assert matchProject

    //TODO do matching
    //TODO save target project

    return 0;
  }

  final String shortDescription = 'Migrate a source project based on a project providing a schema matching'

  final boolean experimental = true

}
