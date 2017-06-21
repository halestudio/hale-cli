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

package to.wetransform.halecli.util;

import static eu.esdihumboldt.hale.app.transform.ExecUtil.fail;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Map;

import eu.esdihumboldt.hale.common.core.io.ExportProvider;
import eu.esdihumboldt.hale.common.core.io.HaleIO;
import eu.esdihumboldt.hale.common.core.io.ImportProvider;
import eu.esdihumboldt.hale.common.core.io.Value;
import eu.esdihumboldt.hale.common.core.io.supplier.DefaultInputSupplier;
import eu.esdihumboldt.hale.common.core.io.supplier.FileIOSupplier;
import eu.esdihumboldt.hale.common.core.io.supplier.LocatableInputSupplier;
import eu.esdihumboldt.hale.common.core.io.supplier.LocatableOutputSupplier;
import eu.esdihumboldt.hale.common.core.io.supplier.NoStreamOutputSupplier;
import eu.esdihumboldt.util.Pair;

/**
 * General HaleIO helper functions
 *
 * @author Simon Templer
 */
public class HaleIOHelper {

  public static <T extends ExportProvider> T prepareWriter(String providerId,
      Class<T> providerClass, Map<String, String> settings, URI targetLoc) {
    // create I/O provider
    // use specified provider
    T writer = HaleIO.createIOProvider(providerClass, null, providerId);
    if (writer == null) {
      fail("Could not find export provider with ID " + providerId);
    }

    if (targetLoc != null) {
      LocatableOutputSupplier<? extends OutputStream> target = null;

      try {
        target = new FileIOSupplier(new File(targetLoc));
      } catch (Exception e) {
        // ignore
      }

      if (target == null) {
        // fall back to no stream output supplier
        target = new NoStreamOutputSupplier(targetLoc);
      }

      writer.setTarget(target);
    }

    // apply custom settings
    settings.forEach((setting, value) -> {
      writer.setParameter(setting, Value.simple(value));
    });

    return writer;
  }

  public static <T extends ImportProvider> Pair<T, String> prepareReader(URI loc,
      Class<T> providerClass, Map<String, String> settings, String customProvider) {
    LocatableInputSupplier<? extends InputStream> sourceIn = new DefaultInputSupplier(loc);

    // create I/O provider
    T reader = null;
    String providerId = null;
    if (customProvider != null) {
      // use specified provider
      reader = HaleIO.createIOProvider(providerClass, null, customProvider);
      if (reader == null) {
        fail("Could not find import provider with ID " + customProvider);
      }
      providerId = customProvider;
    }
    if (reader == null) {
      // find applicable reader
      Pair<T, String> providerInfo = HaleIO.findIOProviderAndId(providerClass, sourceIn, loc.getPath());
      if (providerInfo != null) {
        reader = providerInfo.getFirst();
        providerId = providerInfo.getSecond();
      }
    }
    if (reader == null) {
      throw fail("Could not determine import provider to use for source");
    }

    final T finalReader = reader;

    // apply custom settings
    settings.forEach((setting, value) -> {
      finalReader.setParameter(setting, Value.simple(value));
    });

    reader.setSource(sourceIn);

    return new Pair<>(reader, providerId);
  }

}
