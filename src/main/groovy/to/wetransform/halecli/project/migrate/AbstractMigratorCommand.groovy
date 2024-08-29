
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
package to.wetransform.halecli.project.migrate

import groovy.cli.picocli.CliBuilder
import groovy.cli.picocli.OptionAccessor

import eu.esdihumboldt.hale.common.align.migrate.AlignmentMigration
import eu.esdihumboldt.hale.common.align.migrate.AlignmentMigrator
import eu.esdihumboldt.hale.common.align.migrate.MigrationOptions
import eu.esdihumboldt.hale.common.align.migrate.impl.MigrationOptionsImpl
import eu.esdihumboldt.hale.common.align.migrate.util.EffectiveMapping
import eu.esdihumboldt.hale.common.align.model.Alignment
import eu.esdihumboldt.hale.common.cli.HaleCLIUtil
import eu.esdihumboldt.hale.common.core.io.project.model.IOConfiguration
import eu.esdihumboldt.hale.common.core.io.project.model.Project
import eu.esdihumboldt.hale.common.core.report.SimpleLog
import eu.esdihumboldt.hale.common.core.service.ServiceProvider
import eu.esdihumboldt.hale.common.headless.impl.ProjectTransformationEnvironment
import eu.esdihumboldt.hale.common.instance.io.InstanceIO
import eu.esdihumboldt.hale.common.schema.io.SchemaIO
import eu.esdihumboldt.hale.common.schema.model.SchemaSpace
import eu.esdihumboldt.util.cli.Command
import eu.esdihumboldt.util.cli.CommandContext
import to.wetransform.halecli.util.ProjectCLI

/**
 * Base class for commands migrating a project to a different schema.
 *
 * @author Simon Templer
 */
abstract class AbstractMigratorCommand<M extends AlignmentMigrator, T extends AlignmentMigration>
implements Command {

  protected abstract void addOptions(CliBuilder cli)

  protected abstract M createMigrator(ServiceProvider serviceProvider, OptionAccessor options)

  protected abstract T createMigration(OptionAccessor options)

  protected abstract SchemaSpace getNewSource(T migration, OptionAccessor options)

  protected abstract List<IOConfiguration> getNewSourceConfig(T migration, OptionAccessor options)

  @Override
  public int run(List<String> args, CommandContext context) {
    CliBuilder cli = new CliBuilder(usage : "${context.baseCommand} [options] [...]")

    HaleCLIUtil.defaultOptions(cli, true)

    cli._(longOpt: 'help', 'Show this help')

    // options for projects to load
    ProjectCLI.loadProjectOptions(cli, 'source-project', 'The source project to migrate')
    // options for project to save
    ProjectCLI.saveProjectOptions(cli)

    // add custom command options
    addOptions(cli)

    OptionAccessor options = cli.parse(args)

    if (options.help) {
      cli.usage()
      return 0
    }

    def reports = HaleCLIUtil.createReportHandler(options)

    init(options)

    SimpleLog log = SimpleLog.CONSOLE_LOG //TODO use report or other kind of log?

    // load projects
    println 'Loading source project...'
    ProjectTransformationEnvironment sourceProject = ProjectCLI.loadProject(options, 'source-project')
    assert sourceProject

    //TODO do matching
    ServiceProvider serviceProvider = sourceProject.serviceProvider
    AlignmentMigrator migrator = createMigrator(serviceProvider, options)

    Alignment originalAlignment = sourceProject.alignment

    T migration = createMigration(options)

    // to effective mapping TODO configurable?
    originalAlignment = EffectiveMapping.expand(originalAlignment)

    //FIXME configurable if source or target is updated?
    //XXX right now only updating the source schema is supported
    boolean updateSource = true
    boolean updateTarget = false
    boolean transferBase = true
    MigrationOptions opts = new MigrationOptionsImpl(updateSource, updateTarget, transferBase)
    def newAlignment = migrator.updateAligmment(originalAlignment, migration, opts, log)

    SchemaSpace newSource
    SchemaSpace newTarget

    newSource = getNewSource(migration, options)
    newTarget = sourceProject.targetSchema

    Project newProject = new Project(sourceProject.project)
    // update I/O configurations -> new source schema
    // remove source schema and data
    newProject.resources.removeIf { IOConfiguration conf ->
      conf.actionId == SchemaIO.ACTION_LOAD_SOURCE_SCHEMA || conf.actionId == InstanceIO.ACTION_LOAD_SOURCE_DATA
    }
    // apply source schema
    def sourceConfs = getNewSourceConfig(migration, options).collect { IOConfiguration conf ->
      IOConfiguration clone = conf.clone()
      clone.actionId = SchemaIO.ACTION_LOAD_SOURCE_SCHEMA
      clone
    }
    newProject.resources.addAll(sourceConfs)

    // migrate project (mapping relevant source types etc.)
    ProjectMigrator.updateProject(newProject, migration, opts, sourceProject.sourceSchema,
      sourceProject.targetSchema, log)

    // save target project
    println 'Saving migrated project...'
    ProjectCLI.saveProject(options, newProject, newAlignment, newSource, newTarget, null)

    wrapup()

    println 'Completed'

    return 0
  }

  protected void init(OptionAccessor options) {
    // override me
  }

  protected void wrapup() {
    // override me
  }
}
