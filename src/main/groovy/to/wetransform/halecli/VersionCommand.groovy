
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
 */
package to.wetransform.halecli

import eu.esdihumboldt.hale.common.core.HalePlatform
import eu.esdihumboldt.util.cli.Command
import eu.esdihumboldt.util.cli.CommandContext

/**
 * Command that prints the hale version.
 *
 * @author Simon Templer
 */
class VersionCommand implements Command {

  @Override
  int run(List<String> args, CommandContext context) {
    // print hale version
    println HalePlatform.coreVersion
    //TODO also include CLI version
    0
  }

  final String shortDescription = 'Print the hale version'
}
