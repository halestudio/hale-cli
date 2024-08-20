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

package to.wetransform.halecli.project

import org.eclipse.core.runtime.content.IContentType

import eu.esdihumboldt.hale.common.align.model.Alignment
import eu.esdihumboldt.hale.common.core.io.HaleIO
import eu.esdihumboldt.hale.common.core.io.Value
import eu.esdihumboldt.hale.common.core.io.extension.IOProviderDescriptor
import eu.esdihumboldt.hale.common.core.io.project.FixedProjectInfoService
import eu.esdihumboldt.hale.common.core.io.project.ProjectInfoService
import eu.esdihumboldt.hale.common.core.io.project.ProjectWriter
import eu.esdihumboldt.hale.common.core.io.project.model.IOConfiguration
import eu.esdihumboldt.hale.common.core.io.project.model.Project
import eu.esdihumboldt.hale.common.core.io.report.IOReport
import eu.esdihumboldt.hale.common.core.io.supplier.LocatableOutputSupplier
import eu.esdihumboldt.hale.common.core.report.ReportHandler
import eu.esdihumboldt.hale.common.core.service.ServiceManager
import eu.esdihumboldt.hale.common.core.service.ServiceProvider
import eu.esdihumboldt.hale.common.schema.model.SchemaSpace
import groovy.transform.CompileStatic
import to.wetransform.halecli.project.advisor.SaveProjectAdvisor

/**
 * Helper for dealing with hale projects.
 *
 * @author Simon Templer
 */
@CompileStatic
class ProjectHelper {

  static void saveProject(Project project, Alignment alignment, SchemaSpace sourceSchema,
    SchemaSpace targetSchema, LocatableOutputSupplier<? extends OutputStream> output,
    ReportHandler reports, String extension, URI projectLoadLocation,
    Map<String, String> settings = [:]) {

    // write project
    IContentType projectType = HaleIO.findContentType(
      ProjectWriter.class, null, "project.$extension")
    IOProviderDescriptor factory = HaleIO.findIOProviderFactory(
      ProjectWriter.class, projectType, null)

    saveProject(project, alignment, sourceSchema, targetSchema, output,
      reports, factory, projectLoadLocation, settings)
  }

  static void saveProject(Project project, Alignment alignment, SchemaSpace sourceSchema,
    SchemaSpace targetSchema, LocatableOutputSupplier<? extends OutputStream> output,
    ReportHandler reports, IOProviderDescriptor writerFactory, URI projectLoadLocation,
    Map<String, String> settings = [:]) {

    // write project
    ProjectWriter projectWriter
    try {
      projectWriter = (ProjectWriter) writerFactory.createExtensionObject()
    } catch (Exception e1) {
      throw new IllegalStateException("Failed to create project writer", e1)
    }
    projectWriter.setTarget(output)

    // apply custom settings
    settings.each { setting, value ->
      projectWriter.setParameter(setting, Value.simple(value))
    }

    // store (incomplete) save configuration
    IOConfiguration saveConf = new IOConfiguration()
    projectWriter.storeConfiguration(saveConf.getProviderConfiguration())
    saveConf.setProviderId(writerFactory.getIdentifier())
    project.setSaveConfiguration(saveConf)

    // service provider
    // XXX what services are needed for project export?
    ServiceProvider projectScope = new ServiceManager(ServiceManager.SCOPE_PROJECT)
    ProjectInfoService projectInfo = new FixedProjectInfoService(project, projectLoadLocation)
    ServiceProvider serviceProvider = new ServiceProvider() {

        @Override
        public <T> T getService(Class<T> serviceInterface) {
          if (ProjectInfoService.metaClass.equals(serviceInterface)) {
            return projectInfo
          }
          return projectScope.getService(serviceInterface)
        }
      }

    SaveProjectAdvisor advisor = new SaveProjectAdvisor(project, alignment, sourceSchema,
      targetSchema, projectLoadLocation)
    advisor.setServiceProvider(serviceProvider)
    advisor.prepareProvider(projectWriter)
    advisor.updateConfiguration(projectWriter)
    // HeadlessIO.executeProvider(projectWriter, advisor, null, reports);
    IOReport report
    try {
      report = projectWriter.execute(null)
    } catch (Exception e) {
      throw new IllegalStateException("Error writing project file.", e)
    }
    if (report != null) {
      reports?.publishReport(report)
      if (!report.isSuccess() || report.errors) {
        throw new IllegalStateException("Error writing project file.")
      }
    }
  }
}
