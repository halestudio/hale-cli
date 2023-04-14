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

package to.wetransform.halecli.project.alignment

import javax.xml.namespace.QName

import eu.esdihumboldt.cst.functions.groovy.GroovyJoin
import eu.esdihumboldt.hale.common.align.migrate.util.MigrationUtil;
import eu.esdihumboldt.hale.common.align.model.Alignment
import eu.esdihumboldt.hale.common.align.model.AlignmentUtil;
import eu.esdihumboldt.hale.common.align.model.BaseAlignmentCell;
import eu.esdihumboldt.hale.common.align.model.Cell
import eu.esdihumboldt.hale.common.align.model.CellUtil;
import eu.esdihumboldt.hale.common.align.model.Entity
import eu.esdihumboldt.hale.common.align.model.ModifiableCell;
import eu.esdihumboldt.hale.common.align.model.MutableAlignment
import eu.esdihumboldt.hale.common.align.model.MutableCell
import eu.esdihumboldt.hale.common.align.model.TransformationMode;
import eu.esdihumboldt.hale.common.align.model.functions.JoinFunction
import eu.esdihumboldt.hale.common.align.model.functions.join.JoinParameter
import eu.esdihumboldt.hale.common.align.model.impl.DefaultAlignment
import eu.esdihumboldt.hale.common.align.model.impl.DefaultCell;
import eu.esdihumboldt.hale.common.core.io.ExportProvider;
import eu.esdihumboldt.hale.common.core.io.Value
import eu.esdihumboldt.hale.common.core.io.ValueList;
import eu.esdihumboldt.hale.common.core.io.project.ComplexConfigurationService
import eu.esdihumboldt.hale.common.core.io.project.ProjectIO;
import eu.esdihumboldt.hale.common.core.io.project.model.Project
import eu.esdihumboldt.hale.common.headless.impl.ProjectTransformationEnvironment
import eu.esdihumboldt.hale.common.schema.SchemaSpaceID;
import eu.esdihumboldt.hale.common.schema.io.SchemaIO;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition
import eu.esdihumboldt.hale.io.xsd.constraint.XmlElements
import eu.esdihumboldt.hale.io.xsd.model.XmlElement;
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic;
import groovy.cli.picocli.CliBuilder;
import groovy.cli.picocli.OptionAccessor;
import to.wetransform.halecli.project.AbstractDeriveProjectCommand
import to.wetransform.halecli.project.AbstractDeriveProjectCommand.DeriveProjectResult

/**
 * Command creating a project with a filtered alignment.
 *
 * @author Simon Templer
 */
class FilterAlignmentCommand extends AbstractDeriveProjectCommand {

  final String shortDescription = 'Create a project copy with a filtered alignment'

  @Override
  void setupOptions(CliBuilder cli) {
    super.setupOptions(cli)

    cli._(longOpt: 'json-filter', args: 1, argName: 'json-file', required: true,
      'Specify a JSON file with the filter definition')
    cli.b(longOpt: 'use-base-alignment', 'Use the original alignment as base alignment instead of copying the cells')
    cli._(longOpt: 'skip-empty', 'Specify to skip the project if the filtered alignment is empty')
    cli._(longOpt: 'skip-no-type-cells', 'Specify to skip the project if the filtered alignment contains no type cells')
    cli._(longOpt: 'prepend-description', args: 1, argName: 'text', 'Text to prepend to the project description')
  }

