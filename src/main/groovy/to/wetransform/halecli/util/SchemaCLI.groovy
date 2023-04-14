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
import java.net.URI;
import java.util.Map

import eu.esdihumboldt.hale.common.cli.HaleCLIUtil;
import eu.esdihumboldt.hale.common.core.io.HaleIO
import eu.esdihumboldt.hale.common.core.io.Value
import eu.esdihumboldt.hale.common.core.io.impl.LogProgressIndicator;
import eu.esdihumboldt.hale.common.core.io.project.model.IOConfiguration;
import eu.esdihumboldt.hale.common.core.io.report.IOReport;
import eu.esdihumboldt.hale.common.core.io.supplier.DefaultInputSupplier;
import eu.esdihumboldt.hale.common.core.io.supplier.LocatableInputSupplier;
import eu.esdihumboldt.hale.common.core.report.ReportHandler
import eu.esdihumboldt.hale.common.instance.io.InstanceReader
import eu.esdihumboldt.hale.common.instance.io.InstanceWriter;
import eu.esdihumboldt.hale.common.instance.model.InstanceCollection;
import eu.esdihumboldt.hale.common.schema.io.SchemaIO;
import eu.esdihumboldt.hale.common.schema.io.SchemaReader
import eu.esdihumboldt.hale.common.schema.io.SchemaWriter;
import eu.esdihumboldt.hale.common.schema.model.Schema
import eu.esdihumboldt.hale.common.schema.model.SchemaSpace;
import eu.esdihumboldt.hale.common.schema.model.impl.DefaultSchemaSpace
import eu.esdihumboldt.util.Pair;
import eu.esdihumboldt.util.cli.CLIUtil
import groovy.transform.CompileStatic
import groovy.cli.picocli.CliBuilder
import groovy.cli.picocli.OptionAccessor

/**
 * Common utility functions for setting up a CliBuilder for loading a schema.
 *
 * @author Simon Templer
 */
class SchemaCLI {

  static void loadSchemaOptions(CliBuilder cli, String argName = 'schema', String descr = 'Schema to load') {
    cli._(longOpt: argName, args:1, argName:'file-or-URL', descr)
    cli._(longOpt: argName + '-setting', args:2, valueSeparator:'=', argName:'setting=value',
      'Setting for schema reader (optional, repeatable)')
    cli._(longOpt: argName + '-reader', args:1, argName: 'provider-id',
      'Identifier of schema reader to use')
  }

  static Schema loadSchema(OptionAccessor options, String argName = 'schema') {
    def reports = HaleCLIUtil.createReportHandler(options)

    def location = options."$argName"
    if (location) {
      URI loc = CLIUtil.fileOrUri(location)

      def settings = getSettings(options, argName)

      String customProvider = options."${argName}-reader" ?: null

      return loadSchema(loc, settings, customProvider, reports)
    }
    else {
      return null
    }
  }

  static Map<String, String> getSettings(OptionAccessor options, String argName = 'schema') {
    def settings = options."${argName}-settings"
    settings ? settings.toSpreadMap() : [:]
  }

  static IOConfiguration getSchemaIOConfig(OptionAccessor options, String argName = 'schema',
      boolean isSource = true) {
    def location = options."$argName"
    if (location) {
      URI loc = CLIUtil.fileOrUri(location)

      def settings = options."${argName}-settings"
      settings = settings ? settings.toSpreadMap() : [:]

      String customProvider = options."${argName}-reader" ?: null

      return getSchemaIOConfig(loc, settings, customProvider, isSource)
    }
    else {
      return null
    }
  }

  @CompileStatic
  static IOConfiguration getSchemaIOConfig(URI loc, Map<String, String> settings,
      String customProvider, boolean isSource) {
    Pair<SchemaReader, String> readerInfo = prepareReader(loc, SchemaReader, settings, customProvider)
    SchemaReader schemaReader = readerInfo.first

    IOConfiguration conf = new IOConfiguration()

    conf.providerId = readerInfo.second
    conf.actionId = isSource ? SchemaIO.ACTION_LOAD_SOURCE_SCHEMA : SchemaIO.ACTION_LOAD_TARGET_SCHEMA
    schemaReader.storeConfiguration(conf.providerConfiguration)

    conf
  }

  @CompileStatic
  static Schema loadSchema(URI loc, Map<String, String> settings, String customProvider, ReportHandler reports) {
    Pair<SchemaReader, String> readerInfo = prepareReader(loc, SchemaReader, settings, customProvider)
    SchemaReader schemaReader = readerInfo.first

    println "Loading schema from ${loc}..."

    IOReport report = schemaReader.execute(null)
    reports?.publishReport(report)

    schemaReader.getSchema()
  }

  // save schema

  static void saveSchemaOptions(CliBuilder cli, String argName = 'target', String descr = 'Target location') {
    cli._(longOpt: argName, args:1, argName: 'file-or-URI', descr)
    cli._(longOpt: argName + '-setting', args:2, valueSeparator:'=', argName:'setting=value',
      'Setting for target writer (optional, repeatable)')
    cli._(longOpt: argName + '-writer', args:1, argName: 'provider-id',
      'Identifier of schema writer to use')
  }

  private static SchemaWriter getWriter(OptionAccessor options, String argName = 'target') {
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
  private static SchemaWriter getWriter(URI loc, Map<String, String> settings, String providerId) {
    return prepareWriter(providerId, SchemaWriter, settings, loc);
  }

  @CompileStatic
  static IOReport saveSchema(Schema schema, OptionAccessor options, String argName = 'target') {
    DefaultSchemaSpace schemas = new DefaultSchemaSpace()
    schemas.addSchema(schema)
    saveSchema(schemas, options, argName)
  }

  @CompileStatic
  static IOReport saveSchema(SchemaSpace schemas, OptionAccessor options, String argName = 'target') {
    SchemaWriter writer = getWriter(options, argName)
    def loc = writer.getTarget()?.location
    println "Writing schema to ${loc}..."

    writer.setSchemas(schemas)

    IOReport report = writer.execute(new LogProgressIndicator())
    HaleCLIUtil.createReportHandler(options).publishReport(report)

    return report
  }

}
