
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
package to.wetransform.halecli.project.merge

import eu.esdihumboldt.hale.common.align.merge.impl.MatchingMigration
import eu.esdihumboldt.hale.common.core.io.project.model.IOConfiguration
import eu.esdihumboldt.hale.common.core.service.ServiceProvider
import eu.esdihumboldt.hale.common.headless.impl.ProjectTransformationEnvironment
import eu.esdihumboldt.hale.common.schema.io.SchemaIO
import eu.esdihumboldt.hale.common.schema.model.SchemaSpace
import groovy.cli.picocli.CliBuilder
import groovy.cli.picocli.OptionAccessor
import to.wetransform.halecli.project.migrate.AbstractMigratorCommand
import to.wetransform.halecli.util.ProjectCLI

/**
 * Command that migrates a project to a different schema.
 *
 * @author Simon Templer
 */
class MergeCommand extends AbstractMigratorCommand<MergeMigrator, MatchingMigration> {

  private File statisticsFile

  private MergeMigrator migrator

  @Override
  protected void init(OptionAccessor options) {
    super.init(options)

    def statLoc = options.'statistics'
    if (statLoc) {
      statisticsFile = new File(statLoc)
    }
  }

  @Override
  protected void wrapup() {
    if (statisticsFile && migrator?.statistics) {
      statisticsFile.withWriter {
        migrator.statistics.writeTo(it)
      }
    }

    super.wrapup()
  }

  @Override
  protected MergeMigrator createMigrator(ServiceProvider serviceProvider, OptionAccessor options) {
    migrator = new MergeMigrator(serviceProvider, !!statisticsFile)
    migrator
  }

  @Override
  protected void addOptions(CliBuilder cli) {
    // options for loading matching command
    ProjectCLI.loadProjectOptions(cli, 'matching-project', 'The project defining a schema matching')

    // option for collecting statistics
    cli._(longOpt: 'statistics', args:1, argName:'file', 'File to write merge statistics to')
  }

  @Override
  protected MatchingMigration createMigration(OptionAccessor options) {
    println 'Loading matching project...'
    ProjectTransformationEnvironment matchProject = ProjectCLI.loadProject(options, 'matching-project')
    assert matchProject

    //TODO customize migration?

    new MatchingMigration(matchProject, true)
  }

  @Override
  protected SchemaSpace getNewSource(MatchingMigration migration, OptionAccessor options) {
    migration.project.sourceSchema
  }

  @Override
  protected List<IOConfiguration> getNewSourceConfig(MatchingMigration migration, OptionAccessor options) {
    def actionId = SchemaIO.ACTION_LOAD_SOURCE_SCHEMA

    migration.project.project.resources.findAll { IOConfiguration conf ->
      conf.actionId == actionId
    } as List
  }

  final String shortDescription = "Migrate a source project based on a project providing a schema mapping to the project's source"

  final boolean experimental = true
}
