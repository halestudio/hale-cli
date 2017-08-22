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
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.esdihumboldt.hale.common.align.extension.function.FunctionUtil;
import eu.esdihumboldt.hale.common.align.migrate.AlignmentMigration;
import eu.esdihumboldt.hale.common.align.migrate.CellMigrator;
import eu.esdihumboldt.hale.common.align.migrate.MigrationOptions;
import eu.esdihumboldt.hale.common.align.migrate.impl.DefaultAlignmentMigrator;
import eu.esdihumboldt.hale.common.align.migrate.impl.MigrationOptionsImpl;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.CellUtil;
import eu.esdihumboldt.hale.common.align.model.Entity;
import eu.esdihumboldt.hale.common.align.model.EntityDefinition;
import eu.esdihumboldt.hale.common.align.model.MutableCell;
import eu.esdihumboldt.hale.common.align.model.functions.RenameFunction;
import eu.esdihumboldt.hale.common.align.model.functions.RetypeFunction;
import eu.esdihumboldt.hale.common.align.model.impl.DefaultCell;
import eu.esdihumboldt.hale.common.core.service.ServiceProvider;
import to.wetransform.halecli.project.migrate.AbstractMigration;
import to.wetransform.halecli.project.migrate.MatchingMigration;

/**
 * Alignment migrator for alignment merge.
 *
 * @author Simon Templer
 */
public class MergeMigrator extends DefaultAlignmentMigrator implements CellMigrator {

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

  protected CellMigrator getCellMigrator(String transformationIdentifier) {
    return this;
  }

  @Override
  public MutableCell updateCell(Cell originalCell, AlignmentMigration migration, MigrationOptions options) {
    if (originalCell.getSource() == null || originalCell.getSource().isEmpty()) {
      // cells w/ source can be copied w/ changes
      return new DefaultCell(originalCell);
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

      //FIXME for now just look at the case w/ one source
      EntityDefinition source = sources.iterator().next().getDefinition();
      List<Cell> matches = targetIndex.getCellsForTarget(source);
      if (!matches.isEmpty()) {
        //FIXME for now looking only at the case w/ one cell
        Cell match = matches.get(0);
        if (matches.size() > 1) {
          log.warn("Mutiple match cells, looking only at the first one");
        }

        // if the matching is a Retype/Rename, replace source of this cell
        if (isDirectMatch(match)) {
          MigrationOptions replaceSource = new MigrationOptionsImpl(true, false, options.transferBase());
          return super.getCellMigrator(originalCell.getTransformationIdentifier())
            .updateCell(originalCell, migration, replaceSource);
        }
        // if the cell is a Retype/Rename, replace the target of matching cell
        else if (isDirectMatch(originalCell)) {
          //FIXME respect any conditions/contexts on the original source?
          //XXX at least try to transfer them
          MigrationOptions replaceTarget = new MigrationOptionsImpl(false, true, options.transferBase());
          AlignmentMigration cellMigration = new AbstractMigration() {

            @Override
            protected Optional<EntityDefinition> findMatch(EntityDefinition entity) {
              Entity target = CellUtil.getFirstEntity(originalCell.getTarget());
              if (target != null) {
                return Optional.ofNullable(target.getDefinition());
              }
              return Optional.empty();
            }
          };
          return super.getCellMigrator(match.getTransformationIdentifier())
            .updateCell(match, cellMigration, replaceTarget);
        }
        // otherwise, use custom logic to try to combine cells
        else {
          log.warn("Unsupported combination: " + match.getTransformationIdentifier() + " / " + originalCell.getTransformationIdentifier());
          return new DefaultCell(originalCell);
        }
      }
      else {
        // no match -> copy as-is
        return new DefaultCell(originalCell);
      }
    }
    else throw new IllegalStateException();
  }

  /**
   * @param sources
   * @param originalCell
   *
   */
  private void collectStatistics(Cell originalCell, Collection<? extends Entity> sources) {
    if (statistics == null) {
      return;
    }

    String targetFunction = FunctionUtil.getFunction(originalCell.getTransformationIdentifier(), serviceProvider).getDisplayName();
    List<List<String>> sourceFunctions = new ArrayList<>();
    boolean incomplete = false;
    for (Entity source : sources) {
      List<Cell> matches = targetIndex.getCellsForTarget(source.getDefinition());
      if (matches.size() > 1) {
        log.warn("Mutiple match cells");
        statistics.addMultiMatch();
      }
      else if (matches.isEmpty()) {
        log.error("No match for source " + source.getDefinition());
        incomplete = true;
        statistics.addNoMatch(source.getDefinition());
      }

      if (!matches.isEmpty()) {
        List<String> matchFunctions = new ArrayList<>();
        for (Cell match : matches) {
          String sourceFunction = match.getTransformationIdentifier();
          sourceFunction = FunctionUtil.getFunction(sourceFunction, serviceProvider).getDisplayName();
          matchFunctions.add(sourceFunction);
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

  public MergeStatistics getStatistics() {
    return statistics;
  }

  private boolean isDirectMatch(Cell match) {
    return match.getTransformationIdentifier().equals(RetypeFunction.ID) ||
        match.getTransformationIdentifier().equals(RenameFunction.ID);
  }

}
