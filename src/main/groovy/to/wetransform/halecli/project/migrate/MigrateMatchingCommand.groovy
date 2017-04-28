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
import eu.esdihumboldt.hale.common.align.migrate.impl.MigrationOptionsImpl
import eu.esdihumboldt.hale.common.align.migrate.util.EffectiveMapping;
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
import groovy.transform.TypeCheckingMode;
import groovy.util.CliBuilder;
import groovy.util.OptionAccessor;
import to.wetransform.halecli.util.ProjectCLI;;;;

/**
 * Command that migrates a project to a different schema.
 *
 * @author Simon Templer
 */
class MigrateMatchingCommand extends AbstractMigrateCommand<MatchingMigration> {

  @Override
  protected void addOptions(CliBuilder cli) {
    // options for loading matching command
    ProjectCLI.loadProjectOptions(cli, 'matching-project', 'The project defining a schema matching')

    cli._(longOpt: 'reverse', 'If the matching project has the schema to be migrated to as source')
  }

  @Override
  protected MatchingMigration createMigration(OptionAccessor options) {
    println 'Loading matching project...'
    ProjectTransformationEnvironment matchProject = ProjectCLI.loadProject(options, 'matching-project')
    assert matchProject

    boolean reverse = options.reverse

    new MatchingMigration(matchProject, reverse)
  }

  @Override
  protected SchemaSpace getNewSource(MatchingMigration migration, OptionAccessor options) {
    boolean reverse = options.reverse
    reverse ? migration.project.sourceSchema : migration.project.targetSchema
  }

  @Override
  protected List<IOConfiguration> getNewSourceConfig(MatchingMigration migration, OptionAccessor options) {
    boolean reverse = options.reverse
    def actionId = reverse ? SchemaIO.ACTION_LOAD_SOURCE_SCHEMA : SchemaIO.ACTION_LOAD_TARGET_SCHEMA

    migration.project.project.resources.findAll { IOConfiguration conf ->
      conf.actionId == actionId
    } as List
  }

  final String shortDescription = 'Migrate a source project based on a project providing a schema matching'

  final boolean experimental = true

}
