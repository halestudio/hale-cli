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

import eu.esdihumboldt.util.cli.Runner
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.SystemOutRule
import to.wetransform.halecli.internal.Init

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

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

  @Test
  void testRewriteGuessSchema() {

    def args = ['data', 'rewrite'];

    args << '--data'
    args << getClass().getClassLoader().getResource("testdata/inspire.gml")

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

  @Test
  void testRewriteNoPassthrough() {

    def args = ['data', 'rewrite'];

    args << '--data'
    args << getClass().getClassLoader().getResource("testdata/inspire.gml")

    def targetFile = File.createTempFile('rewrite', '.gml')
    args << '--target'
    args << targetFile.absolutePath
    args << '--target-writer'
    args << 'eu.esdihumboldt.hale.io.wfs.fc.write-2.0'
    args << '--target-setting'
    args << 'skipFeatureCount=false'

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

  @Test
  void testRewriteFilter() {

    def args = ['data', 'rewrite'];

    args << '--data'
    args << getClass().getClassLoader().getResource("testdata/inspire2.gml")

    args << '--data-filter'
    args << "CQL:id = 'A1'"

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

      def xml = new XmlSlurper().parse(targetFile)
      def objects = xml.featureMember

      assertEquals(1, objects.size())
    } finally {
      targetFile.delete()
    }
  }

  @Test
  void testRewriteFilterList() {

    def args = ['data', 'rewrite'];

    args << '--data'
    args << getClass().getClassLoader().getResource("testdata/inspire2.gml")

    args << '--data-filter'
    args << "CQL:id = 'SW1'"

    args << '--data-filter'
    args << "CQL:id = 'A1'"

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

      def xml = new XmlSlurper().parse(targetFile)
      def objects = xml.featureMember

      assertEquals(2, objects.size())
      def wcs = objects.Watercourse
      assertEquals(1, wcs.size())
      def sws = objects.StandingWater
      assertEquals(1, sws.size())
    } finally {
      targetFile.delete()
    }
  }

  @Test
  void testRewriteExcludeType() {

    def args = ['data', 'rewrite'];

    args << '--data'
    args << getClass().getClassLoader().getResource("testdata/inspire2.gml")

    args << '--data-exclude-type'
    args << 'Watercourse'

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

      def xml = new XmlSlurper().parse(targetFile)
      def objects = xml.featureMember

      assertEquals(1, objects.size())
      def wcs = objects.Watercourse
      assertEquals(0, wcs.size())
      def sws = objects.StandingWater
      assertEquals(1, sws.size())
    } finally {
      targetFile.delete()
    }
  }

  @Ignore('Does not succeed due to bug in hale InstanceFilterDefinition - exclude is ignored if no other filter is present')
  @Test
  void testRewriteExclude() {

    def args = ['data', 'rewrite'];

    args << '--data'
    args << getClass().getClassLoader().getResource("testdata/inspire2.gml")

    args << '--data-exclude'
    args << "\"id\" = 'A2'"

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

      def xml = new XmlSlurper().parse(targetFile)
      def objects = xml.featureMember

      assertEquals(3, objects.size())
      def wcs = objects.Watercourse
      assertEquals(2, wcs.size())
      def sws = objects.StandingWater
      assertEquals(1, sws.size())
    } finally {
      targetFile.delete()
    }
  }

  @Test
  void testRewriteExcludeWorkaround() {

    def args = ['data', 'rewrite'];

    args << '--data'
    args << getClass().getClassLoader().getResource("testdata/inspire2.gml")

    args << '--data-exclude'
    args << "\"id\" = 'A2'"

    args << '--data-exclude-type'
    args << 'DoesNotExist'

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

      def xml = new XmlSlurper().parse(targetFile)
      def objects = xml.featureMember

      assertEquals(3, objects.size())
      def wcs = objects.Watercourse
      assertEquals(2, wcs.size())
      def sws = objects.StandingWater
      assertEquals(1, sws.size())
    } finally {
      targetFile.delete()
    }
  }

  @Test
  void testRewriteFilterContext() {

    def args = ['data', 'rewrite'];

    args << '--data'
    args << getClass().getClassLoader().getResource("testdata/inspire2.gml")

    args << '--data-filter'
    args << '''groovy:
      def type = instance.definition.name
      boolean rejected = false
      if (type) {
        withContext { c ->
          def typeMap = c.typeCounts
          if (!typeMap) {
            typeMap = [:]
            c.typeCounts = typeMap
          }
          def count = typeMap[type] ?: 0
          if (count >= 2) { // only keep max 2 per type
            rejected = true
          }
          typeMap[type] = count + 1
        }
      }
      !rejected
    '''

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

      def xml = new XmlSlurper().parse(targetFile)
      def objects = xml.featureMember

      assertEquals(3, objects.size())
      def wcs = objects.Watercourse
      assertEquals(2, wcs.size())
      def sws = objects.StandingWater
      assertEquals(1, sws.size())
    } finally {
      targetFile.delete()
    }
  }

}
