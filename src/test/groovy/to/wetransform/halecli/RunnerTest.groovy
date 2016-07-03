package to.wetransform.halecli

import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.SystemOutRule

import eu.esdihumboldt.hale.common.core.HalePlatform
import to.wetransform.halecli.internal.Init

import static org.junit.Assert.*

import org.junit.BeforeClass

class RunnerTest {
  
  @BeforeClass
  static void init() {
    Init.init()
  }
  
  @Rule
  public final SystemOutRule out = new SystemOutRule().enableLog()
  
  @Test
  void testVersion() {
    int code = new Runner().run('version')
    assertEquals(0, code)
    assertEquals(HalePlatform.coreVersion.toString(), out.log.trim())
  }
  
  @Test
  void testNoArgs() {
    int code = new Runner().run()
    assertEquals(0, code)
    assertTrue(out.log.startsWith('usage:'))
  }
  
  @Test
  void testWrongCommands() {
    int code = new Runner().run('zombie-apocalypse')
    assertEquals(1, code)
    assertTrue(out.log.startsWith('usage:'))
  }
  
  @Test
  void testTransformUsage() {
    int code = new Runner().run('transform')
    assertEquals(0, code)
    assertTrue(out.log.trim().startsWith('Usage:'))
    assertTrue(out.log.contains('-project'))
  }

}
