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

package to.wetransform.halecli.data

import java.util.Iterator;
import java.util.List

import eu.esdihumboldt.hale.common.instance.graph.reference.ReferenceGraph;
import eu.esdihumboldt.hale.common.instance.graph.reference.impl.XMLInspector;
import eu.esdihumboldt.hale.common.instance.model.InstanceCollection
import eu.esdihumboldt.hale.common.schema.model.Schema;
import eu.esdihumboldt.util.cli.Command
import eu.esdihumboldt.util.cli.CommandContext
import to.wetransform.halecli.util.InstanceCLI;
import to.wetransform.halecli.util.SchemaCLI;;;

/**
 * @author simon
 *
 */
class SplitCommand implements Command {

  @Override
  public int run(List<String> args, CommandContext context) {
    CliBuilder cli = new CliBuilder(usage : "${context.baseCommand} [options] [...]")

    cli._(longOpt: 'help', 'Show this help')

    //TODO more options

    // options for schema
    SchemaCLI.loadSchemaOptions(cli)

    // options for source data
    InstanceCLI.loadOptions(cli)

    // options for target data
    InstanceCLI.saveOptions(cli)

    OptionAccessor options = cli.parse(args)

    if (options.help) {
      cli.usage()
      return 0
    }

    // handle schema
    Schema schema = SchemaCLI.loadSchema(options)

    // handle source data
    InstanceCollection source = InstanceCLI.load(options, schema)

    // create a reference graph
    ReferenceGraph<String> rg = new ReferenceGraph<String>(new XMLInspector(),
        source)

    // partition the graph
    int threshold = 10000
    Iterator<InstanceCollection> parts = rg.partition(threshold);

    //FIXME
    int partCount = 0
    while (parts.hasNext()) {
      def size = parts.next().size()
      if (size >= 0) {
        println "Part with $size instances"
      }
      else {
        println "Part with undefined size"
      }
      partCount++
    }
    println "Total $partCount parts"

    //TODO write target

    // TODO Auto-generated method stub
    return 0
  }

  final String shortDescription = 'Split a source file into portions'

  final boolean experimental = true

}
