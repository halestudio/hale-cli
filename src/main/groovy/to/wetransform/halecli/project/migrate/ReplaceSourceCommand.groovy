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

import eu.esdihumboldt.hale.common.align.merge.impl.DefaultSchemaMigration
import eu.esdihumboldt.hale.common.core.io.project.model.IOConfiguration
import eu.esdihumboldt.hale.common.schema.model.Schema
import eu.esdihumboldt.hale.common.schema.model.SchemaSpace
import eu.esdihumboldt.hale.common.schema.model.impl.DefaultSchemaSpace
import groovy.cli.picocli.CliBuilder
import groovy.transform.CompileStatic
import to.wetransform.halecli.util.SchemaCLI
import groovy.cli.picocli.OptionAccessor
/**
 * Command that migrates a project to a different schema.
 *
 * @author Simon Templer
 */
@CompileStatic
class ReplaceSourceCommand extends AbstractMigrationCommand<DefaultSchemaMigration> {

  @Override
  protected void addOptions(CliBuilder cli) {
// options for loading new source schema
    SchemaCLI.loadSchemaOptions(cli, 'schema', 'The new source schema for the project')
  }

  @Override
  protected DefaultSchemaMigration createMigration(OptionAccessor options) {
    println 'Loading new source schema...'
    Schema newSource = SchemaCLI.loadSchema(options, 'schema')
    assert newSource

    //TODO support multiple schemas?

    DefaultSchemaSpace schemaSpace = new DefaultSchemaSpace()
    schemaSpace.addSchema(newSource)

    new DefaultSchemaMigration(schemaSpace)
  }

  @Override
  protected SchemaSpace getNewSource(DefaultSchemaMigration migration, OptionAccessor options) {
    migration.newSchema
  }

  @Override
  protected List<IOConfiguration> getNewSourceConfig(DefaultSchemaMigration migration, OptionAccessor options) {
    [SchemaCLI.getSchemaIOConfig(options, 'schema', true)]
  }

  final String shortDescription = 'Migrate a source project to a new source schema'

  final boolean experimental = true

}
