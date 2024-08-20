
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
 */
package to.wetransform.halecli.project.migrate;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import eu.esdihumboldt.hale.common.align.merge.impl.MatchingMigration;
import eu.esdihumboldt.hale.common.align.migrate.AlignmentMigration;
import eu.esdihumboldt.hale.common.align.migrate.MigrationOptions;
import eu.esdihumboldt.hale.common.align.model.impl.TypeEntityDefinition;
import eu.esdihumboldt.hale.common.core.io.project.ComplexConfigurationService;
import eu.esdihumboldt.hale.common.core.io.project.ProjectIO;
import eu.esdihumboldt.hale.common.core.io.project.model.Project;
import eu.esdihumboldt.hale.common.core.report.SimpleLog;
import eu.esdihumboldt.hale.common.schema.SchemaSpaceID;
import eu.esdihumboldt.hale.common.schema.io.SchemaIO;
import eu.esdihumboldt.hale.common.schema.model.TypeIndex;

/**
 * Helper that updates a project
 *
 * @author Simon Templer
 */
public class ProjectMigrator {

  public static void updateProject(Project project, AlignmentMigration migration,
      MigrationOptions options, @Nullable TypeIndex oldSourceSchema,
      @Nullable TypeIndex oldTargetSchema, SimpleLog log) {
    ComplexConfigurationService conf = ProjectIO.createProjectConfigService(project);

    // mapping relevant types
    if (oldSourceSchema != null && options.updateSource()) {
      updateMappingRelevantTypes(conf, SchemaSpaceID.SOURCE, oldSourceSchema, migration, log);
    }
    if (oldTargetSchema != null && options.updateTarget()) {
      updateMappingRelevantTypes(conf, SchemaSpaceID.TARGET, oldTargetSchema, migration, log);
    }

    // TODO what else?
  }

  private static void updateMappingRelevantTypes(ComplexConfigurationService config,
      SchemaSpaceID schemaSpace, TypeIndex oldSchema, AlignmentMigration migration, SimpleLog log) {
    String confName = SchemaIO.getMappingRelevantTypesParameterName(schemaSpace);

    List<String> cfg = config.getList(confName);

    if (cfg != null) {
      List<String> typeNames = cfg.stream().map(name -> QName.valueOf(name))
          .map(name -> oldSchema.getType(name)).filter(type -> type != null)
          .map(type -> new TypeEntityDefinition(type, schemaSpace, null))

          .flatMap(entity -> {
            if (migration instanceof MatchingMigration) {
              return ((MatchingMigration) migration).findMatches(entity)
                  .orElse(Collections.emptyList()).stream();
            }
            else {
              return migration.entityReplacement(entity, log).map(e -> Collections.singletonList(e))
                  .orElse(Collections.emptyList()).stream();
            }
          })

          .map(option -> option.getType().getName().toString()).collect(Collectors.toList());

      config.setList(confName, typeNames);
    }
  }

}
