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
import eu.esdihumboldt.hale.common.schema.io.SchemaReader;
import eu.esdihumboldt.hale.common.schema.model.Schema
import eu.esdihumboldt.util.cli.CLIUtil
import groovy.transform.CompileStatic;;

/**
 * Common utility functions for setting up a CliBuilder for loading a schema.
 *
 * @author Simon Templer
 */
class SchemaCLI {

  static void loadSchemaOptions(CliBuilder cli, String argName = 'schema', String descr = 'Schema to load') {
    cli._(longOpt: argName, args:1, argName:'file-or-URL', descr)
  }

  static Schema loadSchema(OptionAccessor options, String argName = 'schema') {
    def location = options."$argName"
    if (location) {
      URI loc = CLIUtil.fileOrUri(location)
      return loadSchema(loc)
    }
    else {
      return null
    }
  }

  @CompileStatic
  static Schema loadSchema(URI loc) {
    LocatableInputSupplier<? extends InputStream> sourceIn = new DefaultInputSupplier(loc)

    // create I/O provider
    SchemaReader schemaReader = null
    String customProvider = null
    if (customProvider != null) {
      // use specified provider
      schemaReader = HaleIO.createIOProvider(SchemaReader.class, null, customProvider);
      if (schemaReader == null) {
        fail("Could not find schema reader with ID " + customProvider);
      }
    }
    if (schemaReader == null) {
      // find applicable reader
      schemaReader = HaleIO.findIOProvider(SchemaReader.class, sourceIn, loc.getPath());
    }
    if (schemaReader == null) {
      throw fail("Could not determine instance reader to use for source data");
    }

    //TODO apply custom settings

    schemaReader.setSource(sourceIn);

    IOReport report = schemaReader.execute(null)
    //TODO report?

    schemaReader.getSchema()
  }

}
