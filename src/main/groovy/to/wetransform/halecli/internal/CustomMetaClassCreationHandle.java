package to.wetransform.halecli.internal;

import java.lang.reflect.Constructor;

import org.eclipse.equinox.nonosgi.registry.RegistryFactoryHelper;

import eu.esdihumboldt.util.groovy.meta.extension.MetaClassDescriptor;
import eu.esdihumboldt.util.groovy.meta.extension.MetaClassExtension;
import groovy.lang.MetaClass;
import groovy.lang.MetaClassRegistry;
import groovy.lang.MetaClassRegistry.MetaClassCreationHandle;

/**
 * Adapts created meta classes with delegating meta classes registered in the
 * {@link MetaClassExtension}.
 * 
 * @author Simon Templer
 */
public class CustomMetaClassCreationHandle extends MetaClassCreationHandle {

  private final MetaClassExtension ext;

  public CustomMetaClassCreationHandle() {
    // initialize registry
    RegistryFactoryHelper.getRegistry();
    
    ext = new MetaClassExtension();
  }
  
  @Override
  protected MetaClass createNormalMetaClass(@SuppressWarnings("rawtypes") Class theClass,
      MetaClassRegistry registry) {
    MetaClass metaClass = super.createNormalMetaClass(theClass, registry);

    for (MetaClassDescriptor descriptor : ext.getElements()) {
      if (descriptorApplies(descriptor, theClass)) {
        // create meta class
        Class<?> delegatingMetaClass = descriptor.getMetaClass();
        try {
          Constructor<?> constructor = delegatingMetaClass
              .getConstructor(MetaClass.class);
          metaClass = (MetaClass) constructor.newInstance(metaClass);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    return metaClass;
  }

  /**
   * Check if a meta class descriptor applies to a given class.
   * 
   * @param descriptor the meta class descriptor
   * @param theClass the class for which should be determined if the
   *            descriptor applies
   * @return <code>true</code> if the descriptor applies to the class,
   *         <code>false</code> otherwise
   */
  private boolean descriptorApplies(MetaClassDescriptor descriptor,
      @SuppressWarnings("rawtypes") Class theClass) {
    Class<?> forClass = descriptor.getForClass();
    if (descriptor.isForArray()) {
      if (theClass.isArray()) {
        Class<?> componentClass = theClass.getComponentType();
        if (componentClass != null) {
          return forClass.equals(componentClass)
              || forClass.isAssignableFrom(componentClass);
        }
        else {
          // should actually not happen
          return false;
        }
      }
      else {
        // no array
        return false;
      }
    }
    else {
      return forClass.equals(theClass) || forClass.isAssignableFrom(theClass);
    }
  }

}
