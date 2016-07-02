package to.wetransform.halecli.project.commands;

import java.net.URI;

import eu.esdihumboldt.hale.common.align.io.AlignmentWriter
import eu.esdihumboldt.hale.common.core.io.report.IOReport;
import eu.esdihumboldt.hale.common.core.io.supplier.FileIOSupplier;
import eu.esdihumboldt.hale.common.headless.impl.ProjectTransformationEnvironment
import eu.esdihumboldt.hale.io.xls.writer.XLSAlignmentMappingWriter
import groovy.transform.CompileStatic;
import groovy.util.OptionAccessor
import to.wetransform.halecli.CommandContext
import to.wetransform.halecli.Util;
import to.wetransform.halecli.project.AbstractProjectCommand

@CompileStatic
class MappingTableCommand extends AbstractProjectCommand {

  boolean runForProject(ProjectTransformationEnvironment projectEnv, URI projectLocation,
      OptionAccessor options, CommandContext context) {
    // configure writer
    XLSAlignmentMappingWriter writer = new XLSAlignmentMappingWriter()
    writer.alignment = projectEnv.alignment
    writer.projectInfo = projectEnv.project
    writer.projectLocation = projectLocation
    writer.serviceProvider = projectEnv
    writer.sourceSchema = projectEnv.sourceSchema
    writer.targetSchema = projectEnv.targetSchema
    
    //XXX only supported for files right now
    File projectFile = new File(projectLocation)
    
    // derive file name for Excel file
    File mappingTable = new File(projectFile.parentFile, projectFile.name + '.xlsx')
    
    writer.target = new FileIOSupplier(mappingTable)
    
    IOReport report = writer.execute(null)
    
    Util.printSummary(report)
    
    report.isSuccess() && !report.errors
  }

}
