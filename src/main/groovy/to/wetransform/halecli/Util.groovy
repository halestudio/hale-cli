package to.wetransform.halecli

import org.joda.time.Period
import org.joda.time.format.PeriodFormat

import eu.esdihumboldt.hale.app.transform.ExecUtil;
import eu.esdihumboldt.hale.common.core.report.Report
import eu.esdihumboldt.hale.common.core.report.ReportHandler;
import eu.esdihumboldt.hale.common.headless.report.ReportFile;
import groovy.transform.CompileStatic

class Util {
  
  public static final String BASH_COMPLETION_FILE = 'FILE'

  @CompileStatic
  static URI fileOrUri(String value) {
    try {
      URI uri = URI.create(value)
      if (uri.scheme && uri.scheme.length() > 1) {
        // only accept as URI if a schema is present
        // and the scheme is more than just one character
        // which is likely a Windows drive letter
        return uri
      }
      else {
        return new File(value).toURI()
      }
    } catch (e) {
      return new File(value).toURI()
    }
  }
  
  @CompileStatic
  static void printSummary(Report report) {
    // print report summary
    println "Action summary: ${report.taskName}"
    println "   ${report.errors.size()} errors"
    println "   ${report.warnings.size()} warnings"

    // state success
    print(report.isSuccess() ?
      "   Completed" :
      "   Failed")
    
    // add duration if applicable
    if (report.startTime) {
      def duration = PeriodFormat.wordBased().print(
        new Period(report.startTime.time, report.timestamp.time))
      
      print(report.isSuccess() ? ' in ' : ' after ')
      print duration
    }
    // complete success line
    println ''
  }
  
  @CompileStatic
  static ReportHandler createReportHandler(File reportFile = null) {
    final ReportHandler delegateTo
    if (reportFile) {
      delegateTo = new ReportFile(reportFile)
    }
    else {
      delegateTo = null
    }

    new ReportHandler() {

      @Override
      public void publishReport(Report report) {
        printSummary(report)
        if (delegateTo != null) {
          delegateTo.publishReport(report);
        }
      }
    }
  }
  
  @CompileStatic
  static void printUsage(CommandContext context, Map<String, Command> commands) {
    println "usage: ${context.baseCommand} <command> [<args>]"
    println()
    println 'Supported commands are:'
    
    String maxEntry = commands.keySet().max {
      it.length()
    }
    
    if (maxEntry) {
      print '  help'
      print(' - '.padLeft(maxEntry.length() - 1))
      print 'Show this help'
      println()
      
      commands.sort().each { name, command ->
        print "  $name"
        if (command.shortDescription) {
          print(' - '.padLeft(maxEntry.length() - name.length() + 3))
          print command.shortDescription
        }
        println()
      }
    }
  }
  
}
