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
 *
 * Contributors:
 *     wetransform GmbH <http://www.wetransform.to>
 */

package to.wetransform.halecli.project.match.postnas;

import java.util.List

import eu.esdihumboldt.hale.common.align.groovy.accessor.EntityAccessor
import eu.esdihumboldt.hale.common.align.groovy.accessor.PathElement;
import eu.esdihumboldt.hale.common.align.groovy.accessor.internal.EntityAccessorUtil;
import eu.esdihumboldt.hale.common.align.model.EntityDefinition
import eu.esdihumboldt.util.groovy.paths.Path;
import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.transform.ToString

@CompileStatic
@Immutable
@ToString(includePackage = false)
class PostNASPropertyInfo {

  String baseProperty
  List<String> path
  String typeCategory
  String typeName
  String cardinality

  String assocRef
  String assocType

  static PostNASPropertyInfo fromDescription(String description) {
    String baseProperty
    List<String> path
    String typeCategory
    String typeName
    String cardinality

    /**
     * Association type in reference schema
     */
    String assocRef
    /**
     * Association type in PostNAS schema
     */
    String assocType

    if (description) {
      if (description.startsWith('Assoziation zu:')) {
        // association
        // Example 'Assoziation zu: FeatureType AA_Meilenstein (aa_meilenstein) 0..1'

        def descr = description[15..-1].trim()
        def parts = descr.split(/\s/)

        if (parts.length >= 2) {
          assocRef = parts[1] ?: null
        }
        if (parts.length >= 3) {
          def at = parts[2]
          if (at.startsWith('(')) {
            at = at[1..-1]
          }
          if (at.endsWith(')')) {
            at = at[0..-2]
          }
          assocType = at
        }
        if (parts.length >= 4) {
          cardinality = parts[3] ?: null
        }
      }
      else {
        // "normal" property
        // Example 'modellart|AA_Modellart|advStandardModell enumeration AA_AdVStandardModell 0..1'

        def parts = description.split(/\s/)
        if (parts.length >= 1) {
          String propertyPart = parts[0]

          if (propertyPart) {
            path = propertyPart.split(/\|/) as List
            if (path) {
              baseProperty = path.remove(0)
            }
          }
        }
        if (parts.length >= 2) {
          typeCategory = parts[1] ?: null
        }
        if (parts.length >= 3) {
          typeName = parts[2] ?: null
        }
        if (parts.length >= 4) {
          cardinality = parts[3] ?: null
        }
      }
    }

    new PostNASPropertyInfo(baseProperty: baseProperty, path: path,
      typeCategory: typeCategory, typeName: typeName, cardinality: cardinality,
      assocRef: assocRef, assocType: assocType)
  }

  EntityDefinition findEntity(EntityDefinition typeEntity) {
    EntityAccessor accessor = new EntityAccessor(typeEntity)

    if (!baseProperty) {
      return null
    }

    accessor = accessor.findChildren(baseProperty)

    if (path) {
      path.each {
        accessor = accessor.findChildren(it)
      }
    }

    try {
      accessor.toEntityDefinition()
    } catch (IllegalStateException e) {
      // multiple results found
      def options = accessor.all().collect {
        EntityAccessorUtil.createEntity((Path<PathElement>)it)
      }

      def candidates = options.findAll {
        // prefer properties with AdV namespace
        def ns = it.definition.name.namespaceURI
        ns && ns.toLowerCase().contains('adv')
      }

      if (candidates.size() > 1) {
        def names = candidates.collect { it.definition.name }
        println "Multiple candidates found for match $names"
      }

      if (candidates.empty) {
        println "No candidates chosen for property path with multiple results"
        null
      }
      else {
        candidates[0]
      }
    }
  }

}
