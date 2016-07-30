package to.wetransform.halecli

import static org.junit.Assert.*

import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.SystemOutRule

import to.wetransform.halecli.internal.Init
import eu.esdihumboldt.hale.common.core.HalePlatform
import eu.esdihumboldt.util.cli.Runner

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
