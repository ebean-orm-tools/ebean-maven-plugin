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
@Mojo(name = "testEnhance", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class TestEnhanceMojo extends AbstractEnhance {

  /**
   * The directory holding the class files we want to transform.
   */
  @Parameter(property = "project.build.testOutputDirectory")
  String testClassSource;

  public void execute() throws MojoExecutionException {
    File testOutDir = new File(testClassSource);
    if (testOutDir.exists()) {
      executeFor(testClassSource);
    }
  }

}