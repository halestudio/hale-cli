
/*
 * Copyright (c) 2016 wetransform GmbH
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution. If not, see <http://www.gnu.org/licenses/>.
 */
package to.wetransform.halecli.project.advisor;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.igd.eclipse.util.extension.ExtensionObjectFactoryCollection;
import de.fhg.igd.eclipse.util.extension.FactoryFilter;
import eu.esdihumboldt.hale.common.align.model.Alignment;
import eu.esdihumboldt.hale.common.core.HalePlatform;
import eu.esdihumboldt.hale.common.core.io.IOAdvisor;
import eu.esdihumboldt.hale.common.core.io.IOAdvisorRegister;
import eu.esdihumboldt.hale.common.core.io.extension.IOAdvisorExtension;
import eu.esdihumboldt.hale.common.core.io.extension.IOAdvisorFactory;
import eu.esdihumboldt.hale.common.core.io.impl.AbstractIOAdvisor;
import eu.esdihumboldt.hale.common.core.io.project.ProjectIO;
import eu.esdihumboldt.hale.common.core.io.project.ProjectWriter;
import eu.esdihumboldt.hale.common.core.io.project.extension.ProjectFileExtension;
import eu.esdihumboldt.hale.common.core.io.project.model.AdvisorProjectFile;
import eu.esdihumboldt.hale.common.core.io.project.model.Project;
import eu.esdihumboldt.hale.common.core.io.project.model.ProjectFile;
import eu.esdihumboldt.hale.common.core.service.ServiceProvider;
import eu.esdihumboldt.hale.common.schema.model.SchemaSpace;

/**
 * Headless advisor for saving a project.
 *
 * @author Simon Templer
 */
public class SaveProjectAdvisor extends AbstractIOAdvisor<ProjectWriter>
    implements IOAdvisorRegister {

  private static final Logger log = LoggerFactory.getLogger(SaveProjectAdvisor.class);

  private final Map<String, IOAdvisor<?>> advisors = new HashMap<>();

  private final Project project;

  private final URI projectLoadLocation;

  public SaveProjectAdvisor(Project project, Alignment alignment, SchemaSpace sourceSchema,
      SchemaSpace targetSchema, URI projectLoadLocation) {
    super();
    this.project = project;
    this.projectLoadLocation = projectLoadLocation;

    advisors.put("eu.esdihumboldt.hale.io.align.write", new SaveAlignmentAdvisor(project, alignment,
        sourceSchema, targetSchema, projectLoadLocation));
  }

  @Override
  public void prepareProvider(ProjectWriter provider) {
    provider.setProject(project);
  }

  @Override
  public void updateConfiguration(ProjectWriter provider) {
    provider.getProject().setModified(new Date());
    provider.getProject().setHaleVersion(HalePlatform.getCoreVersion());
    Map<String, ProjectFile> projectFiles = ProjectIO.createDefaultProjectFiles(this);

    // only keep project files that can be saved in this context (i.e. that
    // have a save advisor available), e.g. styles.sld has none headlessly
    projectFiles.entrySet().removeIf(entry -> {
      if (!(entry.getValue() instanceof AdvisorProjectFile)) {
        // project file does not need an I/O advisor for saving
        return false;
      }
      String saveActionId = findSaveActionId(entry.getKey());
      boolean supported = saveActionId != null && hasSaveAdvisor(saveActionId);
      if (!supported) {
        log.warn("Project file '{}' has no I/O advisor for saving available and will be skipped.",
            entry.getKey());
      }
      return !supported;
    });

    for (ProjectFile pf : projectFiles.values()) {
      if (pf instanceof AdvisorProjectFile) {
        ((AdvisorProjectFile) pf).setAdvisorRegister(this);
      }
    }

    provider.setProjectFiles(projectFiles);
    if (projectLoadLocation != null) {
      provider.setPreviousTarget(projectLoadLocation);
    }
  }

  /**
   * Determine the action ID for saving the project file with the given name, as
   * declared at the project file extension point.
   *
   * @param fileName the project file name
   * @return the save action ID or <code>null</code> if it cannot be determined
   */
  private String findSaveActionId(String fileName) {
    for (IConfigurationElement element : Platform.getExtensionRegistry()
        .getConfigurationElementsFor(ProjectFileExtension.ID)) {
      if ("action-file".equals(element.getName())
          && fileName.equals(element.getAttribute("name"))) {
        IConfigurationElement[] save = element.getChildren("save");
        if (save != null && save.length > 0) {
          return save[0].getAttribute("action");
        }
      }
    }
    return null;
  }

  /**
   * Determine if an I/O advisor for the given save action is available, either
   * from the advisors managed here or from the advisor extension point.
   *
   * @param saveActionId the ID of the action for saving the project file
   * @return if an advisor is available
   */
  private boolean hasSaveAdvisor(String saveActionId) {
    if (advisors.containsKey(saveActionId)) {
      return true;
    }
    List<IOAdvisorFactory> factories = IOAdvisorExtension.getInstance()
        .getFactories(new FactoryFilter<IOAdvisor<?>, IOAdvisorFactory>() {

          @Override
          public boolean acceptFactory(IOAdvisorFactory factory) {
            return saveActionId.equals(factory.getActionID());
          }

          @Override
          public boolean acceptCollection(
              ExtensionObjectFactoryCollection<IOAdvisor<?>, IOAdvisorFactory> collection) {
            return true;
          }
        });
    return factories != null && !factories.isEmpty();
  }

  @Override
  public IOAdvisor<?> findAdvisor(String actionId, ServiceProvider serviceProvider) {
    IOAdvisor<?> advisor = advisors.get(actionId);
    if (advisor == null) {
      // fall back to the advisor extension (the default register)
      return IOAdvisorExtension.getInstance().findAdvisor(actionId, serviceProvider);
    }
    advisor.setServiceProvider(serviceProvider); // not sure if this is needed here
    return advisor;
  }

}
