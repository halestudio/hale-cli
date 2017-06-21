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

package to.wetransform.halecli.util

import static eu.esdihumboldt.hale.app.transform.ExecUtil.fail;
import static to.wetransform.halecli.util.HaleIOHelper.*

import java.io.InputStream
import java.io.OutputStream;
import java.net.URI;
import java.util.Map

import org.eclipse.core.runtime.jobs.Job;

import com.google.common.io.Files

import eu.esdihumboldt.hale.app.transform.ConsoleProgressMonitor;
import eu.esdihumboldt.hale.common.core.io.HaleIO
import eu.esdihumboldt.hale.common.core.io.impl.LogProgressIndicator
import eu.esdihumboldt.hale.common.core.io.report.IOReport;
import eu.esdihumboldt.hale.common.core.io.supplier.DefaultInputSupplier;
import eu.esdihumboldt.hale.common.core.io.supplier.LocatableInputSupplier
import eu.esdihumboldt.hale.common.core.io.supplier.LocatableOutputSupplier
import eu.esdihumboldt.hale.common.core.report.ReportHandler;
import eu.esdihumboldt.hale.common.core.service.ServiceProvider;
import eu.esdihumboldt.hale.common.instance.io.InstanceReader
import eu.esdihumboldt.hale.common.instance.io.InstanceWriter;
import eu.esdihumboldt.hale.common.instance.model.InstanceCollection
import eu.esdihumboldt.hale.common.instance.orient.storage.LocalOrientDB
import eu.esdihumboldt.hale.common.instance.orient.storage.StoreInstancesJob;
import eu.esdihumboldt.hale.common.schema.io.SchemaReader;
import eu.esdihumboldt.hale.common.schema.model.Schema
import eu.esdihumboldt.hale.common.schema.model.SchemaSpace
import eu.esdihumboldt.hale.common.schema.model.TypeIndex
import eu.esdihumboldt.util.Pair;
import eu.esdihumboldt.util.cli.CLIUtil
import groovy.transform.CompileStatic
import groovy.util.OptionAccessor

/**
 * Common utility functions for setting up a CliBuilder for loading/saving instances.
 *
 * @author Simon Templer
 */
class InstanceCLI {

  static void loadOptions(CliBuilder cli, String argName = 'data', String descr = 'Data to load') {
    cli._(longOpt: argName, args:1, argName:'file-or-URL', descr)
    cli._(longOpt: argName + '-setting', args:2, valueSeparator:'=', argName:'setting=value',
      'Setting for instance reader (optional, repeatable)')
    cli._(longOpt: argName + '-reader', args:1, argName: 'provider-id',
      'Identifier of instance reader to use (otherwise auto-detect)')
  }

  static InstanceCollection load(OptionAccessor options, TypeIndex schema, String argName = 'data') {
    def location = options."$argName"
    if (location) {
      URI loc = CLIUtil.fileOrUri(location)

      def settings = options."${argName}-settings"
      settings = settings ? settings.toSpreadMap() : [:]

      String customProvider = options."${argName}-reader" ?: null

      return load(loc, settings, customProvider, schema)
    }
    else {
      return null
    }
  }

  @CompileStatic
  static InstanceCollection load(URI loc, Map<String, String> settings, String customProvider,
      TypeIndex schema) {

    Pair<InstanceReader, String> readerInfo = prepareReader(loc, InstanceReader, settings, customProvider)
    InstanceReader instanceReader = readerInfo.first

    instanceReader.setSourceSchema(schema)

    println "Loading data from ${loc}..."

    IOReport report = instanceReader.execute(null)
    //TODO report?

    instanceReader.getInstances()
  }

  // save data

  static void saveOptions(CliBuilder cli, String argName = 'target', String descr = 'Target location') {
    //TODO support preset?
    cli._(longOpt: argName, args:1, argName: 'file-or-URI', descr)
    cli._(longOpt: argName + '-setting', args:2, valueSeparator:'=', argName:'setting=value',
      'Setting for target writer (optional, repeatable)')
    cli._(longOpt: argName + '-writer', args:1, argName: 'provider-id',
      'Identifier of instance writer to use')
  }

  static InstanceWriter getWriter(OptionAccessor options, String argName = 'target') {
    def location = options."$argName"
    if (location) {
      URI loc = CLIUtil.fileOrUri(location)

      def settings = options."${argName}-settings"
      settings = settings ? settings.toSpreadMap() : [:]

      String providerId = options."${argName}-writer" ?: null

      return getWriter(loc, settings, providerId)
    }
    else {
      return null
    }
  }

  @CompileStatic
  static InstanceWriter getWriter(URI loc, Map<String, String> settings, String providerId) {
    // writer is returned because of writing instance directly, because for some
    // use cases it is required to first check

    return prepareWriter(providerId, InstanceWriter, settings, loc);
  }

  @CompileStatic
  static IOReport save(InstanceWriter instanceWriter, InstanceCollection instances, SchemaSpace targetSchema) {
    def loc = instanceWriter.getTarget()?.location
    println "Writing instances to ${loc}..."

    instanceWriter.setTargetSchema(targetSchema)
    instanceWriter.setInstances(instances)

    IOReport report = instanceWriter.execute(new LogProgressIndicator())
    //TODO report?

    return report
  }

  // other helpers

  @CompileStatic
  static LocalOrientDB loadTempDatabase(InstanceCollection instances, TypeIndex schema) {
    // create db
    File tmpDir = Files.createTempDir();
    LocalOrientDB db = new LocalOrientDB(tmpDir);
    tmpDir.deleteOnExit();

    ServiceProvider serviceProvider = null
    ReportHandler reportHandler = null

    // run store instance job first...
    Job storeJob = new StoreInstancesJob("Load source instances into temporary database",
        db, instances, serviceProvider, reportHandler, false) {

      @Override
      protected void onComplete() {
        // do nothing
      }

    };

    storeJob.run(new ConsoleProgressMonitor())

    db
  }

}
