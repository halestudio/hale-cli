
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
package to.wetransform.halecli.project.merge

import groovy.json.JsonOutput

import java.util.List

import eu.esdihumboldt.hale.common.align.extension.function.FunctionDefinition
import eu.esdihumboldt.hale.common.align.extension.function.FunctionUtil
import eu.esdihumboldt.hale.common.align.helper.EntityDefinitionComparator
import eu.esdihumboldt.hale.common.align.model.EntityDefinition

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
   * Collects the number of matched sources in the new source schema that have associated conditions.
   */
  private int conditionNewSource = 0

  /**
   * Collects the number of sources in the old source schema that have associated conditions.
   */
  private int conditionOldSource = 0

  /**
   * Collects the number of matches where both old and new source have conditions that need to be combined.
   */
  private int matchConditionCombination = 0

  /**
   * Collects matches.
   */
  private final Map<String, Object> matches = [:]

  /**
   * Collects functions used in original alignment.
   */
  private final Map<String, Object> functions = [:]

  /**
   * Collects functions used in migration alignment.
   */
  private final Map<String, Object> migrationFunctions = [:]

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

    stats.conditionNewSource = conditionNewSource

    stats.conditionOldSource = conditionOldSource

    stats.matchConditionCombination = matchConditionCombination

    stats.cells = cells

    stats.functions = functions

    stats.mergeFunctions = migrationFunctions

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

  public void addConditionOldSource() {
    conditionOldSource++
  }

  public void addConditionNewSource() {
    conditionNewSource++
  }

  public void addMatchConditionCombination() {
    matchConditionCombination++
  }

  /**
   * Add the use of a specific function (in a cell).
   * @param functionId the function ID
   * @param fun the function definition if available
   * @param migrationFunction if the function usage is from the migration alignment
   */
  public void addFunctionUse(String functionId, FunctionDefinition fun, boolean migrationFunction, boolean noSource) {
    def map = migrationFunction ? migrationFunctions : functions

    String name = fun?.displayName ?: functionId

    def entry = map[name]
    if (entry == null) {
      entry = [
        count: 1,
        id: functionId,
        noSource: 0,
        augmentation: fun?.augmentation
      ]
      map[name] = entry
    }
    else {
      entry.count++
    }
    if (noSource) {
      entry.noSource++
    }
  }
}
