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

import java.util.List

import eu.esdihumboldt.hale.common.align.migrate.AlignmentMigration;
import eu.esdihumboldt.hale.common.align.migrate.AlignmentMigrator
import eu.esdihumboldt.hale.common.align.migrate.MigrationOptions;
import eu.esdihumboldt.hale.common.align.migrate.impl.DefaultAlignmentMigrator
import eu.esdihumboldt.hale.common.align.migrate.impl.MigrationOptionsImpl
import eu.esdihumboldt.hale.common.align.migrate.util.EffectiveMapping;
import eu.esdihumboldt.hale.common.align.model.Alignment
import eu.esdihumboldt.hale.common.core.io.project.model.IOConfiguration;
import eu.esdihumboldt.hale.common.core.io.project.model.Project;
import eu.esdihumboldt.hale.common.core.service.ServiceProvider;
import eu.esdihumboldt.hale.common.headless.impl.ProjectTransformationEnvironment
import eu.esdihumboldt.hale.common.instance.io.InstanceIO;
import eu.esdihumboldt.hale.common.schema.io.SchemaIO;
import eu.esdihumboldt.hale.common.schema.model.SchemaSpace;
import eu.esdihumboldt.util.cli.Command
import eu.esdihumboldt.util.cli.CommandContext
import groovy.transform.CompileStatic
import groovy.util.OptionAccessor;
import to.wetransform.halecli.util.ProjectCLI;;;;

/**
 * Base class for commands migrating a project to a different schema.
 *
 * @author Simon Templer
 */
abstract class AbstractMigrationCommand<T extends AlignmentMigration> extends AbstractMigratorCommand<DefaultAlignmentMigrator, T> {

  protected abstract void addOptions(CliBuilder cli)

  protected abstract T createMigration(OptionAccessor options)

  protected DefaultAlignmentMigrator createMigrator(ServiceProvider serviceProvider, OptionAccessor options) {
    new DefaultAlignmentMigrator(serviceProvider)
  }

  protected abstract SchemaSpace getNewSource(T migration, OptionAccessor options)

  protected abstract List<IOConfiguration> getNewSourceConfig(T migration, OptionAccessor options)

}
