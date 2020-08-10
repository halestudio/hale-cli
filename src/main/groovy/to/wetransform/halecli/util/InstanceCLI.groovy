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

import eu.esdihumboldt.hale.app.transform.ConsoleProgressMonitor
import eu.esdihumboldt.hale.common.cli.HaleCLIUtil;
import eu.esdihumboldt.hale.common.core.io.HaleIO
import eu.esdihumboldt.hale.common.core.io.impl.LogProgressIndicator
import eu.esdihumboldt.hale.common.core.io.report.IOReport;
import eu.esdihumboldt.hale.common.core.io.supplier.DefaultInputSupplier;
import eu.esdihumboldt.hale.common.core.io.supplier.LocatableInputSupplier
import eu.esdihumboldt.hale.common.core.io.supplier.LocatableOutputSupplier
import eu.esdihumboldt.hale.common.core.report.ReportHandler;
import eu.esdihumboldt.hale.common.core.service.ServiceProvider;
import eu.esdihumboldt.hale.common.headless.transform.filter.InstanceFilterDefinition
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

  static void loadOptions(CliBuilder cli, String argName = 'data', String descr = 'Data to load', boolean allowFilter = true) {
    cli._(longOpt: argName, args:1, argName:'file-or-URL', descr)
    cli._(longOpt: argName + '-setting', args:2, valueSeparator:'=', argName:'setting=value',
      'Setting for instance reader (optional, repeatable)')
    cli._(longOpt: argName + '-reader', args:1, argName: 'provider-id',
      'Identifier of instance reader to use (otherwise auto-detect)')

    // filter options
    if (allowFilter) {
      filterOptions(cli, argName)
    }
  }

  static void filterOptions(CliBuilder cli, String argName) {
    def prefix = argName ? argName + '-' : ''
    cli._(longOpt: prefix + 'filter', args: 1, argName: 'filter',
      'Filter expression that is checked against all objects read from the source. The filter language can be specified at the beginning of the filter expression, followed by a colon. If no language is provided explicitly, the expression is assumed to be CQL. If multiple filters are provided an object must only match one of them.')
    cli._(longOpt: prefix + 'exclude', args: 1, argName: 'filter',
      'All objects matching the filter will be exlcuded.')
    cli._(longOpt: prefix + 'filter-on', args: 2, valueSeparator:'=', argName: 'type=filter',
      'Filter on a specific type only. You can specify the type\'s name with or without namespace. If you want to specify the namespace, wrap it in curly braces and prepend it to the type name.')
    cli._(longOpt: prefix + 'exclude-type', args: 1, argName: 'type',
      'Exclude a specific type')
  }

  static InstanceFilterDefinition createFilter(OptionAccessor options, String argName) {
    def prefix = argName ? argName + '-' : ''
    InstanceFilterDefinition res = null

    def filter = options."${prefix}filters" // magic "s" at the end yields a list
    if (filter) {
      if (!res) res = new InstanceFilterDefinition();

      filter.each {
        res.addUnconditionalFilter(it)
      }
    }

    filter = options."${prefix}excludes"
    if (filter) {
      if (!res) res = new InstanceFilterDefinition();

      filter.each {
        res.addExcludeFilter(it)
      }
    }

    filter = options."${prefix}filter-ons"
    if (filter) {
      if (!res) res = new InstanceFilterDefinition();

      filter.toSpreadMap().each { key, value ->
        res.addTypeFilter(key, value)
      }
    }

    filter = options."${prefix}exclude-types"
    if (filter) {
      if (!res) res = new InstanceFilterDefinition();

      filter.each {
        res.addExcludedType(it)
      }
    }

    return res
  }

  static InstanceCollection load(OptionAccessor options, TypeIndex schema, String argName = 'data') {
    def reports = HaleCLIUtil.createReportHandler(options)

    def location = options."$argName"
    if (location) {
      URI loc = CLIUtil.fileOrUri(location)

      def settings = options."${argName}-settings"
      settings = settings ? settings.toSpreadMap() : [:]

      String customProvider = options."${argName}-reader" ?: null

      InstanceFilterDefinition filter = createFilter(options, argName)

      return load(loc, settings, customProvider, schema, reports, filter)
    }
    else {
      return null
    }
  }

  @CompileStatic
  static InstanceCollection load(URI loc, Map<String, String> settings, String customProvider,
      TypeIndex schema, ReportHandler reports, InstanceFilterDefinition filter = null) {

    Pair<InstanceReader, String> readerInfo = prepareReader(loc, InstanceReader, settings, customProvider)
    InstanceReader instanceReader = readerInfo.first

    instanceReader.setSourceSchema(schema)

    println "Loading data from ${loc}..."

    IOReport report = instanceReader.execute(null)
    reports?.publishReport(report)

    InstanceCollection result = instanceReader.getInstances()

    if (filter) {
      result = result.select(filter)
    }

    return result
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
  static IOReport save(InstanceWriter instanceWriter, InstanceCollection instances, SchemaSpace targetSchema,
      ReportHandler reports) {
    def loc = instanceWriter.getTarget()?.location
    println "Writing instances to ${loc}..."

    instanceWriter.setTargetSchema(targetSchema)
    instanceWriter.setInstances(instances)

    IOReport report = instanceWriter.execute(new LogProgressIndicator())
    reports?.publishReport(report)

    return report
  }

  // other helpers

  @CompileStatic
  static LocalOrientDB loadTempDatabase(InstanceCollection instances, TypeIndex schema,
      ReportHandler reports = null) {
    // create db
    File tmpDir = Files.createTempDir();
    LocalOrientDB db = new LocalOrientDB(tmpDir);
    tmpDir.deleteOnExit();

    ServiceProvider serviceProvider = null

    // run store instance job first...
    Job storeJob = new StoreInstancesJob("Load source instances into temporary database",
        db, instances, serviceProvider, reports, false) {

      @Override
      protected void onComplete() {
        // do nothing
      }

    };

    storeJob.run(new ConsoleProgressMonitor())

    db
  }

}
