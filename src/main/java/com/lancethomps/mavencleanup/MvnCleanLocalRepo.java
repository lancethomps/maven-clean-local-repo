package com.lancethomps.mavencleanup;

import static com.lancethomps.mavencleanup.MavenCleanupUtils.fullPath;
import static com.lancethomps.mavencleanup.MavenCleanupUtils.printerr;
import static com.lancethomps.mavencleanup.MavenCleanupUtils.printlnWithPrefix;

import java.io.File;
import java.util.concurrent.Callable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.text.StringSubstitutor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "mvn_clean_local_repo", mixinStandardHelpOptions = true, version = "1.0")
public class MvnCleanLocalRepo implements Callable<Integer> {

  private static final long KB = 1024L;
  private static final long MB = KB * 1024L;
  private static final long GB = MB * 1024L;
  @Option(names = {"--dir"})
  private File baseDir;
  @Option(names = {"--debug"})
  private boolean debugMode;
  @Option(names = {"-v", "--verbose"})
  private boolean verbose;

  public static void main(String... args) {
    int exitCode = new CommandLine(new MvnCleanLocalRepo()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() throws Exception {
    MavenCleanupUtils.setDebugMode(debugMode);
    if (baseDir == null) {
      baseDir = findBaseDir();
    }
    if (!isValidCache(baseDir)) {
      return 3;
    }

    CacheWalker walker = new CacheWalker(verbose, debugMode, baseDir);
    printlnWithPrefix("Cleaning Maven local cache at '%s'", fullPath(baseDir));
    int exitCode = walker.call();

    printlnWithPrefix("Total deleted %s file(s).", walker.getDeleted());
    printlnWithPrefix("Reclaimed space %s", getHrSize(walker.getReclaimedSpace()));
    if (walker.getFailedToDelete() > 0) {
      printlnWithPrefix("Failed to delete %s file(s).", walker.getFailedToDelete());
    }

    return exitCode;
  }

  private File findBaseDir() {
    File settingsXmlFile = new File(new File(System.getProperty("user.home"), ".m2"), "settings.xml");
    try {
      if (settingsXmlFile.exists() && settingsXmlFile.isFile()) {
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = docBuilder.parse(settingsXmlFile);
        Element settingsElement = doc.getDocumentElement();

        NodeList nodes = settingsElement.getElementsByTagName("localRepository");
        if (nodes != null && nodes.getLength() > 0) {

          if (nodes.getLength() > 1) {
            printerr("settings.xml file contains %s <localRepository> tags, using the first one.", nodes.getLength());
          }

          Element localRepositoryElement = (Element) nodes.item(0);

          String mavenCachePath = getTextValue(localRepositoryElement);
          String expandedPath = StringSubstitutor.createInterpolator().replace(mavenCachePath).replace(
              "${env.HOME}",
              System.getProperty("user.home")
          );

          File extractedBaseDir = new File(expandedPath);
          if (extractedBaseDir.exists()) {
            if (extractedBaseDir.isDirectory()) {
              return extractedBaseDir;
            }
            printerr("Maven local repository path '%s' is not a folder", mavenCachePath);
          } else {
            printerr("Maven local repository path '%s' is invalid", mavenCachePath);
          }
        }
      } else {
        printerr("Failed to locate maven settings file at '%s'", fullPath(settingsXmlFile));
      }
    } catch (Throwable e) {
      printerr(e, "Failed to parse maven settings file ");
    }
    return new File(new File(System.getProperty("user.home"), ".m2"), "repository");
  }

  private String getHrSize(long reclaimedSpace) {
    String unit = "Byte(s)";
    double value = (double) reclaimedSpace;

    if (reclaimedSpace >= GB) {
      value = value / (double) GB;
      unit = "GB";
    } else if (reclaimedSpace >= MB) {
      value = value / (double) MB;
      unit = "MB";
    } else if (reclaimedSpace >= KB) {
      value = value / (double) KB;
      unit = "KB";
    }

    return String.format("%1$.2f %2$s", value, unit);
  }

  private String getTextValue(Element node) {
    NodeList children = node.getChildNodes();
    Text textNode = null;

    for (int i = 0; i < children.getLength(); i++) {
      Node item = children.item(i);
      if (item instanceof Text) {
        textNode = (Text) item;
        break;
      }
    }

    if (textNode != null) {
      return textNode.getNodeValue();
    }

    return "";
  }

  private boolean isValidCache(File cacheDir) {
    return cacheDir.isDirectory();
  }

}
