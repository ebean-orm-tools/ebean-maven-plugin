package io.ebean.enhance.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Enhancement for src/main classes.
 *
 */
@Mojo(name = "testEnhance", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class TestEnhanceMojo extends AbstractEnhance {

  /**
   * The directory holding the class files we want to transform.
   */
  @Parameter(property = "project.build.testOutputDirectory")
  String classSource;

  public void execute() throws MojoExecutionException {
    executeFor(classSource);
  }

}