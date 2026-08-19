
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
package to.wetransform.halecli.project

import static org.junit.Assert.*

import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import eu.esdihumboldt.hale.common.align.model.Alignment
import eu.esdihumboldt.hale.common.align.model.impl.DefaultAlignment
import eu.esdihumboldt.hale.common.core.io.project.model.Project
import eu.esdihumboldt.hale.common.core.io.supplier.FileIOSupplier
import eu.esdihumboldt.hale.common.core.report.Report
import eu.esdihumboldt.hale.common.core.report.ReportHandler
import eu.esdihumboldt.hale.common.schema.model.SchemaSpace
import eu.esdihumboldt.hale.common.schema.model.impl.DefaultSchemaSpace
import eu.esdihumboldt.util.nonosgi.Init

/**
 * Tests for saving a project as done by commands that load and save a project
 * (e.g. the migrate commands).
 *
 * @author Simon Templer
 */
class SaveProjectTest {

  @BeforeClass
  static void init() {
    Init.init()
  }

  @Rule
  public final TemporaryFolder folder = new TemporaryFolder()

  /**
   * Save a minimal project as .halex project.
   *
   * Reproduces the NullPointerException in SaveProjectAdvisor.findAdvisor
   * that occurs because project files other than the alignment (e.g. styles.sld
   * contributed by eu.esdihumboldt.hale.common.style) have no registered advisor.
   */
  @Test
  void testSaveProject() {
    File tempFolder = folder.newFolder()
    File targetFile = new File(tempFolder, 'saved.halex')

    Project project = new Project()
    Alignment alignment = new DefaultAlignment()
    SchemaSpace sourceSchema = new DefaultSchemaSpace()
    SchemaSpace targetSchema = new DefaultSchemaSpace()

    def reports = { Report report -> } as ReportHandler
    def output = new FileIOSupplier(targetFile)

    ProjectHelper.saveProject(project, alignment, sourceSchema, targetSchema,
      output, reports, 'halex', null)

    assertTrue(targetFile.exists())
    assertTrue(targetFile.length() > 0)
  }
}
