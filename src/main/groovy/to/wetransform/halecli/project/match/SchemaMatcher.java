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

package to.wetransform.halecli.project.match;

import eu.esdihumboldt.hale.common.align.model.Alignment;
import eu.esdihumboldt.hale.common.schema.model.TypeIndex;

public interface SchemaMatcher {

  /**
   * Generate a matching between two schemas represented by an alignment.
   *
   * @param refSchema the reference schema
   * @param targetSchema the target schema to map to
   * @return the generated alignment
   */
  Alignment generateSchemaMatching(TypeIndex refSchema, TypeIndex targetSchema);

}
