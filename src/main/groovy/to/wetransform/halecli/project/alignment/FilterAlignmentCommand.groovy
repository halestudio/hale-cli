package to.wetransform.halecli.project.alignment

import eu.esdihumboldt.hale.common.align.model.Alignment
import eu.esdihumboldt.hale.common.core.io.project.model.Project
import eu.esdihumboldt.hale.common.headless.impl.ProjectTransformationEnvironment
import groovy.util.OptionAccessor;
import to.wetransform.halecli.project.AbstractModifyProjectCommand
import to.wetransform.halecli.project.AbstractModifyProjectCommand.ModifyProjectResult;;

class FilterAlignmentCommand extends AbstractModifyProjectCommand {

  final String shortDescription = 'Create a project copy with a filtered alignment'

  ModifyProjectResult modifyProject(ProjectTransformationEnvironment projectEnv,
    OptionAccessor options) {
    
    //TODO actually do some filtering
    
    new ModifyProjectResult(project: projectEnv.project, alignment: projectEnv.alignment)
  }

}
