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

package to.wetransform.halecli

import eu.esdihumboldt.util.nonosgi.Init

import static org.junit.Assert.*

import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.SystemOutRule

import eu.esdihumboldt.hale.common.core.HalePlatform
import eu.esdihumboldt.util.cli.Runner

/**
 * Simple tests for CLI runner.
 *
 * @author Simon Templer
 */
class RunnerTest {

  @BeforeClass
  static void init() {
    Init.init()
  }

  @Rule
  public final SystemOutRule out = new SystemOutRule().enableLog()

  @Test
  void testVersion() {
    int code = new Runner('hale').run('version')
    assertEquals(0, code)
    assertEquals(HalePlatform.coreVersion.toString(), out.log.trim())
  }

  @Test
  void testNoArgs() {
    int code = new Runner('hale').run()
    assertEquals(1, code)
    assertTrue(out.log.startsWith('usage:'))
  }

  @Test
  void testWrongCommands() {
    int code = new Runner('hale').run('zombie-apocalypse')
    assertEquals(1, code)
    assertTrue(out.log.startsWith('usage:'))
  }

  @Test
  void testHelp() {
    int code = new Runner('hale').run('help')
    assertEquals(0, code)
    assertTrue(out.log.startsWith('usage:'))
  }

  @Test
  void testTransformUsage() {
    int code = new Runner('hale').run('transform')
    assertEquals(0, code)
    assertTrue(out.log.trim().startsWith('Usage:'))
    assertTrue(out.log.contains('-project'))
  }

}
