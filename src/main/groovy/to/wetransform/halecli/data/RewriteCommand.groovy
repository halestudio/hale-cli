
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
package to.wetransform.halecli.data

import static to.wetransform.halecli.util.HaleIOHelper.guessSchema

import groovy.cli.picocli.CliBuilder
import groovy.cli.picocli.OptionAccessor

import eu.esdihumboldt.hale.common.cli.HaleCLIUtil
import eu.esdihumboldt.hale.common.core.io.report.IOReport
import eu.esdihumboldt.hale.common.instance.io.InstanceWriter
import eu.esdihumboldt.hale.common.instance.model.DataSet
import eu.esdihumboldt.hale.common.instance.model.InstanceCollection
import eu.esdihumboldt.hale.common.instance.orient.storage.BrowseOrientInstanceCollection
import eu.esdihumboldt.hale.common.instance.orient.storage.LocalOrientDB
import eu.esdihumboldt.hale.common.schema.model.Schema
import eu.esdihumboldt.hale.common.schema.model.impl.DefaultSchemaSpace
import eu.esdihumboldt.util.cli.CLIUtil
import eu.esdihumboldt.util.cli.Command
import eu.esdihumboldt.util.cli.CommandContext
import to.wetransform.halecli.util.InstanceCLI
import to.wetransform.halecli.util.SchemaCLI

/**
 * Reads a source file and writes it.
 *
 * @author Simon Templer
 */
class RewriteCommand implements Command {

  @Override
  public int run(List<String> args, CommandContext context) {
    CliBuilder cli = new CliBuilder(usage : "${context.baseCommand} [options] [...]")

    HaleCLIUtil.defaultOptions(cli, true)

    cli._(longOpt: 'help', 'Show this help')

    // options for schema
    SchemaCLI.loadSchemaOptions(cli)

    // options for source data
    InstanceCLI.loadOptions(cli)

    // options for target data
    InstanceCLI.saveOptions(cli)

    OptionAccessor options = cli.parse(args)

    if (options.help) {
      cli.usage()
      return 0
    }

    def reports = HaleCLIUtil.createReportHandler(options)

    // handle schema
    Schema schema
    if (options.schema) {
      schema = SchemaCLI.loadSchema(options)
    }
    if (!schema && options.data) {
      // try to guess schema from the source
      URI dataLoc = CLIUtil.fileOrUri(options.data)
      URI schemaLoc = guessSchema(dataLoc)
      if (schemaLoc) {
        println "Guessed schema location as $schemaLoc"
        schema = SchemaCLI.loadSchema(schemaLoc, SchemaCLI.getSettings(options), null, reports)
      }
    }
    assert schema

    // handle source data
    InstanceCollection source = InstanceCLI.load(options, schema)
    assert source

    // create target writer
    InstanceWriter writer = InstanceCLI.getWriter(options)
    assert writer

    // store in temporary database (if necessary)
    LocalOrientDB db
    if (!writer.passthrough) {
      db = InstanceCLI.loadTempDatabase(source, schema, reports)
    }
    try {
      // replace source with database
      if (db != null) {
        source = new BrowseOrientInstanceCollection(db, schema, DataSet.SOURCE)
      }
      // Note: It is important that OrientDB caches are disabled
      // via system properties to have a decent performance

      // write instances
      DefaultSchemaSpace schemaSpace = new DefaultSchemaSpace()
      schemaSpace.addSchema(schema)
      IOReport report = InstanceCLI.save(writer, source, schemaSpace,
        reports)

      if (!report.isSuccess()) {
        throw new IllegalStateException('Writing target file failed: ' + report.summary)
      }
    } finally {
      if (db != null) {
        db.delete()
      }
    }

    return 0
  }

  final String shortDescription = 'Read a data source and write it with specific settings'

  final boolean experimental = true
}
