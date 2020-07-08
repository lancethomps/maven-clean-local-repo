package com.lancethomps.mavencleanup;

import java.io.File;

public class MavenCleanupUtils {

  private static boolean debugMode;

  public static String fullPath(File file) {
    if (file != null) {
      try {
        return file.getCanonicalPath();
      } catch (Throwable e) {
        printerr(e, "Issue getting canonical path for file [%s] - returning absolute path.", file);
        return file.getAbsolutePath();
      }
    }
    return null;
  }

  public static String getMessage(final String message, final Object... formatArgs) {
    return (message == null) || (formatArgs == null) || (formatArgs.length == 0) ? message : String.format(message, formatArgs);
  }

  public static boolean isDebugMode() {
    return debugMode;
  }

  public static void setDebugMode(boolean debugMode) {
    MavenCleanupUtils.debugMode = debugMode;
  }

  public static void printWithoutNewline(final Object message, final Object... formatArgs) {
    System.out.print(getMessage(message == null ? null : message.toString(), formatArgs));
  }

  public static void printerr(final Throwable exception, final Object message, final Object... formatArgs) {
    System.err.println(getMessage(message == null ? null : message.toString(), formatArgs));
    if (exception != null) {
      exception.printStackTrace(System.err);
    }
  }

  public static void printerr(final Object message, final Object... formatArgs) {
    printerr((Throwable) null, message, formatArgs);
  }

  public static void println(final Object message, final Object... formatArgs) {
    System.out.println(getMessage(message == null ? null : message.toString(), formatArgs));
  }

  public static void printlnWithPrefix(final Object message, final Object... formatArgs) {
    System.out.println((debugMode ? "[DEBUG] " : "") + getMessage(message == null ? null : message.toString(), formatArgs));
  }

}
