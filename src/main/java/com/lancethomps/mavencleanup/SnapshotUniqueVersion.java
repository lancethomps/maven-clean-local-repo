package com.lancethomps.mavencleanup;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public final class SnapshotUniqueVersion implements Comparable<SnapshotUniqueVersion> {

  private final int buildNo;
  private final Date timestamp;

  SnapshotUniqueVersion(String date, String time, String buildNo) throws ParseException {
    timestamp = getFormat().parse(date + '.' + time);
    this.buildNo = Integer.parseInt(buildNo);
  }

  private static SimpleDateFormat getFormat() {
    SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd.HHmmss");
    fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
    return fmt;
  }

  public int getBuildNo() {
    return buildNo;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  @Override
  public String toString() {
    return getFormat().format(timestamp) + '-' + Integer.toString(buildNo);
  }

  public int compareTo(SnapshotUniqueVersion o) {
    if (o == null) {
      return 1;
    }

    int result = getTimestamp().compareTo(o.getTimestamp());
    if (result != 0) {
      return result;
    }

    return Integer.compare(getBuildNo(), o.getBuildNo());
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    if (obj instanceof SnapshotUniqueVersion) {
      SnapshotUniqueVersion ver = (SnapshotUniqueVersion) obj;
      return (getTimestamp().getTime() == ver.getTimestamp().getTime()) && (getBuildNo() == ver.getBuildNo());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

}