  DeriveProjectResult deriveProject(ProjectTransformationEnvironment projectEnv,
    OptionAccessor options) {

    def filterFile = options.'json-filter'
    if (filterFile) {
      filterFile = new File(filterFile)

      def filterDef = new JsonSlurper().parse(filterFile)

      Project project = projectEnv.project

      def useBaseAlignment = !!options.'use-base-alignment'

      Alignment alignment = filterAlignment(projectEnv.alignment, filterDef, project, useBaseAlignment)
      if (options.'skip-empty' && alignment.cells.empty) {
        println 'Skipping creating project, as the filtered alignment is empty'
        return null
      }

      if (options.'skip-no-type-cells' && alignment.typeCells.empty) {
        println 'Skipping creating project, as the filtered alignment contains no type cells'
        return null
      }

      ComplexConfigurationService conf = ProjectIO.createProjectConfigService(project)

      // adapt mapping relevant source types
      if (filterDef && filterDef.sourceTypes) {
        String confName = SchemaIO.getMappingRelevantTypesParameterName(SchemaSpaceID.SOURCE)

        List<String> typeNames = conf.getList(confName)
        List<String> retain = []

        typeNames.each { String name ->
          QName qname = QName.valueOf(name)
          TypeDefinition typeDef = projectEnv.sourceSchema.getType(qname)
          if (typeDef && matchesTypes(typeDef, filterDef.sourceTypes)) {
            retain << name
          }
        }

        conf.setList(confName, retain)
      }

      // project adaptions
      if (options.'prepend-description') {
        if (project.description) {
          project.description = options.'prepend-description' + '\n\n' + project.description
        }
        else {
          project.description = options.'prepend-description'
        }
      }

      // derived project
      return new DeriveProjectResult(project: project, alignment: alignment)
    }

    throw new IllegalStateException('No alignment filter definition provided')
  }

