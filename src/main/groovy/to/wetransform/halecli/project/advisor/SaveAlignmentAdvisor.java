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

package to.wetransform.halecli.project.advisor;

import java.net.URI;

import eu.esdihumboldt.hale.common.align.io.AlignmentWriter;
import eu.esdihumboldt.hale.common.align.model.Alignment;
import eu.esdihumboldt.hale.common.core.io.impl.AbstractIOAdvisor;
import eu.esdihumboldt.hale.common.core.io.project.ProjectInfo;
import eu.esdihumboldt.hale.common.core.io.project.ProjectInfoAware;
import eu.esdihumboldt.hale.common.schema.model.SchemaSpace;

/**
 * Headless advisor for saving an alignment.
 *
 * @author Simon Templer
 */
public class SaveAlignmentAdvisor extends AbstractIOAdvisor<AlignmentWriter> {

  private final ProjectInfo projectInfo;
  private final Alignment alignment;
  private final SchemaSpace sourceSchema;
  private final SchemaSpace targetSchema;
  private final URI projectLoadLocation;

  public SaveAlignmentAdvisor(ProjectInfo projectInfo, Alignment alignment, SchemaSpace sourceSchema,
      SchemaSpace targetSchema, URI projectLoadLocation) {
    super();
    this.projectInfo = projectInfo;
    this.alignment = alignment;
    this.sourceSchema = sourceSchema;
    this.targetSchema = targetSchema;
    this.projectLoadLocation = projectLoadLocation;
  }

  @Override
  public void prepareProvider(AlignmentWriter provider) {
    super.prepareProvider(provider);

    provider.setTargetSchema(targetSchema);
    provider.setSourceSchema(sourceSchema);
    provider.setAlignment(alignment);
    if (provider instanceof ProjectInfoAware) {
      ProjectInfoAware aware = (ProjectInfoAware) provider;
      aware.setProjectInfo(projectInfo);
      aware.setProjectLocation(projectLoadLocation);
    }
  }

}
