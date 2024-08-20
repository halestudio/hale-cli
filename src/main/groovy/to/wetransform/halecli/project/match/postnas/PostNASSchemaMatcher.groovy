
/*
 * Copyright (c) 2017 wetransform GmbH
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution. If not, see <http://www.gnu.org/licenses/>.
 */
package to.wetransform.halecli.project.match.postnas

import javax.xml.namespace.QName

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.ListMultimap

import eu.esdihumboldt.hale.common.align.groovy.accessor.EntityAccessor
import eu.esdihumboldt.hale.common.align.model.Alignment
import eu.esdihumboldt.hale.common.align.model.AlignmentUtil
import eu.esdihumboldt.hale.common.align.model.Entity
import eu.esdihumboldt.hale.common.align.model.EntityDefinition
import eu.esdihumboldt.hale.common.align.model.MutableAlignment
import eu.esdihumboldt.hale.common.align.model.MutableCell
import eu.esdihumboldt.hale.common.align.model.functions.RenameFunction
import eu.esdihumboldt.hale.common.align.model.functions.RetypeFunction
import eu.esdihumboldt.hale.common.align.model.impl.DefaultAlignment
import eu.esdihumboldt.hale.common.align.model.impl.DefaultCell
import eu.esdihumboldt.hale.common.align.model.impl.DefaultProperty
import eu.esdihumboldt.hale.common.align.model.impl.DefaultType
import eu.esdihumboldt.hale.common.align.model.impl.PropertyEntityDefinition
import eu.esdihumboldt.hale.common.align.model.impl.TypeEntityDefinition
import eu.esdihumboldt.hale.common.schema.SchemaSpaceID
import eu.esdihumboldt.hale.common.schema.model.ChildDefinition
import eu.esdihumboldt.hale.common.schema.model.DefinitionUtil
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition
import eu.esdihumboldt.hale.common.schema.model.TypeIndex
import groovy.transform.CompileStatic
import to.wetransform.halecli.project.match.SchemaMatcher

/**
 * Schema matcher for PostNAS that uses information available in target schema
 * comments to determine the respective element in the reference schema.
 *
 * @author Simon Templer
 */
@CompileStatic
class PostNASSchemaMatcher implements SchemaMatcher {

  @Override
  public Alignment generateSchemaMatching(TypeIndex refSchema, TypeIndex targetSchema) {
    /*
     * First collect type names of reference types matching target types
     */

    // reference types to target types
    Map<QName, QName> typeMap = [:]
    // type infos for each target type
    Map<QName, Map> targetTypeInfo = [:]
    // candidate names for reference types to target types
    Map<String, QName> targetTypeCandidates = [:]

    def targetTypes = (Collection<TypeDefinition>) targetSchema.getMappingRelevantTypes()
    targetTypes.each {
      // try to determine reference name (and other information) from description
      def typeInfo = parseTypeInfo(it.description)

      def candidateName
      if (typeInfo) {
        //XXX DEBUG
        //        println "Type $it.name"
        //        println typeInfo

        targetTypeInfo[it.name] = typeInfo

        if (typeInfo.FeatureType) {
          candidateName = typeInfo.FeatureType
        }
      }
      else {
        //TODO do default matching? - right now rely on type info
        // candidateName = it.name.localPart.toLowerCase()
      }

      if (candidateName) {
        targetTypeCandidates[candidateName] = it.name
      }
    }

    // iterate through reference types and collect targets

    def refTypes = (Collection<TypeDefinition>) refSchema.getMappingRelevantTypes()
    refTypes.each {
      def localName = it.displayName

      QName candidate = targetTypeCandidates[localName]
      if (candidate) {
        typeMap[it.name] = candidate
      }
    }

    MutableAlignment alignment = new DefaultAlignment()

    typeMap.each { QName ref, QName target ->
      println "Type match $ref.localPart -> $target.localPart"

      relateTypes(alignment, refSchema.getType(ref), targetSchema.getType(target))
    }

    alignment
  }

  private Map<String, String> parseTypeInfo(String typeDescription) {
    if (!typeDescription) {
      return [:]
    }

    def sections = typeDescription.split(/,/)

    sections.collectEntries { String section ->
      def property = section.split(/:/)
      if (property && property.length > 1) {
        // property found
        def propertyName = property[0].trim()
        def propertyValue = property[1..-1].join('').trim()
        // remove quotes
        if (propertyValue.length() >= 2) {
          if (propertyValue[0] == '"') {
            propertyValue = propertyValue[1..-1]
          }
          if (propertyValue[-1] == '"') {
            propertyValue = propertyValue[0..-2]
          }
        }

        [(propertyName): propertyValue]
      }
      else {
        [:]
      }
    }
  }

