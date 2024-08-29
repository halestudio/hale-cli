
/*
 * Copyright (c) 2024 wetransform GmbH
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

import static org.junit.Assert.*

import java.nio.file.Files
import java.nio.file.StandardCopyOption

import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.SystemOutRule
import org.junit.rules.TemporaryFolder

import eu.esdihumboldt.hale.common.core.HalePlatform
import eu.esdihumboldt.util.cli.Runner
import eu.esdihumboldt.util.io.IOUtils
import eu.esdihumboldt.util.nonosgi.Init
import groovy.xml.XmlUtil

/**
 * Simple tests for CLI runner.
 *
 * @author Simon Templer
 */
class TransformTest {

  @BeforeClass
  static void init() {
    Init.init()
  }

  @Rule
  public final SystemOutRule out = new SystemOutRule().enableLog()

  @Rule
  public final TemporaryFolder folder = new TemporaryFolder()

  @Test
  void testTransformExample() {
    File tempFolder = folder.newFolder()

    def projectFileName = 'hydroEx.halez'
    def sourceDataName = 'hydroEx_River_1Feature.gml'
    def expectedResultName = 'expected.gml'

    // copy files to folder
    [
      projectFileName,
      sourceDataName,
      expectedResultName
    ].each {fileName ->
      def resPath = "testdata/hydroex/$fileName"
      def targetFile = new File(tempFolder, fileName)

      getClass().getClassLoader().getResourceAsStream(resPath).withStream {
        Files.copy(it, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
      }
    }

    def projectFile = new File(tempFolder, projectFileName)
    def sourceFile = new File(tempFolder, sourceDataName)
    def targetFile = new File(tempFolder, 'target.gml')

    int code = new Runner('hale').run(
      'transform',
      '-project',
      projectFile.absolutePath,
      '-source',
      sourceFile.absolutePath,
      '-target',
      targetFile.absolutePath,
      '-providerId',
      'eu.esdihumboldt.hale.io.gml.writer'
      )
    assertEquals(0, code)

    assertTrue(out.log.contains('Transformation completed'))

    assertTrue(targetFile.exists())
    assertTrue(targetFile.length() > 0)

    def expectedResult = new File(tempFolder, expectedResultName)

    def expectedXml = new XmlSlurper().parse(expectedResult)
    def resultXml = new XmlSlurper().parse(targetFile)

    assertEquals(XmlUtil.serialize(expectedXml.featureMember), XmlUtil.serialize(resultXml.featureMember))
  }
}
