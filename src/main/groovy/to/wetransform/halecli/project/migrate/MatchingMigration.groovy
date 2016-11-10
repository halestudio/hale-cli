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

package to.wetransform.halecli.project.migrate

import java.util.Optional

import eu.esdihumboldt.hale.common.align.migrate.AlignmentMigration
import eu.esdihumboldt.hale.common.align.model.Alignment
import eu.esdihumboldt.hale.common.align.model.AlignmentUtil
import eu.esdihumboldt.hale.common.align.model.Cell
import eu.esdihumboldt.hale.common.align.model.Entity;
import eu.esdihumboldt.hale.common.align.model.EntityDefinition
import groovy.transform.CompileStatic;;;

/**
 * Alignment migration based on a alignment representing a matching between different schemas.
 *
 * @author Simon Templer
 */
@CompileStatic
class MatchingMigration implements AlignmentMigration {

  private final Alignment matching

  MatchingMigration(Alignment matching) {
    this.matching = matching
  }

  protected Optional<EntityDefinition> findMatch(EntityDefinition entity) {
    Collection<? extends Cell> cells = matching.getCells(entity)

    if (cells.empty) {
      //XXX no replacement can be found -> what to do in this case?
      Optional.empty()
    }
    else {
      if (cells.size() == 1) {
        Cell cell = cells.iterator().next()

        // replace by target
        //XXX also support other way round?
        if (cell.target) {
          Entity e = cell.target.values().iterator().next()
          Optional.ofNullable(e.definition)
        }
        else {
          Optional.empty()
        }
      }
      else {
        //XXX more than one cell - for now ignored

        Optional.empty()
      }
    }
  }

  @Override
  Optional<EntityDefinition> entityReplacement(EntityDefinition entity) {
    def defaultEntity = AlignmentUtil.getAllDefaultEntity(entity)

    def matchedEntity = findMatch(defaultEntity);

    if (entity != defaultEntity) {
      // entity contained contexts -> translate them if possible
      //TODO
      println "Contexts for entities not handled yet"
    }

    if (!matchedEntity.isPresent()) {
      println "No match for entity $entity found"
    }

    return matchedEntity;
  }

}
