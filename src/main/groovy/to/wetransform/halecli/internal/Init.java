package to.wetransform.halecli.internal;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.equinox.nonosgi.registry.RegistryFactoryHelper;

import groovy.lang.GroovySystem;

public class Init {
  
  private static AtomicBoolean initialized = new AtomicBoolean(false);
  
  public static void init() {
    if (initialized.compareAndSet(false, true)) {
      // initialize registry
      RegistryFactoryHelper.getRegistry();
      
      // initialize meta extensions
      GroovySystem.getMetaClassRegistry().setMetaClassCreationHandle(new CustomMetaClassCreationHandle());
    }
  }

}
