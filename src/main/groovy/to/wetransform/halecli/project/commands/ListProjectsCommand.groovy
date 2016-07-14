package to.wetransform.halecli.project.commands

import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths;
import java.util.List;

import eu.esdihumboldt.hale.common.headless.impl.ProjectTransformationEnvironment
import groovy.transform.CompileStatic;
import groovy.util.OptionAccessor;
import to.wetransform.halecli.CommandContext;
import to.wetransform.halecli.project.AbstractProjectCommand
import to.wetransform.halecli.project.AbstractProjectEnvironmentCommand

@CompileStatic
class ListProjectsCommand extends AbstractProjectEnvironmentCommand {

  final String shortDescription = 'List hale projects found at the specified location'

  int runForProjects(List<URI> projects, OptionAccessor options, CommandContext context) {
    println()
    if (projects) {
      println 'Found the following hale projects:'
      projects.each { URI location ->
        try {
          Path path = Paths.get(location)
          Path relative = Paths.get('.').toAbsolutePath().parent.relativize(path)
          println "  $relative"
        } catch (e) {
          e.printStackTrace()
          // ignore - just print URI
          println "  $location"
        }
      }
      0
    }
    else {
      println 'No hale projects found.'
      0
    }
  }

  boolean runForProject(ProjectTransformationEnvironment projectEnv, URI projectLocation, OptionAccessor options,
      CommandContext context) {
    // is not being called
    true
  }

}
