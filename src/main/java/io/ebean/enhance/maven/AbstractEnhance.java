package io.ebean.enhance.maven;

import io.ebean.enhance.Transformer;
import io.ebean.enhance.ant.OfflineFileTransform;
import io.ebean.enhance.ant.TransformationListener;
import io.ebean.enhance.common.EnhanceContext;
import io.ebean.enhance.common.SummaryInfo;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Base ebean enhancement for both src/main and src/test.
 */
abstract class AbstractEnhance extends AbstractMojo {

  /**
   * The class path used to read related classes.
   */
  @Parameter(property = "project.compileClasspathElements", required = true, readonly = true)
  List<String> compileClasspathElements;

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

  public abstract void execute() throws MojoExecutionException;

  void executeFor(String classSource) throws MojoExecutionException {

    ClassLoader loader = buildClassLoader(classSource);

    Transformer transformer = new Transformer(loader, transformArgs);
    EnhanceContext ctx = transformer.getEnhanceContext();

    final Log log = getLog();
    log.info("classSource=" + classSource + "  transformArgs=" + nullToEmpty(transformArgs));
    log.info(ctx.getPackagesSummary());
    ctx.collectSummary();

    OfflineFileTransform ft = new OfflineFileTransform(transformer, loader, classSource);
    ft.setListener(new TransformationListener() {
      public void logEvent(String msg) {
        log.info(msg);
      }

      public void logError(String msg) {
        log.error(msg);
      }
    });

    ft.process(packages);
    final SummaryInfo summaryInfo = ctx.getSummaryInfo();
    log.info("loaded resources: " + summaryInfo.loadedResources());
    log.info(trim(summaryInfo.entities()));
    log.info(trim(summaryInfo.queryBeans()));
    log.info(trim(summaryInfo.transactional()));
    log.info(trim(summaryInfo.queryCallers()));
  }

  private static String trim(String val) {
    if (val.length() > 290) {
      return val.substring(0, 289) + " ...";
    }
    return val;
  }

  /**
   * Return a null string as empty (for pretty output on valid null parameters).
   */
  private String nullToEmpty(String val) {
    return (val == null) ? "" : val;
  }

  private ClassLoader buildClassLoader(String classSource) {
    URL[] urls = buildClassPath(classSource);
    return URLClassLoader.newInstance(urls, Thread.currentThread().getContextClassLoader());
  }

  /**
   * Return the class path using project compileClasspathElements.
   */
  private URL[] buildClassPath(String classSource) {

    try {
      List<URL> urls = new ArrayList<>(compileClasspathElements.size());

      Log log = getLog();

      for (String element : compileClasspathElements) {
        if (log.isDebugEnabled()) {
          log.debug("ClasspathElement: " + element);
        }
        urls.add(new File(element).toURI().toURL());
      }
      log.debug("add source: " + classSource);
      urls.add(new File(classSource).toURI().toURL());

      return urls.toArray(new URL[0]);

    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }
}