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

package to.wetransform.halecli.schema

import static eu.esdihumboldt.hale.app.transform.ExecUtil.fail
import static to.wetransform.halecli.util.HaleIOHelper.*
import to.wetransform.halecli.util.SchemaCLI
import eu.esdihumboldt.hale.common.schema.model.Schema
import eu.esdihumboldt.util.cli.Command
import eu.esdihumboldt.util.cli.CommandContext

/**
 * Reads a schema and writes it.
 *
 * @author Simon Templer
 */
class RewriteCommand implements Command {

  @Override
  public int run(List<String> args, CommandContext context) {
    CliBuilder cli = new CliBuilder(usage : "${context.baseCommand} [options] [...]")

    cli._(longOpt: 'help', 'Show this help')

    // options for loading schema
    SchemaCLI.loadSchemaOptions(cli, 'source')

    // options for saving schema
    SchemaCLI.saveSchemaOptions(cli)

    OptionAccessor options = cli.parse(args)

    if (options.help) {
      cli.usage()
      return 0
    }

    // handle schema
    Schema schema = SchemaCLI.loadSchema(options, 'source')
    assert schema

    SchemaCLI.saveSchema(schema, options)

    return 0
  }

  final String shortDescription = 'Read a schema and write it with specific settings'

  final boolean experimental = true

}