  private void relateTypes(MutableAlignment alignment, TypeDefinition ref, TypeDefinition target) {
    EntityDefinition refEntity = new TypeEntityDefinition(ref, SchemaSpaceID.SOURCE, null)
    EntityDefinition targetEntity = new TypeEntityDefinition(target, SchemaSpaceID.TARGET, null)

    // create Retype
    alignment.addCell(createCell(refEntity, targetEntity, RetypeFunction.ID))

    // try to find matches for properties

    // collect reference type properties by lowercase name
    def refChildren = (Collection<ChildDefinition<?>>) ref.children
    Map<String, EntityDefinition> refByName = refChildren.collectEntries {
      if (it.name.namespaceURI && it.name.namespaceURI.startsWith('http://www.opengis.net/gml')) {
        // ignore GML namespace properties (e.g. name)
        [:]
      }
      else if (it.asProperty()) {
        [(it.name.localPart.toLowerCase()): AlignmentUtil.getChild(refEntity, it.name)]
      }
      else {
        [:]
      }
    }

    // first check all target properties for the information in their description
    //XXX really simple - no handling of choices
    def children = (Collection<ChildDefinition<?>>) target.getChildren()
    children.each {
      def property = it.asProperty()
      if (property) {
        def propertyInfo = PostNASPropertyInfo.fromDescription(property.description)

        // property reference in description
        def targetProperty = AlignmentUtil.getChild(targetEntity, it.name)

        def refProperty = propertyInfo.findEntity(refEntity)
        if (refProperty) {
          alignment.addCell(createCell(refProperty, targetProperty, RenameFunction.ID))
        }
        else {
          // try to manually find match

          def byName = refByName.get(property.name.localPart)
          if (byName) {
            // relate to entity

            if (isReferenceType(byName)) {
              // for ReferenceType connect to href
              def byNameHref = new EntityAccessor(byName).findChildren('href').toEntityDefinition()
              if (byNameHref) {
                alignment.addCell(createCell(byNameHref, targetProperty, RenameFunction.ID))
              }
              else {
                println "Unable to find href attribute in ReferenceType"
                alignment.addCell(createCell(byName, targetProperty, RenameFunction.ID))
              }
            }
            else {
              alignment.addCell(createCell(byName, targetProperty, RenameFunction.ID))
            }
          }
          else if (property.name.localPart == 'gml_id') {
            // special case handling for GML id

            // mapping for GML id
            def refId = new EntityAccessor(refEntity).findChildren('id').toEntityDefinition()
            if (refId) {
              alignment.addCell(createCell(refId, targetProperty, RenameFunction.ID))
            }
            // mapping for GML identifier
            /*
             * XXX references in the DB schema are handled via gml_id, while
             * in the XSD schema they are handled via the identifier. Thus in
             * most cases replacing the identifier by gml_id will be correct.
             * TODO mark as unsafe matching
             */
            def refIdent = new EntityAccessor(refEntity).findChildren('identifier').toEntityDefinition()
            if (refIdent) {
              alignment.addCell(createCell(refIdent, targetProperty, RenameFunction.ID))
            }
          }
          else if (property.name.localPart == 'identifier') {
            // special case handling for identifier

            // mapping for GML identifier
            //XXX identifier is not used for references
            /*
             def refIdent = new EntityAccessor(refEntity).findChildren('identifier').toEntityDefinition()
             if (refIdent) {
             alignment.addCell(createCell(refIdent, targetProperty, RenameFunction.ID))
             }
             */
          }
          else if (property.name.localPart == 'ogc_fid') {
            // ignore generated sequential id in database
          }
          else if (property.name.localPart == 'wkb_geometry') {
            // handle geometry

            // try "position"
            def refPosition = new EntityAccessor(refEntity).findChildren('position').toEntityDefinition()
            if (refPosition) {
              //FIXME check if match is OK?
              alignment.addCell(createCell(refPosition, targetProperty, RenameFunction.ID))
            }
            else {
              println "No source match found for geometry property ${target.displayName}.$it.displayName - $propertyInfo"
            }
          }
          else {
            println "No source match found for property ${target.displayName}.$it.displayName - $propertyInfo"
          }
        }
      }
    }
  }

  private boolean isReferenceType(EntityDefinition entity) {
    if (entity.propertyPath && entity.propertyPath[-1].child.asProperty()) {
      def propertyType = entity.propertyPath[-1].child.asProperty().propertyType
      if (propertyType.name.localPart == 'ReferenceType') {
        // normal GML ReferenceType
        true
      }
      else {
        // AbstractMemberType extension
        propertyType.superType && propertyType.superType.name.localPart == 'AbstractMemberType'
      }
    }
    else {
      false
    }
  }

  private MutableCell createCell(EntityDefinition refEntity, EntityDefinition targetEntity, String functionId) {
    MutableCell cell = new DefaultCell()

    def ref = refEntity instanceof TypeEntityDefinition ? new DefaultType(refEntity) : new DefaultProperty((PropertyEntityDefinition) refEntity)
    def target = targetEntity instanceof TypeEntityDefinition ? new DefaultType(targetEntity) : new DefaultProperty((PropertyEntityDefinition) targetEntity)

    def sources = ArrayListMultimap.create()
    sources.put(null, ref)
    cell.source = sources as ListMultimap<String, ? extends Entity>

    def targets = ArrayListMultimap.create()
    targets.put(null, target)
    cell.target = targets as ListMultimap<String, ? extends Entity>

    cell.transformationIdentifier = functionId

    cell
  }
}
