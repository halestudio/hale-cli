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
 *
 * Contributors:
 *     wetransform GmbH <http://www.wetransform.to>
 */

package to.wetransform.halecli.project.advisor;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import eu.esdihumboldt.hale.common.align.model.Alignment;
import eu.esdihumboldt.hale.common.core.HalePlatform;
import eu.esdihumboldt.hale.common.core.io.IOAdvisor;
import eu.esdihumboldt.hale.common.core.io.IOAdvisorRegister;
import eu.esdihumboldt.hale.common.core.io.impl.AbstractIOAdvisor;
import eu.esdihumboldt.hale.common.core.io.project.ProjectIO;
import eu.esdihumboldt.hale.common.core.io.project.ProjectWriter;
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
public class SaveProjectAdvisor extends AbstractIOAdvisor<ProjectWriter> implements IOAdvisorRegister {

  private final Map<String, IOAdvisor<?>> advisors = new HashMap<>();

  private final Project project;

  private final URI projectLoadLocation;

  public SaveProjectAdvisor(Project project, Alignment alignment,
      SchemaSpace sourceSchema, SchemaSpace targetSchema, URI projectLoadLocation) {
    super();
    this.project = project;
    this.projectLoadLocation = projectLoadLocation;

    advisors.put("eu.esdihumboldt.hale.io.align.write",
        new SaveAlignmentAdvisor(project, alignment, sourceSchema, targetSchema,
            projectLoadLocation));
  }

  @Override
  public void prepareProvider(ProjectWriter provider) {
    provider.setProject(project);
  }

  @Override
  public void updateConfiguration(ProjectWriter provider) {
    provider.getProject().setModified(new Date());
    provider.getProject().setHaleVersion(HalePlatform.getCoreVersion());
    Map<String, ProjectFile> projectFiles = ProjectIO
        .createDefaultProjectFiles(this);

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

  @Override
  public IOAdvisor<?> findAdvisor(String actionId, ServiceProvider serviceProvider) {
    IOAdvisor<?> advisor = advisors.get(actionId);
    advisor.setServiceProvider(serviceProvider); // not sure if this is needed here
    return advisor;
  }

}
