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

package to.wetransform.halecli.project.merge

import java.util.List;

import eu.esdihumboldt.hale.common.align.helper.EntityDefinitionComparator
import eu.esdihumboldt.hale.common.align.model.EntityDefinition;
import groovy.json.JsonOutput;

/**
 * Collects statistics on the merge.
 *
 * @author Simon Templer
 */
class MergeStatistics {

  /**
   * Collects entities for which no match is found.
   */
  private final Set<EntityDefinition> noMatch = new TreeSet<>(new EntityDefinitionComparator())

  /**
   * Collects number of multi-matches encountered
   * (where the source of a function again has different matched sources).
   */
  private int multiMatches = 0

  /**
   * Collects the number of incomplete cells.
   */
  private int incomplete = 0

  /**
   * Collects the number of overall cells.
   */
  private int cells = 0

  /**
   * Collects matches.
   */
  private final Map<String, Object> matches = [:]

  void addNoMatch(EntityDefinition entity) {
    noMatch.add(entity)
  }

  void addMultiMatch() {
    multiMatches++
  }

  void writeTo(Writer w) {
    Map stats = [:]

    stats.matches = matches

    stats.noMatch = noMatch.collect { it.toString() }.toList()

    stats.multiMatches = multiMatches

    stats.incomplete = incomplete

    stats.cells = cells

    w.write(JsonOutput.prettyPrint(JsonOutput.toJson(stats)))
  }

  /**
   * Add a concrete match of transformation functions.
   *
   * @param targetFunction the target transformation function
   * @param sourceFunctions the source transformation functions, grouped by cell source
   */
  public void addMatch(String targetFunction, List<List<String>> sourceFunctions) {
    def sources = matches[targetFunction]
    if (sources == null) {
      sources = []
      matches[targetFunction] = sources
    }
    sources << sourceFunctions
  }

  public void addIncomplete() {
    incomplete++
  }

  public void addCell() {
    cells++
  }

}
