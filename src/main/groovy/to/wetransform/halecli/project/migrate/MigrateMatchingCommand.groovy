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

import eu.esdihumboldt.hale.common.align.migrate.AlignmentMigration;
import eu.esdihumboldt.hale.common.align.migrate.AlignmentMigrator
import eu.esdihumboldt.hale.common.align.migrate.MigrationOptions;
import eu.esdihumboldt.hale.common.align.migrate.impl.DefaultAlignmentMigrator
import eu.esdihumboldt.hale.common.align.migrate.impl.MigrationOptionsImpl;
import eu.esdihumboldt.hale.common.align.model.Alignment
import eu.esdihumboldt.hale.common.core.io.project.model.IOConfiguration;
import eu.esdihumboldt.hale.common.core.io.project.model.Project;
import eu.esdihumboldt.hale.common.core.service.ServiceProvider;
import eu.esdihumboldt.hale.common.headless.impl.ProjectTransformationEnvironment
import eu.esdihumboldt.hale.common.instance.io.InstanceIO;
import eu.esdihumboldt.hale.common.schema.io.SchemaIO;
import eu.esdihumboldt.hale.common.schema.model.SchemaSpace;
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
    // options for project to save
    ProjectCLI.saveProjectOptions(cli)

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
    ServiceProvider serviceProvider = sourceProject.serviceProvider
    AlignmentMigrator migrator = new DefaultAlignmentMigrator(serviceProvider)

    Alignment originalAlignment = sourceProject.alignment
    AlignmentMigration migration = new MatchingMigration(matchProject.alignment)

    //FIXME configurable if source or target is updated?
    boolean updateSource = true
    boolean updateTarget = false
    boolean transferBase = true
    MigrationOptions opts = new MigrationOptionsImpl(updateSource, updateTarget, transferBase)
    def newAlignment = migrator.updateAligmment(originalAlignment, migration, opts)

    SchemaSpace newSource
    SchemaSpace newTarget

    newSource = matchProject.targetSchema
    newTarget = sourceProject.targetSchema

    Project newProject = new Project(sourceProject.project)
    // update I/O configurations -> new source schema
    // remove source schema and data
    newProject.resources.removeIf { IOConfiguration conf ->
      conf.actionId == SchemaIO.ACTION_LOAD_SOURCE_SCHEMA || conf.actionId == InstanceIO.ACTION_LOAD_SOURCE_DATA
    }
    // transfer source schema from matching project
    def sourceConfs = matchProject.project.resources.findAll { IOConfiguration conf ->
      conf.actionId == SchemaIO.ACTION_LOAD_TARGET_SCHEMA
    }.collect { IOConfiguration conf ->
      IOConfiguration clone = conf.clone()
      clone.actionId = SchemaIO.ACTION_LOAD_SOURCE_SCHEMA
      clone
    }
    newProject.resources.addAll(sourceConfs)

    // migrate project (mapping relevant source types etc.)
    ProjectMigrator.updateProject(newProject, migration, opts, sourceProject.sourceSchema, sourceProject.targetSchema)

    // save target project
    println 'Saving migrated project...'
    ProjectCLI.saveProject(options, newProject, newAlignment, newSource, newTarget)

    println 'Completed'

    return 0
  }

  final String shortDescription = 'Migrate a source project based on a project providing a schema matching'

  final boolean experimental = true

}
