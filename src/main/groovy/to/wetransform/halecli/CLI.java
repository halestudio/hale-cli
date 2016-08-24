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

package to.wetransform.halecli;

import eu.esdihumboldt.util.cli.Runner;
import to.wetransform.halecli.internal.Init;

/**
 * hale CLI main class.
 *
 * @author Simon Templer
 */
public class CLI {

  public static void main(String[] args) {
    // initialize hale in non-OSGi environment
    Init.init();

    Runner runner = new Runner("hale");
    int returnCode = runner.run(args);

    if (returnCode != 0) {
      System.exit(returnCode);
    }
  }

}
