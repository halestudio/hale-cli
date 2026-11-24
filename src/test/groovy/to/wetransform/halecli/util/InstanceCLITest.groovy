
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
package to.wetransform.halecli.util

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

import org.junit.BeforeClass
import org.junit.Test

import eu.esdihumboldt.hale.common.core.report.impl.DefaultReporter
import eu.esdihumboldt.hale.common.instance.model.Instance
import eu.esdihumboldt.hale.common.instance.model.InstanceCollection
import eu.esdihumboldt.hale.common.instance.model.ResourceIterator
import eu.esdihumboldt.hale.common.instance.model.impl.DefaultInstance
import eu.esdihumboldt.hale.common.instance.model.impl.DefaultInstanceCollection
import eu.esdihumboldt.hale.common.instance.store.InstanceStore
import eu.esdihumboldt.hale.common.schema.model.Schema
import eu.esdihumboldt.util.nonosgi.Init

/**
 * Tests for instance helpers.
 */
class InstanceCLITest {

  private static final URI SCHEMA_LOCATION = URI.create('http://inspire.ec.europa.eu/schemas/hy-p/4.0/HydroPhysicalWaters.xsd')

  private static Schema schema

  @BeforeClass
  static void init() {
    Init.init()

    schema = SchemaCLI.loadSchema(SCHEMA_LOCATION, [:], null, null)
  }

  @Test
  void testLoadTempDatabase() {
    URI data = InstanceCLITest.classLoader.getResource('testdata/inspire2.gml').toURI()
    InstanceCollection source = InstanceCLI.load(data, [:], null, schema, null)

    InstanceStore store = InstanceCLI.loadTempDatabase(source, schema)
    try {
      int count = 0
      ResourceIterator<Instance> it = store.getInstances(schema).iterator()
      try {
        while (it.hasNext()) {
          it.next()
          count++
        }
      } finally {
        it.close()
      }

      assertEquals(4, count)
    } finally {
      store.close()
    }
  }

  @Test
  void testLoadTempDatabaseFailure() {
    InstanceCollection failing = new DefaultInstanceCollection() {

        @Override
        ResourceIterator<Instance> iterator() {
          throw new RuntimeException('Simulated failure reading the source')
        }
      }

    assertFailsAndCleansUp(failing)
  }

  @Test
  void testLoadTempDatabaseUnstorableInstance() {
    // an instance without type definition cannot be stored
    InstanceCollection unstorable = new DefaultInstanceCollection([
      new DefaultInstance(null, null)
    ])

    IllegalStateException e = assertFailsAndCleansUp(unstorable)

    // the reason is part of the message, unless listing report messages is disabled
    boolean messagesListed = DefaultReporter.MESSAGE_CAP != 0
    if (messagesListed) {
      assertTrue("Unexpected message: ${e.message}", e.message.contains('could not be stored'))
    }
  }

  private static IllegalStateException assertFailsAndCleansUp(InstanceCollection instances) {
    Set<String> before = storeDirs()

    IllegalStateException failure = null
    try {
      InstanceStore store = InstanceCLI.loadTempDatabase(instances, schema)
      store.close()
    } catch (IllegalStateException e) {
      failure = e
    }
    assertNotNull('Expected loading the instances to fail', failure)

    // the store was closed and its directory deleted
    assertEquals(before, storeDirs())

    failure
  }

  private static Set<String> storeDirs() {
    new File(System.getProperty('java.io.tmpdir')).list().findAll {
      it.startsWith('hale-cli-store')
    } as Set
  }
}
