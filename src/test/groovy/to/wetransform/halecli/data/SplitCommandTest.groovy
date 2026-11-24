
/*
 * Copyright (c) 2026 wetransform GmbH
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution. If not, see <http://www.gnu.org/licenses/>.
 */
package to.wetransform.halecli.data

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import eu.esdihumboldt.util.cli.Runner
import eu.esdihumboldt.util.nonosgi.Init

/**
 * Tests for split command.
 */
class SplitCommandTest {

  private static final String SCHEMA_LOCATION = 'http://inspire.ec.europa.eu/schemas/hy-p/4.0/HydroPhysicalWaters.xsd'

  @BeforeClass
  static void init() {
    Init.init()
  }

  @Rule
  public final TemporaryFolder tmp = new TemporaryFolder()

  private List<String> splitArgs(File targetFolder, String data = 'testdata/inspire2.gml') {
    def args = ['data', 'split']

    args << '--data'
    args << getClass().getClassLoader().getResource(data).toString()

    args << '--schema'
    args << SCHEMA_LOCATION

    args << '--target'
    args << targetFolder.absolutePath

    args << '--threshold'
    args << '2'

    args
  }

  @Test
  void testSplit() {
    File targetFolder = tmp.newFolder()

    int code = new Runner('hale').run(splitArgs(targetFolder) as String[])

    // expecting a successful execution
    assertEquals(0, code)

    def parts = targetFolder.listFiles().findAll { it.name ==~ /part_\d+\.gml/ }
    def counts = parts.collect { new XmlSlurper().parse(it).featureMember.size() }

    // all instances are written, none more than once
    assertEquals(4, counts.sum())
    // threshold is respected
    assertTrue("Unexpected part sizes: $counts", counts.every { it <= 2 })
  }

  /**
   * Without the reference the features A1 and SW1 end up in different parts.
   * Keeping them together requires that references are resolved via the store.
   */
  @Test
  void testSplitKeepsReferencedInstancesTogether() {
    File targetFolder = tmp.newFolder()

    int code = new Runner('hale').run(
      splitArgs(targetFolder, 'testdata/inspire2-references.gml') as String[])

    // expecting a successful execution
    assertEquals(0, code)

    def parts = targetFolder.listFiles().findAll { it.name ==~ /part_\d+\.gml/ }
    // sorted lists of the feature IDs per part, an instance written twice would show up
    List<List<String>> partIds = parts.collect { part ->
      new XmlSlurper().parse(part).featureMember.collect { member ->
        member.children()[0].attributes().find { name, value -> name.endsWith('id') }?.value
      }.sort()
    }

    // A1 and the referenced SW1 are in the same part, every instance is written exactly once
    assertEquals([['A1', 'SW1'], ['A2', 'A3']] as Set, partIds as Set)
    assertEquals(2, partIds.size())
  }

  @Test
  void testSplitNoInstances() {
    File targetFolder = tmp.newFolder()

    def args = splitArgs(targetFolder)
    args << '--data-filter'
    args << "CQL:id = 'doesNotExist'"

    int code = new Runner('hale').run(args as String[])

    // expecting a successful execution
    assertEquals(0, code)

    assertEquals(0, targetFolder.listFiles().count { it.name.endsWith('.gml') })
  }
}
