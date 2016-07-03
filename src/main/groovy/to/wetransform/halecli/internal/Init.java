package to.wetransform.halecli.internal;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.equinox.nonosgi.registry.RegistryFactoryHelper;
import org.slf4j.bridge.SLF4JBridgeHandler;

import groovy.lang.GroovySystem;

public class Init {
  
  private static AtomicBoolean initialized = new AtomicBoolean(false);
  
  public static void init() {
    if (initialized.compareAndSet(false, true)) {
      SLF4JBridgeHandler.install();
      
      // initialize registry
      RegistryFactoryHelper.getRegistry();
      
      // initialize meta extensions
      GroovySystem.getMetaClassRegistry().setMetaClassCreationHandle(new CustomMetaClassCreationHandle());
    }
  }

}
