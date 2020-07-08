package com.lancethomps.mavencleanup;

import static com.lancethomps.mavencleanup.MavenCleanupUtils.fullPath;
import static com.lancethomps.mavencleanup.MavenCleanupUtils.printerr;
import static com.lancethomps.mavencleanup.MavenCleanupUtils.println;

import java.io.File;
import java.io.FileFilter;
import java.text.ParseException;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CacheWalker {

  private static final int SNAPSHOT_LEN = "SNAPSHOT".length();
  private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";
  private static final Pattern SNAPSHOT_VERSION_PATTERN = Pattern.compile("(\\d{8})\\.(\\d{6})-(\\d+)(.+)");
  private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)(\\.)?(\\d+)?(\\.)?(.+)?(-)?(.+)?");
  private final boolean debugMode;
  private final boolean verbose;
  private long deleted;
  private long failedToDelete;
  private long reclaimedSpace;

  CacheWalker(boolean verbose, boolean debugMode) {
    this.verbose = verbose;
    this.debugMode = debugMode;
  }

  public long getDeleted() {
    return deleted;
  }

  public long getFailedToDelete() {
    return failedToDelete;
  }

  public long getReclaimedSpace() {
    return reclaimedSpace;
  }

  public int processDirectory(File cacheDir) {
    int exitCode = 0;

    File[] versions = cacheDir.listFiles(new DirPatternFilter(VERSION_PATTERN, false));

    for (File versionDir : versions) {
      if (versionDir.getName().endsWith(SNAPSHOT_SUFFIX)) {
        cleanSnapshotDir(versionDir);
      }
    }

    File[] subDirs = cacheDir.listFiles(new DirPatternFilter(VERSION_PATTERN, true));

    for (File subdir : subDirs) {
      exitCode = Math.max(exitCode, processDirectory(subdir));
    }

    return exitCode;
  }

  private void cleanSnapshotDir(File versionDir) {
    String artifactId = versionDir.getParentFile().getName();
    String artifactBaseVersion = versionDir.getName().substring(0, versionDir.getName().length() - SNAPSHOT_LEN);

    String filenamePrefix = artifactId + '-' + artifactBaseVersion;

    File[] timestampedFiles = versionDir.listFiles(new TimestampedFileFilter(filenamePrefix));
    if (timestampedFiles != null && timestampedFiles.length > 0) {
      String versionToKeep = getLatestVersion(timestampedFiles, filenamePrefix);
      if (versionToKeep != null) {
        String filePrifixToKeep = filenamePrefix + versionToKeep;

        for (File file : timestampedFiles) {
          if (file.getName().startsWith(filePrifixToKeep)) {
            continue;
          }

          long fileSize = file.length();

          if (deleteOrDebug(file)) {
            if (verbose) {
              println("%sRemoved %s", debugMode ? "[DEBUG] " : "", file.getAbsolutePath());
            }
            deleted++;
            reclaimedSpace += fileSize;
          } else {
            failedToDelete++;
            printerr("Failed to delete file '%s'", fullPath(file));
          }
        }
      }
    }
  }

  private boolean deleteOrDebug(File file) {
    if (debugMode) {
      return true;
    }
    return file.delete();
  }

  private String getLatestVersion(File[] timestampedFiles, String filenamePrefix) {
    TreeSet<SnapshotUniqueVersion> versions = new TreeSet<SnapshotUniqueVersion>();
    int prefixLen = filenamePrefix.length();

    for (File file : timestampedFiles) {
      String filenamerest = file.getName().substring(prefixLen);
      Matcher m = SNAPSHOT_VERSION_PATTERN.matcher(filenamerest);
      if (m.matches()) {
        String dateString = m.group(1);
        String timeString = m.group(2);
        String buildString = m.group(3);
        try {
          SnapshotUniqueVersion ver = new SnapshotUniqueVersion(dateString, timeString, buildString);
          versions.add(ver);
        } catch (ParseException e) {
          printerr(e, "Failed to parse filename '%s'.", file.getName());
        }
      }
    }

    if (versions.size() > 0) {
      SnapshotUniqueVersion latestVersion = versions.last();
      return latestVersion.toString();
    }

    return null;
  }

  private static final class DirPatternFilter implements FileFilter {

    private final boolean notPattern;
    private final Pattern pattern;

    DirPatternFilter(Pattern pattern, boolean notPattern) {
      this.pattern = pattern;
      this.notPattern = notPattern;
    }

    public boolean accept(File pathname) {
      if (pathname.isDirectory()) {
        Matcher m = pattern.matcher(pathname.getName());
        boolean matches = m.matches();
        return (notPattern) != matches;
      }

      return false;
    }

  }

  private static final class TimestampedFileFilter implements FileFilter {

    private final String prefix;

    TimestampedFileFilter(String prefix) {
      this.prefix = prefix;
    }

    public boolean accept(File pathname) {
      if (pathname.isFile()) {
        String fileName = pathname.getName();

        if (fileName.startsWith(prefix)) {
          String nameRest = fileName.substring(prefix.length());
          Matcher m = SNAPSHOT_VERSION_PATTERN.matcher(nameRest);
          return m.matches();
        }
      }
      return false;
    }

  }

}
