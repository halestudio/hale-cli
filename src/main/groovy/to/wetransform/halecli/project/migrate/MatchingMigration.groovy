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
import eu.esdihumboldt.hale.common.align.model.Alignment;
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

  @Override
  public Optional<EntityDefinition> entityReplacement(EntityDefinition entity) {
    // TODO Auto-generated method stub
    return Optional.empty();
  }

}
