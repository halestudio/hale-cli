
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
package to.wetransform.halecli.data

import static eu.esdihumboldt.hale.app.transform.ExecUtil.fail

import eu.esdihumboldt.hale.common.cli.HaleCLIUtil
import eu.esdihumboldt.hale.common.core.io.HaleIO
import eu.esdihumboldt.hale.common.core.io.Value
import eu.esdihumboldt.hale.common.core.io.report.IOReport
import eu.esdihumboldt.hale.common.core.io.supplier.FileIOSupplier
import eu.esdihumboldt.hale.common.core.report.ReportHandler
import eu.esdihumboldt.hale.common.instance.graph.reference.ReferenceGraph
import eu.esdihumboldt.hale.common.instance.graph.reference.impl.XMLInspector
import eu.esdihumboldt.hale.common.instance.io.InstanceWriter
import eu.esdihumboldt.hale.common.instance.model.DataSet
import eu.esdihumboldt.hale.common.instance.model.InstanceCollection
import eu.esdihumboldt.hale.common.instance.orient.storage.BrowseOrientInstanceCollection
import eu.esdihumboldt.hale.common.instance.orient.storage.LocalOrientDB
import eu.esdihumboldt.hale.common.schema.model.Schema
import eu.esdihumboldt.hale.common.schema.model.impl.DefaultSchemaSpace
import eu.esdihumboldt.util.cli.Command
import eu.esdihumboldt.util.cli.CommandContext
import groovy.cli.picocli.CliBuilder
import groovy.cli.picocli.OptionAccessor
import groovy.transform.CompileStatic
import to.wetransform.halecli.util.InstanceCLI
import to.wetransform.halecli.util.SchemaCLI

/**
 * Splits a GML source file and creates multiple target files.
 *
 * @author Simon Templer
 */
class SplitCommand implements Command {

  @Override
  public int run(List<String> args, CommandContext context) {
    CliBuilder cli = new CliBuilder(usage : "${context.baseCommand} [options] [...]")

    HaleCLIUtil.defaultOptions(cli, true)

    cli._(longOpt: 'help', 'Show this help')

    // threshold for splitting instances
    cli._(longOpt: 'threshold', args: 1, argName: 'max-number', 'The maximum number of instances to put in a part (if possible)')

    // options for schema
    SchemaCLI.loadSchemaOptions(cli)

    // options for source data
    InstanceCLI.loadOptions(cli)

    // options for target data
    cli._(longOpt: 'target', args: 1, required: true, argName: 'target-folder', 'The target folder to write the parts too')
    //TODO more options

    OptionAccessor options = cli.parse(args)

    if (options.help) {
      cli.usage()
      return 0
    }

    // handle schema
    Schema schema = SchemaCLI.loadSchema(options)
    assert schema

    // handle source data
    InstanceCollection source = InstanceCLI.load(options, schema)
    assert source

    def reports = HaleCLIUtil.createReportHandler(options)

    // store in temporary database
    //XXX reason is that sources may have slow InstanceReference resolving (e.g. XML/GML)
    LocalOrientDB db = InstanceCLI.loadTempDatabase(source, schema, reports)
    try {
      // replace source with database
      source = new BrowseOrientInstanceCollection(db, schema, DataSet.SOURCE)
      // Note: It is important that OrientDB caches are disabled
      // via system properties to have a decent performance

      println "Building reference graph..."

      // create a reference graph
      ReferenceGraph<String> rg = new ReferenceGraph<String>(new XMLInspector(),
        source)

      // partition the graph
      int threshold = (options.threshold ?: 10000) as int
      Iterator<InstanceCollection> parts = rg.partition(threshold)

      // target
      def target = options.target as File
      if (!target) {
        throw new IllegalStateException('Please provide a target folder')
      }
      if (target.exists()) {
        if (!target.isDirectory()) {
          throw new IllegalStateException('Target is not a folder')
        }
      }
      else {
        target.mkdirs()
      }

      int partCount = 0
      while (parts.hasNext()) {
        partCount++

        def instances = parts.next()

        //FIXME right now only GML as target supported, with default settings
        File targetFile = new File(target, "part_${partCount}.gml")

        def size = instances.size()
        if (size >= 0) {
          println "Writing part with $size instances to $targetFile"
        }
        else {
          println "Writing part with undefined size to $targetFile"
        }

        saveGml(instances, targetFile, schema, reports)
      }
      println "Total $partCount parts"
    } finally {
      db.delete()
    }

    return 0
  }

  @CompileStatic
  private void saveGml(InstanceCollection instances, File targetFile, Schema schema,
    ReportHandler reports) {
    def target = new FileIOSupplier(targetFile)

    // create I/O provider
    InstanceWriter instanceWriter = null
    String customProvider = 'eu.esdihumboldt.hale.io.gml.writer'
    if (customProvider != null) {
      // use specified provider
      instanceWriter = HaleIO.createIOProvider(InstanceWriter, null, customProvider)
      if (instanceWriter == null) {
        fail("Could not find instance writer with ID " + customProvider)
      }
    }
    if (instanceWriter == null) {
      // find applicable reader
      instanceWriter = HaleIO.findIOProvider(InstanceWriter, target, targetFile.name)
    }
    if (instanceWriter == null) {
      throw fail("Could not determine instance reader to use for source data")
    }

    //FIXME apply custom settings
    instanceWriter.setParameter('xml.pretty', Value.of((Boolean)true))

    DefaultSchemaSpace schemaSpace = new DefaultSchemaSpace()
    schemaSpace.addSchema(schema)
    instanceWriter.targetSchema = schemaSpace
    instanceWriter.target = target
    instanceWriter.instances = instances

    IOReport report = instanceWriter.execute(null)
    reports?.publishReport(report)

    if (!report.isSuccess()) {
      //TODO common way to deal with reports
      throw new IllegalStateException('Writing target file failed: ' + report.summary)
    }
  }

  final String shortDescription = 'Split a source file (GML) into portions'

  final boolean experimental = true
}
