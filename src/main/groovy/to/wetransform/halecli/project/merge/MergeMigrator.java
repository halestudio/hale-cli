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

package to.wetransform.halecli.project.merge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.esdihumboldt.hale.common.align.extension.function.FunctionDefinition;
import eu.esdihumboldt.hale.common.align.extension.function.FunctionUtil;
import eu.esdihumboldt.hale.common.align.merge.MergeCellMigrator;
import eu.esdihumboldt.hale.common.align.merge.extension.MigratorExtension;
import eu.esdihumboldt.hale.common.align.merge.impl.DefaultMergeCellMigrator;
import eu.esdihumboldt.hale.common.align.merge.impl.MatchingMigration;
import eu.esdihumboldt.hale.common.align.merge.impl.TargetIndex;
import eu.esdihumboldt.hale.common.align.migrate.AlignmentMigration;
import eu.esdihumboldt.hale.common.align.migrate.CellMigrator;
import eu.esdihumboldt.hale.common.align.migrate.MigrationOptions;
import eu.esdihumboldt.hale.common.align.migrate.impl.DefaultAlignmentMigrator;
import eu.esdihumboldt.hale.common.align.migrate.util.MigrationUtil;
import eu.esdihumboldt.hale.common.align.model.Alignment;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.ChildContext;
import eu.esdihumboldt.hale.common.align.model.Entity;
import eu.esdihumboldt.hale.common.align.model.EntityDefinition;
import eu.esdihumboldt.hale.common.align.model.MutableAlignment;
import eu.esdihumboldt.hale.common.align.model.MutableCell;
import eu.esdihumboldt.hale.common.align.model.impl.DefaultAlignment;
import eu.esdihumboldt.hale.common.align.model.impl.DefaultCell;
import eu.esdihumboldt.hale.common.core.report.SimpleLog;
import eu.esdihumboldt.hale.common.core.service.ServiceProvider;

/**
 * Alignment migrator for alignment merge.
 *
 * @author Simon Templer
 */
public class MergeMigrator extends DefaultAlignmentMigrator {

  private static final Logger log = LoggerFactory.getLogger(MergeMigrator.class);

  private TargetIndex targetIndex;

  private final MergeStatistics statistics;

  /**
   * @param serviceProvider the service provider if available
   */
  public MergeMigrator(@Nullable ServiceProvider serviceProvider, boolean collectStatistics) {
    super(serviceProvider);

    if (collectStatistics) {
      statistics = new MergeStatistics();
    }
    else {
      statistics = null;
    }
  }

  @Override
  public MutableAlignment updateAligmment(Alignment originalAlignment, AlignmentMigration migration,
      MigrationOptions options, SimpleLog log) {
    if (migration instanceof MatchingMigration) {
      MatchingMigration mig = (MatchingMigration) migration;

      collectAlignmentStatistics(mig.getProject().getAlignment(), true);
    }

    collectAlignmentStatistics(originalAlignment, false);

    //XXX the following is copied and adapted from the base class

    MutableAlignment result = new DefaultAlignment(originalAlignment);

    // XXX TODO adapt custom functions?!
//      result.getCustomPropertyFunctions();

    Collection<? extends Cell> cellList = new ArrayList<>(result.getCells());
    for (Cell cell : cellList) {
      // XXX
      if (cell instanceof MutableCell) {
        Iterable<MutableCell> newCells = mergeCell(cell, migration, options, log);
        result.removeCell(cell);
        for (MutableCell newCell : newCells) {
          MigrationUtil.removeIdPrefix(newCell, options.transferBase(),
              options.transferBase());
          if (newCell != null) {
            result.addCell(newCell);
          }
        }
      }
      else {
        // XXX can we deal with other cases? (Base alignment cells)
        if (options.transferBase()) {
          // include base alignment cell as mutable mapping cell
          Iterable<MutableCell> newCells = mergeCell(cell, migration, options, log);
          result.removeCell(cell);
          for (MutableCell newCell : newCells) {
            MigrationUtil.removeIdPrefix(newCell, true, true);
            if (newCell != null) {
              result.addCell(newCell);
            }
          }
        }
      }
    }

    if (options.transferBase()) {
      MigrationUtil.removeBaseCells(result);
    }
    else {
      // does something need to be done to correctly retain base
      // alignments?
    }

    return result;
  }

