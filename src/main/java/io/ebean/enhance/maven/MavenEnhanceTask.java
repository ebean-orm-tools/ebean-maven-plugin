package io.ebean.enhance.maven;

import io.ebean.enhance.agent.Transformer;
import io.ebean.enhance.ant.OfflineFileTransform;
import io.ebean.enhance.ant.TransformationListener;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A Maven Plugin that can enhance entity beans etc for use by Ebean.
 * <p>
 * You can use this plugin as part of your build process to enhance entity beans
 * etc.
 * </p>
 * <p>
 * The parameters are:
 * <ul>
 * <li><b>classSource</b> This is the root directory where the .class files are
 * found. If this is left out then this defaults to
 * ${project.build.outputDirectory}.</li>
 * <li><b>classDestination</b> This is the root directory where the .class files
 * are written to. If this is left out then this defaults to the
 * <b>classSource</b>.</li>
 * <li><b>packages</b> A comma delimited list of packages that is searched for
 * classes that need to be enhanced. If the package ends with ** or * then all
 * subpackages are also searched.</li>
 * <li><b>transformArgs</b> Arguments passed to the transformer. Typically a
 * debug level in the form of debug=1 etc.</li>
 * </ul>
 * </p>
 * 
 * <pre>{@code
 *
 *       <plugin>
 *         <groupId>org.avaje.ebeanorm</groupId>
 *         <artifactId>avaje-ebeanorm-mavenenhancer</artifactId>
 *         <version>4.6.1</version>
 *         <executions>
 *           <execution>
 *             <id>main</id>
 *             <phase>process-classes</phase>
 *             <configuration>
 *               <!--<classSource>target/classes</classSource>-->
 *               <!--<packages>org.example.domain.**</packages>-->
 *               <!--<transformArgs>debug=3</transformArgs>-->
 *             </configuration>
 *           <goals>
 *             <goal>enhance</goal>
 *           </goals>
 *         </execution>
 *       </executions>
 *     </plugin>
 *
 * }</pre>
 *
 * <h3>To invoke explicitly:</h3
 * <pre>{@code
 *
 *   mvn avaje-ebeanormenhancer:enhance
 *
 * }</pre>
 *
 * @author Paul Mendelson, Vaughn Butt, Rob Bygrave
 * @since 2.5, Mar, 2009
 */
@Mojo(name = "enhance", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class MavenEnhanceTask extends AbstractMojo {

  /**
   * The class path used to read related classes.
   */
  @Parameter(property = "project.compileClasspathElements", required = true, readonly = true)
  List<String> compileClasspathElements;

  /**
   * The directory holding the class files we want to transform.
   */
  @Parameter(property = "project.build.outputDirectory")
  String classSource;

  /**
   * the classpath used to search for e.g. inherited classes
   */
  @Parameter(name = "classpath")
  private String classpath;

  /**
   * Set the arguments passed to the transformer.
   */
  @Parameter(name = "transformArgs")
  String transformArgs;

  /**
   * Set the package name to search for classes to transform.
   * <p>
   * If the package name ends in "/**" then this recursively transforms all sub
   * packages as well.
   * </p>
   * <p>
   * This is optional. When not set the agent may visit more classes to see if they need
   * enhancement but will still enhance effectively and pretty quickly (ignoring standard
   * jdk classes and lots of common libraries and language sdk's).
   * </p>
   */
  @Parameter(name = "packages")
  String packages;

  /**
   * Set to true to fail the maven build if exceptions occurred during enhancement.
   */
  @Parameter(name = "failOnExceptions")
  boolean failOnExceptions;

  public void execute() throws MojoExecutionException {

    final Log log = getLog();
    if (classSource == null) {
      classSource = "target/classes";
    }

    File f = new File("");
    log.info("Current Directory: " + f.getAbsolutePath());

    StringBuilder extraClassPath = new StringBuilder();
    extraClassPath.append(classSource);
    if (classpath != null && !classpath.isEmpty()) {
      if (!extraClassPath.toString().endsWith(";")) {
        extraClassPath.append(";");
      }
      extraClassPath.append(classpath);
    }

    ClassLoader classLoader = buildClassLoader();

    Transformer transformer = new Transformer(extraClassPath.toString(), transformArgs);
    log.info("classSource=" + classSource + "  transformArgs=" + nullToEmpty(transformArgs) + "  packages=" + nullToEmpty(packages));

    OfflineFileTransform ft = new OfflineFileTransform(transformer, classLoader, classSource);
    ft.setListener(new TransformationListener() {

      public void logEvent(String msg) {
        log.info(msg);
      }

      public void logError(String msg) {
        log.error(msg);
      }
    });

    ft.process(packages);

    Map<String, List<Throwable>> unexpectedExceptions = transformer.getUnexpectedExceptions();
    if (failOnExceptions && !unexpectedExceptions.isEmpty()) {
      throw new MojoExecutionException("Exceptions occurred during EBean enhancements, see the log above for the exact problems.");
    }
  }

  /**
   * Return a null string as empty (for pretty output on valid null parameters).
   */
  private String nullToEmpty(String val) {
    return (val == null) ? "" : val;
  }

  private ClassLoader buildClassLoader() {
    
    URL[] urls = buildClassPath();
    return URLClassLoader.newInstance(urls, Thread.currentThread().getContextClassLoader());
  }

  /**
   * Return the class path using project compileClasspathElements.
   */
  private URL[] buildClassPath() {

    try {
      List<URL> urls = new ArrayList<>(compileClasspathElements.size());

      Log log = getLog();

      for (String element : compileClasspathElements) {
        if (log.isDebugEnabled()) {
          log.debug("ClasspathElement: " + element);
        }
        urls.add(new File(element).toURI().toURL());
      }

      return urls.toArray(new URL[urls.size()]);

    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }
}