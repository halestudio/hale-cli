
/*
 * Copyright (c) 2017 wetransform GmbH
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution. If not, see <http://www.gnu.org/licenses/>.
 */
package to.wetransform.halecli.project.match

import groovy.cli.picocli.CliBuilder
import groovy.cli.picocli.OptionAccessor

import eu.esdihumboldt.hale.common.align.model.Alignment
import eu.esdihumboldt.hale.common.core.io.project.model.Project
import eu.esdihumboldt.hale.common.core.io.supplier.FileIOSupplier
import eu.esdihumboldt.hale.common.schema.model.Schema
import eu.esdihumboldt.hale.common.schema.model.impl.DefaultSchemaSpace
import eu.esdihumboldt.util.cli.Command
import eu.esdihumboldt.util.cli.CommandContext
import to.wetransform.halecli.util.ProjectCLI
import to.wetransform.halecli.util.SchemaCLI

abstract class MatchSchemasCommand implements Command {

  protected abstract SchemaMatcher createMatcher()

  @Override
  public int run(List<String> args, CommandContext context) {
    CliBuilder cli = new CliBuilder(usage : "${context.baseCommand} [options] [...]")

    cli._(longOpt: 'help', 'Show this help')

    // options for loading schemas
    SchemaCLI.loadSchemaOptions(cli, 'reference-schema', 'The reference schema')
    SchemaCLI.loadSchemaOptions(cli, 'target-schema', 'The target schema')
    // options for project to save
    ProjectCLI.saveProjectOptions(cli)

    OptionAccessor options = cli.parse(args)

    if (options.help) {
      cli.usage()
      return 0
    }

    // load schemas
    Schema refSchema = SchemaCLI.loadSchema(options, 'reference-schema')
    assert refSchema
    Schema targetSchema = SchemaCLI.loadSchema(options, 'target-schema')
    assert targetSchema

    // generate mapping between schemas
    SchemaMatcher matcher = createMatcher()
    Alignment alignment = matcher.generateSchemaMatching(refSchema, targetSchema)

    // save project
    Project project = new Project()
    project.author = 'Generated'
    project.name = 'Generated schema-to-schema mapping'

    File projectOut = new File('schemas-mapping.halex')
    def output = new FileIOSupplier(projectOut)

    def sourceSS = new DefaultSchemaSpace()
    sourceSS.addSchema(refSchema)

    def targetSS = new DefaultSchemaSpace()
    targetSS.addSchema(targetSchema)

    project.resources << SchemaCLI.getSchemaIOConfig(options, 'reference-schema', true)
    project.resources << SchemaCLI.getSchemaIOConfig(options, 'target-schema', false)

    println 'Saving generated project...'
    ProjectCLI.saveProject(options, project, alignment, sourceSS, targetSS, null)
    println 'Completed'

    return 0
  }

  final boolean experimental = true
}
