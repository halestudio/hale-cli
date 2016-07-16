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
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic;
import groovy.util.CliBuilder;
import groovy.util.OptionAccessor;
import to.wetransform.halecli.project.AbstractDeriveProjectCommand
import to.wetransform.halecli.project.AbstractDeriveProjectCommand.DeriveProjectResult

class FilterAlignmentCommand extends AbstractDeriveProjectCommand {

  final String shortDescription = 'Create a project copy with a filtered alignment'

  @Override
  void setupOptions(CliBuilder cli) {
    super.setupOptions(cli)
    
    cli._(longOpt: 'json-filter', args: 1, argName: 'json-file', required: true,
      'Specify a JSON file with the filter definition')
    cli._(longOpt: 'skip-empty', 'Specify to skip the project if the filtered alignment is empty')
    cli._(longOpt: 'skip-no-type-cells', 'Specify to skip the project if the filtered alignment contains no type cells')
  }
  
  DeriveProjectResult deriveProject(ProjectTransformationEnvironment projectEnv,
    OptionAccessor options) {
    
    def filterFile = options.'json-filter'
    if (filterFile) {
      filterFile = new File(filterFile)
      
      def filterDef = new JsonSlurper().parse(filterFile)
      
      Project project = projectEnv.project
      
      Alignment alignment = filterAlignment(projectEnv.alignment, filterDef, project)
      if (options.'skip-empty' && alignment.cells.empty) {
        println 'Skipping creating project, as the filtered alignment is empty'
        return null
      }
      
      if (options.'skip-no-type-cells' && alignment.typeCells.empty) {
        println 'Skipping creating project, as the filtered alignment contains no type cells'
        return null
      }
      
      // derived project
      return new DeriveProjectResult(project: project, alignment: alignment)
    }
    
    throw new IllegalStateException('No alignment filter definition provided')
  }
    
  Alignment filterAlignment(Alignment alignment, def filterDef, Project project) {
    ComplexConfigurationService conf = ProjectIO.createProjectConfigService(project)
    
    List<String> messages = []
    
    MutableAlignment result = new DefaultAlignment(alignment)
    def originalCells = new ArrayList<>(result.cells)
    originalCells.each { Cell cell ->
      result.removeCell(cell)
    }
    
    int removed = 0
    int retained = 0
    
    alignment.cells.each { Cell cell ->
      if (acceptCell(cell, filterDef, messages)) {
        result.addCell(cell)
        retained++
      }
      else {
        String cellTypes = cellTypesName(cell)
        messages << "Removed cell ${cell.id} (types $cellTypes)"
        removed++
      }
    }
    
    assert retained == result.cells.size()
    
    messages << "Removed $removed cells"
    messages << "Retained $retained cells from original project"
    
    ValueList msgList = new ValueList()
    messages.each {
      msgList << Value.simple(it)
    }
    conf.setProperty('derivedProjectLog', msgList as Value)
    
    println 'Alignment filter log:'
    messages.each {
      println "  $it"
    } 
    
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
      
      def misMatches = cell.source.values().findAll { Entity entity ->
        !matchesTypes(entity, sourceTypes)
      }
      
      if (!keep) {
        return false
      }
      else if (misMatches) {
        def name = entitiesTypesName(misMatches)
        messages << "Only partial source type match for cell ${cell.id} (also found types $name)"
      }
    }
    
    true
  }
  
  @CompileStatic
  String cellTypesName(Cell cell) {
    String sourceName
    if (cell.source) {
      sourceName = entitiesTypesName(cell.source.values())
    }
    
    String targetName
    if (cell.target) {
      targetName = entitiesTypesName(cell.target.values())
    }
    
    if (sourceName) {
      "$sourceName to $targetName"
    }
    else {
      "To $targetName"
    }
  }
  
  @CompileStatic
  String entitiesTypesName(Collection<? extends Entity> entities) {
    entities.collect { Entity entity ->
      entity.definition.type.displayName
    }.unique().join(', ')
  }

}
