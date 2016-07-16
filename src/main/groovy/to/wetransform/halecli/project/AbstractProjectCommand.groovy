package to.wetransform.halecli.project

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path;
import java.util.List

import eu.esdihumboldt.hale.app.transform.ExecTransformation.DirVisitor
import eu.esdihumboldt.hale.common.core.io.supplier.DefaultInputSupplier
import eu.esdihumboldt.hale.common.core.report.ReportHandler;
import eu.esdihumboldt.hale.common.headless.impl.ProjectTransformationEnvironment;
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode;
import to.wetransform.halecli.Command
import to.wetransform.halecli.CommandContext
import to.wetransform.halecli.Util

@CompileStatic
abstract class AbstractProjectCommand<T> implements Command {
  
  String getUsageExtension() {
    ''
  }
  
  void setupOptions(CliBuilder cli) {
    // override me
  }

  @CompileStatic(TypeCheckingMode.SKIP)
  @Override
  public int run(List<String> args, CommandContext context) {
    def cli = new CliBuilder(usage: context.baseCommand + ' [options] <project>' + usageExtension)
    
    cli._(longOpt: 'help', 'Show this help')
    
    setupOptions(cli)
    
    OptionAccessor options = cli.parse(args)
    
    if (options.help) {
      cli.usage()
      return 0
    }
    
    //TODO check options?
    
    def extraArgs = options.arguments()
    String projectArg
    if (extraArgs) {
      projectArg = extraArgs[0]
    }
    else {
      // default to current directory
      projectArg = '.'
    }
      
    URI projectUri = Util.fileOrUri(projectArg)
    File projectFile
    try {
      projectFile = new File(projectUri)
    } catch (e) {
      // ignore -> project is not a file/directory
    }
    
    List<URI> projects = []
    
    if (projectFile) {
      if (!projectFile.exists()) {
        throw new FileNotFoundException("File $projectFile does not exist")
      }
      
      if (projectFile.isDirectory()) {
        println "Checking directory $projectFile for project files..."
        // search for all projects
        ProjectsVisitor visitor = new ProjectsVisitor()
        try {
          Files.walkFileTree(projectFile.toPath(), visitor);
        } catch (IOException e) {
          throw new IllegalStateException("Error browsing given project directory $projectFile", e)
        }
        visitor.getCollectedFiles().each { Path file ->
          projects << file.toUri()
        }
      }
      else {
        // only project specified
        projects << projectUri
      }
    }
    else {
      // only URI specified 
      projects << projectUri
    }
    
    runForProjects(projects, options, context)
  }
  
  abstract T loadProject(URI location, ReportHandler reports)
  
  abstract String getProjectName(T project)
  
  int runForProjects(List<URI> projects, OptionAccessor options, CommandContext context) {
    List<URI> failedProjects = []
    
    boolean failed = false
    ReportHandler reports = Util.createReportHandler()
    projects.each { URI project ->
      try {
        println()
        println "Loading project at ${project}..."
        
        def projectEnv = loadProject(project, reports)
       
        println()
        String projectName = getProjectName(projectEnv)
        print "Running ${context.commandName} command on project"
        if (projectName) {
          print " \"$projectName\"..."
        }
        else {
          '...'
        }
        println()
         
        boolean success = runForProject(projectEnv, project, options, context, reports)
        if (!success) {
          failedProjects << project
          failed = true
        }
      } catch (e) {
        failedProjects << project
        failed = true
        e.printStackTrace()
      }
    }
    
    if (failed) {
      if (projects.size() > 1) {
        println()
        println 'Execution failed for projects:'
        failedProjects.each { URI projectUri ->
          println "  $projectUri"
        }
      }
      
      1
    }
    else {
      // everything went well
      0
    }
  }
  
  abstract boolean runForProject(T project, URI projectLocation,
    OptionAccessor options, CommandContext context, ReportHandler reports)

  @Override
  String bashCompletion(List<String> args, int current) {
    //TODO handling for options? how to adapt in subclasses?
    
    if (args) {
      // complete project file/dir
      Util.BASH_COMPLETION_FILE
    }
    else {
      null
    }
  }
  
}
