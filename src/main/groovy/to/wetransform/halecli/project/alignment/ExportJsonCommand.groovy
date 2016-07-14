package to.wetransform.halecli.project.alignment;

import java.net.URI;

import eu.esdihumboldt.hale.common.align.io.AlignmentWriter
import eu.esdihumboldt.hale.common.core.HalePlatform;
import eu.esdihumboldt.hale.common.core.io.Value;
import eu.esdihumboldt.hale.common.core.io.report.IOReport;
import eu.esdihumboldt.hale.common.core.io.supplier.FileIOSupplier
import eu.esdihumboldt.hale.common.core.report.ReportHandler;
import eu.esdihumboldt.hale.common.headless.impl.ProjectTransformationEnvironment
import eu.esdihumboldt.hale.io.html.svg.mapping.json.JsonMappingExporter;
import groovy.transform.CompileStatic;
import groovy.util.OptionAccessor
import to.wetransform.halecli.CommandContext
import to.wetransform.halecli.Util;
import to.wetransform.halecli.project.AbstractProjectEnvironmentCommand

@CompileStatic
class ExportJsonCommand extends AbstractProjectEnvironmentCommand {

  boolean runForProject(ProjectTransformationEnvironment projectEnv, URI projectLocation,
      OptionAccessor options, CommandContext context, ReportHandler reports) {
    // configure writer
    JsonMappingExporter writer = new JsonMappingExporter()
    writer.alignment = projectEnv.alignment
    writer.projectInfo = projectEnv.project
    writer.projectLocation = projectLocation
    writer.serviceProvider = projectEnv
    writer.sourceSchema = projectEnv.sourceSchema
    writer.targetSchema = projectEnv.targetSchema
    
    //XXX only supported for files right now
    File projectFile = new File(projectLocation)
    
    // derive file name for HTML file
    File mappingTable = new File(projectFile.parentFile, projectFile.name + '.json')
    
    writer.target = new FileIOSupplier(mappingTable)
    
    IOReport report = writer.execute(null)
    
    Util.printSummary(report)
    
    report.isSuccess() && !report.errors
  }

  final String shortDescription = 'Generate JSON represenation of hale alignments'

}
