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

package to.wetransform.halecli.data

import static org.junit.Assert.*

import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.SystemOutRule

import to.wetransform.halecli.internal.Init
import eu.esdihumboldt.hale.common.core.HalePlatform
import eu.esdihumboldt.util.cli.Runner

/**
 * Tests for rewrite command.
 *
 * @author Simon Templer
 */
class RewriteCommandTest {

  @BeforeClass
  static void init() {
    Init.init()
  }

  @Rule
  public final SystemOutRule out = new SystemOutRule().enableLog()

  @Test
  void testSimpleRewrite() {

    def args = ['data', 'rewrite'];

    args << '--data'
    args << 'https://wetransform.box.com/shared/static/9sjicl1nmrxzxiapq1o2jefu5byz63j4.gml'

    args << '--schema'
    args << 'https://wetransform.box.com/shared/static/2gb9ifjjn0h08rogllm1undbbrhmwllz.xsd'
//    args << '--schema-reader'
//    args << 'eu.esdihumboldt.hale.io.xsd.reader'

    def targetFile = File.createTempFile('rewrite', '.gml')
    args << '--target'
    args << targetFile.absolutePath
    args << '--target-writer'
    args << 'eu.esdihumboldt.hale.io.gml.writer'
    args << '--target-setting'
    args << 'xml.pretty=true'

    try {
      int code = new Runner('hale').run(args as String[])

      // expecting a successful execution
      assertEquals(0, code)

      assertTrue(targetFile.exists())
      assertTrue(targetFile.size() > 0)
      //TODO check file content?

    } finally {
      targetFile.delete()
    }
  }

}
