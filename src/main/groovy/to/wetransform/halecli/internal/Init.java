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

package to.wetransform.halecli.internal;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.equinox.nonosgi.registry.RegistryFactoryHelper;
import org.slf4j.bridge.SLF4JBridgeHandler;

import groovy.lang.GroovySystem;

/**
 * Initializes hale functionality in a non-OSGi environment.
 * 
 * @author Simon Templer
 */
public class Init {
  
  private static AtomicBoolean initialized = new AtomicBoolean(false);
  
  public static void init() {
    if (initialized.compareAndSet(false, true)) {
      SLF4JBridgeHandler.install();
      
      // initialize registry
      RegistryFactoryHelper.getRegistry();
      
      // initialize meta extensions
      GroovySystem.getMetaClassRegistry().setMetaClassCreationHandle(new CustomMetaClassCreationHandle());
    }
  }

}
