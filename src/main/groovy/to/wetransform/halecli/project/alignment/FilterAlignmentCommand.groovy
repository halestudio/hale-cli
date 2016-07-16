package to.wetransform.halecli.project.alignment

import eu.esdihumboldt.hale.common.align.model.Alignment
import eu.esdihumboldt.hale.common.align.model.Cell
import eu.esdihumboldt.hale.common.align.model.Entity;
import eu.esdihumboldt.hale.common.align.model.MutableAlignment
import eu.esdihumboldt.hale.common.align.model.impl.DefaultAlignment
import eu.esdihumboldt.hale.common.core.io.Value
import eu.esdihumboldt.hale.common.core.io.ValueList;
import eu.esdihumboldt.hale.common.core.io.project.ComplexConfigurationService
import eu.esdihumboldt.hale.common.core.io.project.ProjectIO;
import eu.esdihumboldt.hale.common.core.io.project.model.Project
import eu.esdihumboldt.hale.common.headless.impl.ProjectTransformationEnvironment
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition
import eu.esdihumboldt.hale.io.xsd.constraint.XmlElements
import eu.esdihumboldt.hale.io.xsd.model.XmlElement;
import groovy.json.JsonSlurper;
import groovy.util.CliBuilder;
import groovy.util.OptionAccessor;
import to.wetransform.halecli.project.AbstractDeriveProjectCommand
import to.wetransform.halecli.project.AbstractDeriveProjectCommand.DeriveProjectResult

class FilterAlignmentCommand extends AbstractDeriveProjectCommand {

  final String shortDescription = 'Create a project copy with a filtered alignment'

  @Override
  void setupOptions(CliBuilder cli) {
    super.setupOptions(cli)
    
    cli._(longOpt: 'json-filter', args: 1, argName: 'json-file',
      'Specify a JSON file with the filter definition')
  }
  
  DeriveProjectResult deriveProject(ProjectTransformationEnvironment projectEnv,
    OptionAccessor options) {
    
    def filterFile = options.'json-filter'
    if (filterFile) {
      filterFile = new File(filterFile)
      
      def filterDef = new JsonSlurper().parse(filterFile)
      
      Project project = projectEnv.project
      
      Alignment alignment = filterAlignment(projectEnv.alignment, filterDef, project)
      
      // derived project
      return new DeriveProjectResult(project: project, alignment: alignment)
    }
    
    // fall-back
    new DeriveProjectResult(project: projectEnv.project, alignment: projectEnv.alignment)
  }
    
  Alignment filterAlignment(Alignment alignment, def filterDef, Project project) {
    ComplexConfigurationService conf = ProjectIO.createProjectConfigService(project)
    
    List<String> messages = []
    
    MutableAlignment result = new DefaultAlignment()
    
    alignment.cells.each { cell ->
      if (acceptCell(cell, filterDef, messages)) {
        result.addCell(cell)
      }
    }
    
    ValueList msgList = new ValueList()
    messages.each {
      msgList << Value.simple(it)
    }
    conf.setProperty('derivedProjectLog', msgList as Value) 
    
    result
  }
  
  boolean matchesTypes(Entity entity, Collection<String> types) {
    Deque<TypeDefinition> check = new ArrayDeque<>()
    check.push(entity.definition.type)
    
    while (!check.isEmpty()) {
      TypeDefinition typeDef = check.poll()
      
      String localName = typeDef.name.localPart
      if (types.contains(localName)) {
        return true
      }
      
      Set<XmlElement> elements = typeDef.getConstraint(XmlElements).elements
      boolean elementMatch = elements.any { XmlElement element ->
        String elementName = element.name.localPart
        if (types.contains(elementName)) {
          true
        }
        else {
          false
        }
      }
      if (elementMatch) {
        return true
      }
      
      // add sub-types for check
      typeDef.subTypes.each {
        check.push(it)
      }
    }
    
    false
  }
  
  boolean acceptCell(Cell cell, def filterDef, List<String> messages) {
    // check source types
    def sourceTypes = filterDef.sourceTypes
    if (sourceTypes && cell.source) {
      sourceTypes = new HashSet<>(sourceTypes)
      
      boolean keep = cell.source.values().any { Entity entity ->
        matchesTypes(entity, sourceTypes)
      }
      
      boolean partial = cell.source.values().any { Entity entity ->
        !matchesTypes(entity, sourceTypes)
      }
      
      if (!keep) {
        return false
      }
      else if (partial) {
        messages << "Only partial source type match for cell ${cell.id}"
      }
    }
    
    true
  }

}
