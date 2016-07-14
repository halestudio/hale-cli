package to.wetransform.halecli.project.advisor;

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

public class SaveProjectAdvisor extends AbstractIOAdvisor<ProjectWriter> implements IOAdvisorRegister {

  private final Map<String, IOAdvisor<?>> advisors = new HashMap<>();

  private final Project project;
  
  public SaveProjectAdvisor(Project project, Alignment alignment,
      SchemaSpace sourceSchema, SchemaSpace targetSchema) {
    super();
    this.project = project;
    
    advisors.put("eu.esdihumboldt.hale.io.align.write",
        new SaveAlignmentAdvisor(project, alignment, sourceSchema, targetSchema));
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
//    if (projectLocation != null) {
//      provider.setPreviousTarget(projectLocation);
//    }
  }

  @Override
  public IOAdvisor<?> findAdvisor(String actionId, ServiceProvider serviceProvider) {
    IOAdvisor<?> advisor = advisors.get(actionId);
    advisor.setServiceProvider(serviceProvider); // not sure if this is needed here
    return advisor;
  }
  
}
