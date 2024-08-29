
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
package to.wetransform.halecli;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.Collection;

import org.junit.Test;

import eu.esdihumboldt.hale.common.codelist.CodeList;
import eu.esdihumboldt.hale.common.codelist.io.CodeListReader;
import eu.esdihumboldt.hale.common.core.io.impl.LogProgressIndicator;
import eu.esdihumboldt.hale.common.core.io.report.IOReport;
import eu.esdihumboldt.hale.common.core.io.supplier.DefaultInputSupplier;
import eu.esdihumboldt.hale.io.codelist.skos.reader.SkosCodeListReader;

/**
 * Tests reading SKOS code lists to test shadowed dependencies in SKOS I/O
 * bundle.
 */
public class SKOSCodeListTest {

  @Test
  public void testSKOSFromRDF1() throws Exception {
    CodeList codeList = readCodeList(
        getClass().getClassLoader().getResource("testdata/skos/test1.rdf").toURI());

    Collection<CodeList.CodeEntry> entries = codeList.getEntries();
    assertFalse(entries.isEmpty());

    assertEquals(entries.size(), 1);

    assertNotNull(codeList.getLocation());
    assertNotNull(codeList.getIdentifier());

    for (CodeList.CodeEntry entry : entries) {
      assertEquals("Data scientist", entry.getName());
    }
  }

  private CodeList readCodeList(URI source) throws Exception {
    CodeListReader reader = new SkosCodeListReader();

    reader.setSource(new DefaultInputSupplier(source));

    IOReport report = reader.execute(new LogProgressIndicator());
    assertTrue(report.isSuccess());

    return reader.getCodeList();
  }

}
