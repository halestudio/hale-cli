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

import java.io.InputStream;

import eu.esdihumboldt.hale.common.core.io.HaleIO
import eu.esdihumboldt.hale.common.core.io.report.IOReport;
import eu.esdihumboldt.hale.common.core.io.supplier.DefaultInputSupplier;
import eu.esdihumboldt.hale.common.core.io.supplier.LocatableInputSupplier;
import eu.esdihumboldt.hale.common.instance.io.InstanceReader
import eu.esdihumboldt.hale.common.instance.model.InstanceCollection;
import eu.esdihumboldt.hale.common.schema.io.SchemaReader;
import eu.esdihumboldt.hale.common.schema.model.Schema
import eu.esdihumboldt.hale.common.schema.model.TypeIndex
import eu.esdihumboldt.util.cli.CLIUtil
import groovy.transform.CompileStatic;;

/**
 * Common utility functions for setting up a CliBuilder for loading/saving instances.
 *
 * @author Simon Templer
 */
class InstanceCLI {

  static void loadOptions(CliBuilder cli, String argName = 'data', String descr = 'Data to load') {
    cli._(longOpt: argName, args:1, argName:'file-or-URL', descr)
  }

  static InstanceCollection load(OptionAccessor options, TypeIndex schema, String argName = 'data') {
    def location = options."$argName"
    if (location) {
      URI loc = CLIUtil.fileOrUri(location)
      return load(loc, schema)
    }
    else {
      return null
    }
  }

  @CompileStatic
  static InstanceCollection load(URI loc, TypeIndex schema) {
    LocatableInputSupplier<? extends InputStream> sourceIn = new DefaultInputSupplier(loc)

    // create I/O provider
    InstanceReader instanceReader = null
    String customProvider = null
    if (customProvider != null) {
      // use specified provider
      instanceReader = HaleIO.createIOProvider(InstanceReader.class, null, customProvider);
      if (instanceReader == null) {
        fail("Could not find schema reader with ID " + customProvider);
      }
    }
    if (instanceReader == null) {
      // find applicable reader
      instanceReader = HaleIO.findIOProvider(InstanceReader.class, sourceIn, loc.getPath());
    }
    if (instanceReader == null) {
      throw fail("Could not determine instance reader to use for source data");
    }

    //TODO apply custom settings

    instanceReader.setSourceSchema(schema)
    instanceReader.setSource(sourceIn);

    IOReport report = instanceReader.execute(null)
    //TODO report?

    instanceReader.getInstances()
  }

  //TODO

  static void saveOptions(CliBuilder cli, String argName = 'target', String descr = 'Target location') {
    cli._(longOpt: argName, args:2, argName: 'file-or-URL> <providerId', descr)
  }

}