  Alignment filterAlignment(Alignment alignment, def filterDef, Project project,
    boolean useBaseAlignment) {

    ComplexConfigurationService conf = ProjectIO.createProjectConfigService(project)

    List<String> messages = []

    MutableAlignment result

    if (useBaseAlignment) {
      //FIXME determine original alignment file name
      //XXX current implementation only works with .halex projects (and if the save configuration is correct)
      String projectLoc = project.getSaveConfiguration().getProviderConfiguration().get(ExportProvider.PARAM_TARGET)
      int lastSeparator = projectLoc.lastIndexOf('/')
      if (lastSeparator >= 0 && lastSeparator + 1 < projectLoc.size()) {
        projectLoc = projectLoc.substring(lastSeparator + 1)
      }
      URI location = URI.create("./${projectLoc}.alignment.xml")

      result = withBaseAlignment(alignment, location)

      int accepted = 0
      int rejected = 0

      result.cells.each { Cell cell ->
        if (cell instanceof ModifiableCell) {
          if (acceptCell(cell, filterDef, messages)) {
            accepted ++
          }
          else {
            String cellTypes = cellTypesName(cell)
            deactivate(cell)
            messages << "Deactivated alignment cell ${cell.id} (types $cellTypes)"
            rejected++
          }
        }
      }

      messages << "Retained $accepted accepted alignment cells"
      messages << "Deactivated $rejected rejected alignment cells"
    }
    else {
      result = new DefaultAlignment(alignment);

      // remove base alignment cells keeping custom functions
      MigrationUtil.removeBaseCells(result);
      // remove other cells
      result.clearCells();

      int removed = 0
      int retained = 0

      for (Cell cell : alignment.getCells()) {
        if (acceptCell(cell, filterDef, messages)) {
          // transfer cell unchanged
          MutableCell cellNew = new DefaultCell(cell);
          MigrationUtil.removeIdPrefix(cellNew, true, true);
          result.addCell(cellNew);
          retained++;
        }
        else {
          removed++;
        }
      }

      messages << "Removed $removed cells"
      messages << "Retained $retained cells from original project"
    }
    /*
    XXX case that retains base alignments contained in the original alignment
    not working properly currently
    else {
      result = new DefaultAlignment(alignment)
      //FIXME problem is that every cell in result then is a MutableCell,
      // because a DefaultCell is created in the copy constructor

      def originalCells = new ArrayList<>(result.cells)
      originalCells.each { Cell cell ->
        if (cell instanceof MutableCell) {
          result.removeCell(cell)
        }
      }

      int removed = 0
      int retained = 0
      int baseAccepted = 0
      int baseRejected = 0

      alignment.cells.each { Cell cell ->
        if (cell instanceof MutableCell) {
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
      }

      result.cells.each { Cell cell ->
        if (!(cell instanceof MutableCell) && (cell instanceof ModifiableCell)) {
          if (acceptCell(cell, filterDef, messages)) {
            baseAccepted ++
          }
          else {
            String cellTypes = cellTypesName(cell)
            deactivate(cell)
            messages << "Deactivated base alignment cell ${cell.id} (types $cellTypes)"
            baseRejected++
          }
        }
      }

      assert retained == result.cells.size()

      messages << "Removed $removed cells"
      messages << "Retained $retained editable cells from original project"
      if (baseAccepted) {
        messages << "Retained $baseAccepted accepted base alignment cells"
      }
      if (baseRejected) {
        messages << "Deactivated $baseRejected rejected base alignment cells"
      }
    }
    */

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

  /**
   * Create a new alignment with the given alignment as base alignment.
   * @param alignment the base alignment for the new alignment
   * @return a new alignment with only the base alignment added
   */
  MutableAlignment withBaseAlignment(Alignment alignment, URI location) {
    MutableAlignment result = new DefaultAlignment()

    String prefix = 'ba'

    def cells = alignment.cells.collect { ModifiableCell cell ->
      new BaseAlignmentCell(cell, location, prefix)
    }

    def baseFunctions = alignment.allCustomPropertyFunctions.values()

    result.addBaseAlignment(prefix, location, cells, baseFunctions)

    result
  }

  /**
   * Deactivate a modifiable cell.
   * @param cell the cell to deactivate
   */
  void deactivate(ModifiableCell cell) {
    if (AlignmentUtil.isTypeCell(cell)) {
      cell.transformationMode = TransformationMode.disabled
    }
  }

  boolean matchesTypes(Entity entity, Collection<String> types, boolean checkSubtypes = true) {
    matchesTypes(entity.definition.type, types, checkSubtypes)
  }

  boolean matchesTypes(TypeDefinition type, Collection<String> types, boolean checkSubtypes = true) {
    Deque<TypeDefinition> check = new ArrayDeque<>()
    check.push(type)

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

      if (checkSubtypes) {
        // add sub-types for check
        typeDef.subTypes.each {
          check.push(it)
        }
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
        // special case: Join or Groovy Join
        if (cell.transformationIdentifier) {
          switch (cell.transformationIdentifier) {
            case JoinFunction.ID:
            case GroovyJoin.ID:
              // check if mismatch is on first type in Join order -> then don't keep cell
              def joinParam = CellUtil.getFirstParameter(cell, JoinFunction.PARAMETER_JOIN).as(JoinParameter)
              if (joinParam && joinParam.getTypes()) {
                def firstType = joinParam.getTypes()[0]
                boolean notFound = misMatches.any { Entity entity ->
                  entity.definition == firstType
                }
                if (notFound) {
                  messages << "Cell ${cell.id} has a partial source type match but is dropped because the join focus (${firstType.definition.displayName}) is no match"
                  return false
                }
              }
            default:
              break
          }
        }

        def name = entitiesTypesName(misMatches)
        messages << "Only partial source type match for cell ${cell.id} (also found types $name)"
      }
    }

    // check excludes
    def reject = filterDef?.exclude?.any { Map excludeObj ->
      matchesCell(cell, excludeObj, messages)
    }
    if (reject) {
      return false
    }

    true
  }

  boolean matchesCell(Cell cell, Map excludeObj, List<String> messages) {
    if (excludeObj.typeCell) { // match type cells
      //TODO support different kinds of matching?
      // for now lax matching, but source and target need at least one match
      if (AlignmentUtil.isTypeCell(cell)) {
        def sourceTypes = excludeObj.typeCell.source
        boolean sourceMatch = !sourceTypes // matched if no source specified
        if (sourceTypes && cell.source) {
          if (!(sourceTypes instanceof List)) {
            sourceTypes = [sourceTypes]
          }

          sourceMatch = cell.source.values().any { Entity entity ->
            matchesTypes(entity, sourceTypes, false)
          }
        }

        def targetTypes = excludeObj.typeCell.target
        boolean targetMatch = !targetTypes // matched if no target specified
        if (targetTypes && cell.target) {
          if (!(targetTypes instanceof List)) {
            targetTypes = [targetTypes]
          }

          targetMatch = cell.target.values().any { Entity entity ->
            matchesTypes(entity, targetTypes, false)
          }
        }

        if (sourceMatch && targetMatch) {
          return true
        }

      }

    }
    else {
      // unknown definition
      messages << "Unrecognised exclude object $excludeObj"
    }

    false
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

  final boolean experimental = true

}