  protected Iterable<MutableCell> mergeCell(Cell originalCell, AlignmentMigration migration,
      MigrationOptions options, SimpleLog log) {

    if (originalCell.getSource() == null || originalCell.getSource().isEmpty()) {
      // cells w/ source can be copied w/ changes
      return Collections.singleton(new DefaultCell(originalCell));
    }

    if (migration instanceof MatchingMigration) {
      MatchingMigration mig = (MatchingMigration) migration;

      if (targetIndex == null) {
        // build index on matching project, regarding their targets
        targetIndex = new TargetIndex(mig.getProject().getAlignment());
      }

      // check cell sources - how are they represented in the matching?
      Collection<? extends Entity> sources = originalCell.getSource().values();

      collectStatistics(originalCell, sources);

      MergeCellMigrator merger;
      try {
        merger = MigratorExtension.getInstance().getMigrator(originalCell.getTransformationIdentifier()).orElse(null);
      } catch (Exception e) {
        throw new IllegalStateException("Unable to initialize migrator for function " + originalCell.getTransformationIdentifier(), e);
      }
      if (merger == null) {
        CellMigrator cellMigrator = super.getCellMigrator(originalCell.getTransformationIdentifier());
        if (cellMigrator instanceof MergeCellMigrator) {
          // function explicitly supports merge
          merger = (MergeCellMigrator) cellMigrator;
        }
        else {
          // use default behavior
          merger = new DefaultMergeCellMigrator();
        }
      }

      return merger.mergeCell(originalCell, targetIndex, migration, this::getCellMigrator, log);
    }
    else throw new IllegalStateException();
  }

  /**
   * Collect statistics on an alignment.
   *
   * @param alignment the alignment
   * @param migrationAlignment if the alignment is the migration alignment
   */
  private void collectAlignmentStatistics(Alignment alignment, boolean migrationAlignment) {
    alignment.getCells().forEach(cell -> {
      String function = cell.getTransformationIdentifier();
      FunctionDefinition<?> fun = FunctionUtil.getFunction(function, serviceProvider);

      boolean noSource = cell.getSource() == null || cell.getSource().isEmpty();

      statistics.addFunctionUse(function, fun, migrationAlignment, noSource);
    });
  }

  /**
   * Collect statistics related to the given cell to migrate.
   *
   * @param sources the cell sources
   * @param originalCell the cell to migrate
   */
  private void collectStatistics(Cell originalCell, Collection<? extends Entity> sources) {
    if (statistics == null) {
      return;
    }

    String targetFunction = FunctionUtil.getFunction(originalCell.getTransformationIdentifier(), serviceProvider).getDisplayName();
    List<List<String>> sourceFunctions = new ArrayList<>();
    boolean incomplete = false;
    for (Entity source : sources) {
      boolean hasOldSourceCondition = false;
      if (hasConditions(source)) {
        statistics.addConditionOldSource();
        hasOldSourceCondition = true;
      }

      List<Cell> matches = targetIndex.getCellsForTarget(source.getDefinition());
      if (matches.size() > 1) {
        log.warn("Mutiple match cells");
        statistics.addMultiMatch();
      }
      else if (matches.isEmpty()) {
        log.error("No match for source " + source.getDefinition());
        incomplete = true;
        sourceFunctions.add(Collections.singletonList("<No match>"));
        statistics.addNoMatch(source.getDefinition());
      }

      if (!matches.isEmpty()) {
        List<String> matchFunctions = new ArrayList<>();
        for (Cell match : matches) {
          String sourceFunction = match.getTransformationIdentifier();
          sourceFunction = FunctionUtil.getFunction(sourceFunction, serviceProvider).getDisplayName();
          matchFunctions.add(sourceFunction);

          boolean hasNewSourceCondition = false;

          if (match.getSource() != null) {
            //XXX also report multiple sources?

            for (Entity matchSource : match.getSource().values()) {
              if (hasConditions(matchSource)) {
                statistics.addConditionNewSource();
                hasNewSourceCondition = true;
              }
            }
          }

          if (hasOldSourceCondition && hasNewSourceCondition) {
            statistics.addMatchConditionCombination();
          }
        }
        sourceFunctions.add(matchFunctions);
      }
    }
    if (!sourceFunctions.isEmpty()) {
      statistics.addMatch(targetFunction, sourceFunctions);
    }
    if (incomplete) {
      statistics.addIncomplete();
    }
    statistics.addCell();

    // System.out.println("| " + targetFunction + ": " + sourceFunctions.stream().collect(Collectors.joining(", ")) + " |");
  }

  private boolean hasConditions(Entity source) {
    //XXX what about index conditions?

    EntityDefinition def = source.getDefinition();

    if (def.getFilter() != null) {
      return true;
    }

    if (def.getPropertyPath() != null) {
      for (ChildContext child : def.getPropertyPath()) {
        if (child.getCondition() != null && child.getCondition().getFilter() != null) {
          return true;
        }
      }
    }

    return false;
  }

  public MergeStatistics getStatistics() {
    return statistics;
  }

}
