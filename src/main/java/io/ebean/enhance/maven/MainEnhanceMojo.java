package io.ebean.enhance.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;

/**
 * Enhancement for src/main classes. Marked as threadSafe to announce support for parallel building.
 */
@Mojo(name = "enhance", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class MainEnhanceMojo extends AbstractEnhance {

  /**
   * The directory holding the class files we want to transform.
   */
  @Parameter(property = "project.build.outputDirectory")
  String classSource;


  public void execute() throws MojoExecutionException {
    if (new File(classSource).exists()) {
      executeFor(classSource);
    } else {
      getLog().info("Skipping non-existent outputDirectory " + classSource);
    }
  }

}
