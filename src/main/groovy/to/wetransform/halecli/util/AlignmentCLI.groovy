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

package to.wetransform.halecli.util

import static eu.esdihumboldt.hale.app.transform.ExecUtil.fail;
import static to.wetransform.halecli.util.HaleIOHelper.*

import java.io.InputStream
import java.net.URI;
import java.util.Map

import eu.esdihumboldt.hale.common.align.io.AlignmentWriter
import eu.esdihumboldt.hale.common.align.model.Alignment
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
import groovy.util.CliBuilder
import groovy.util.OptionAccessor;;;;

/**
 * Common utility functions for setting up a CliBuilder for processing alignments.
 *
 * @author Simon Templer
 */
class AlignmentCLI {

  // save alignment

  static void saveAlignmentOptions(CliBuilder cli, String argName = 'target', String descr = 'Target location') {
    cli._(longOpt: argName, args:1, argName: 'file-or-URI', descr)
    cli._(longOpt: argName + '-setting', args:2, valueSeparator:'=', argName:'setting=value',
      'Setting for target writer (optional, repeatable)')
    cli._(longOpt: argName + '-writer', args:1, argName: 'provider-id',
      'Identifier of alignment writer to use')
  }

  private static AlignmentWriter getWriter(OptionAccessor options, String argName = 'target') {
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
  private static AlignmentWriter getWriter(URI loc, Map<String, String> settings, String providerId) {
    return prepareWriter(providerId, AlignmentWriter, settings, loc);
  }

  @CompileStatic
  static IOReport saveAlignment(Alignment alignment, SchemaSpace sourceSchema, SchemaSpace targetSchema,
      OptionAccessor options, String argName = 'target') {
    AlignmentWriter writer = getWriter(options, argName)
    def loc = writer.getTarget()?.location
    println "Writing alignment to ${loc}..."

    writer.setAlignment(alignment)
    writer.setSourceSchema(sourceSchema)
    writer.setTargetSchema(targetSchema)

    IOReport report = writer.execute(new LogProgressIndicator())
    HaleCLIUtil.createReportHandler(options).publishReport(report)

    return report
  }

}
