package com.github.bianchui.ff.utils;

public class RenamerUtil {
  /**
   * class rename have 2 pass
   * first pass in renameAllClasses()
   * second pass after rename() in loadClasses() to rename inner classes
   * @param shortName
   * @param fullName
   * @return is the first pass
   */
  public static boolean isRenameAllPass(String shortName, String fullName) {
    if (!fullName.endsWith(shortName)) {
      return false;
    }
    if (fullName.length() == shortName.length()) {
      return true;
    }
    return fullName.charAt(fullName.length() - shortName.length() - 1) == '/';
  }
}
