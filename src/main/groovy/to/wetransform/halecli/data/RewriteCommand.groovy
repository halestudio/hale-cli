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
 *
 * Contributors:
 *     wetransform GmbH <http://www.wetransform.to>
 */

package to.wetransform.halecli.data

import static eu.esdihumboldt.hale.app.transform.ExecUtil.fail
import static to.wetransform.halecli.util.HaleIOHelper.*

import java.io.File;
import java.util.Iterator;
import java.util.List
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

import com.google.common.io.Files

import eu.esdihumboldt.hale.app.transform.ConsoleProgressMonitor;
import eu.esdihumboldt.hale.common.core.io.HaleIO
import eu.esdihumboldt.hale.common.core.io.Value;
import eu.esdihumboldt.hale.common.core.io.report.IOReport;
import eu.esdihumboldt.hale.common.core.io.supplier.FileIOSupplier
import eu.esdihumboldt.hale.common.headless.transform.AbstractTransformationJob;
import eu.esdihumboldt.hale.common.instance.graph.reference.ReferenceGraph;
import eu.esdihumboldt.hale.common.instance.graph.reference.impl.XMLInspector
import eu.esdihumboldt.hale.common.instance.io.InstanceWriter
import eu.esdihumboldt.hale.common.instance.model.DataSet;
import eu.esdihumboldt.hale.common.instance.model.Filter;
import eu.esdihumboldt.hale.common.instance.model.Instance;
import eu.esdihumboldt.hale.common.instance.model.InstanceCollection
import eu.esdihumboldt.hale.common.instance.model.impl.FilteredInstanceCollection;
import eu.esdihumboldt.hale.common.instance.orient.OInstance;
import eu.esdihumboldt.hale.common.instance.orient.storage.BrowseOrientInstanceCollection;
import eu.esdihumboldt.hale.common.instance.orient.storage.LocalOrientDB
import eu.esdihumboldt.hale.common.instance.orient.storage.StoreInstancesJob;
import eu.esdihumboldt.hale.common.schema.model.Schema;
import eu.esdihumboldt.hale.common.schema.model.TypeIndex
import eu.esdihumboldt.hale.common.schema.model.impl.DefaultSchemaSpace
import eu.esdihumboldt.util.cli.CLIUtil;
import eu.esdihumboldt.util.cli.Command
import eu.esdihumboldt.util.cli.CommandContext
import groovy.transform.CompileStatic
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
        schema = SchemaCLI.loadSchema(schemaLoc, [:], null)
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
      db = InstanceCLI.loadTempDatabase(source, schema)
    }
    try {
      // replace source with database
      if (db != null) {
        source = new BrowseOrientInstanceCollection(db, schema, DataSet.SOURCE);
      }
      // Note: It is important that OrientDB caches are disabled
      // via system properties to have a decent performance

      // write instances
      DefaultSchemaSpace schemaSpace = new DefaultSchemaSpace()
      schemaSpace.addSchema(schema)
      IOReport report = InstanceCLI.save(writer, source, schemaSpace)

      if (!report.isSuccess()) {
        //TODO common way to deal with reports
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
