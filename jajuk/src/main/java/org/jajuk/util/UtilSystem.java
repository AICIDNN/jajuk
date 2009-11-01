/*
 *  Jajuk
 *  Copyright (C) 2003-2008 The Jajuk Team
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *  $Revision: 3132 $
 */
package org.jajuk.util;

import ext.service.io.NativeFunctionsUtils;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.swing.ImageIcon;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jajuk.Main;
import org.jajuk.services.core.SessionService;
import org.jajuk.util.error.JajukException;
import org.jajuk.util.filters.DirectoryFilter;
import org.jajuk.util.filters.KnownTypeFilter;
import org.jajuk.util.log.Log;

/**
 * Set of convenient methods for system and IO
 */
public final class UtilSystem {

  private static final String LOCAL_IP = "127.0.0.1";

  /** MPlayer status possible values * */
  public static enum MPlayerStatus {
    MPLAYER_STATUS_OK, MPLAYER_STATUS_NOT_FOUND, MPLAYER_STATUS_WRONG_VERSION, MPLAYER_STATUS_JNLP_DOWNLOAD_PBM
  }

  /** Current date cached (for performances) * */
  public static final Date TODAY = new Date();

  /** Central random object for all Jajuk * */
  private static final Random RANDOM = new Random();

  /**
   * Are we under Linux ? *
   */
  private static final boolean UNDER_LINUX;
  /**
   * Are we under MAC OS Intel ? *
   */
  private static final boolean UNDER_OSX_INTEL;
  /**
   * Are we under MAC OS power ? *
   */
  private static final boolean UNDER_OSX_POWER;
  /**
   * Are we under Windows ? *
   */
  private static final boolean UNDER_WINDOWS;
  /**
   * Are we under Windows 32 bits ? *
   */
  private static final boolean UNDER_WINDOWS_32BIT;
  /**
   * Are we under Windows 64 bits ? *
   */
  private static final boolean UNDER_WINDOWS_64BIT;
  /**
   * Directory filter used in refresh
   */
  private static JajukFileFilter dirFilter;
  /**
   * File filter used in refresh
   */
  private static JajukFileFilter fileFilter;

  // Computes OS detection operations for perf reasons (can be called in loop
  // in refresh method for ie)
  static {
    final String sOS = (String) System.getProperties().get("os.name");
    // os.name can be null with JWS under MacOS
    UNDER_WINDOWS = ((sOS != null) && (sOS.trim().toLowerCase(Locale.getDefault()).lastIndexOf(
        "windows") != -1));
  }

  static {
    UNDER_WINDOWS_32BIT = UtilSystem.isUnderWindows()
        && System.getProperties().get("sun.arch.data.model").equals("32");
  }

  static {
    UNDER_WINDOWS_64BIT = UtilSystem.isUnderWindows()
        && !System.getProperties().get("sun.arch.data.model").equals("32");
  }

  static {
    final String sOS = (String) System.getProperties().get("os.name");
    // os.name can be null with JWS under MacOS
    UNDER_LINUX = ((sOS != null) && (sOS.trim().toLowerCase(Locale.getDefault()).lastIndexOf(
        "linux") != -1));
  }

  static {
    final String sArch = System.getProperty("os.arch");
    UNDER_OSX_INTEL = org.jdesktop.swingx.util.OS.isMacOSX()
        && ((sArch != null) && sArch.matches(".*86"));
  }

  static {
    final String sArch = System.getProperty("os.arch");
    UNDER_OSX_POWER = org.jdesktop.swingx.util.OS.isMacOSX()
        && ((sArch != null) && !sArch.matches(".*86"));
  }

  /** Icons cache */
  static Map<String, ImageIcon> iconCache = new HashMap<String, ImageIcon>(200);
  /** Mplayer exe path */
  private static File mplayerPath = null;
  /** current class loader */
  private static ClassLoader classLoader = null;

  /**
   * private constructor to avoid instantiating utility class
   */
  private UtilSystem() {
  }

  /**
   * Save a file in the same directory with name <filename>_YYYYmmddHHMM.xml and
   * with a given maximum Mb size for the file and its backup files
   * 
   * @param file
   *          The file to back up
   */
  public static void backupFile(final File file, final int iMB) {
    try {
      if (Integer.parseInt(Conf.getString(Const.CONF_BACKUP_SIZE)) <= 0) {
        // 0 or less means no backup
        return;
      }
      // calculates total size in MB for the file to backup and its
      // backup files
      long lUsedMB = 0;
      final List<File> alFiles = new ArrayList<File>(10);
      final File[] files = new File(file.getAbsolutePath()).getParentFile().listFiles();
      if (files != null) {
        for (final File element : files) {
          if (element.getName().indexOf(UtilSystem.removeExtension(file.getName())) != -1) {
            lUsedMB += element.length();
            alFiles.add(element);
          }
        }
        // sort found files
        alFiles.remove(file);
        Collections.sort(alFiles);
        // too much backup files, delete older
        if (((lUsedMB - file.length()) / 1048576 > iMB) && (alFiles.size() > 0)) {
          final File fileToDelete = alFiles.get(0);
          if ((fileToDelete != null) && (!fileToDelete.delete())) {
            Log.warn("Could not delete file " + fileToDelete);
          }
        }
      }
      // backup itself using nio, file name is
      // collection-backup-yyyMMdd.xml
      final String sExt = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
      final File fileNew = new File(UtilSystem.removeExtension(file.getAbsolutePath()) + "-backup-"
          + sExt + "." + UtilSystem.getExtension(file));
      final FileChannel fcSrc = new FileInputStream(file).getChannel();
      try {
        final FileChannel fcDest = new FileOutputStream(fileNew).getChannel();
        try {
          fcDest.transferFrom(fcSrc, 0, fcSrc.size());
        } finally {
          fcDest.close();
        }
      } finally {
        fcSrc.close();
      }
    } catch (final IOException ie) {
      Log.error(ie);
    }
  }

  /**
   * Copy a file to another file
   * 
   * @param file :
   *          file to copy
   * @param fNew :
   *          destination file
   * @throws JajukException
   * @throws IOException
   */
  public static void copy(final File file, final File fNew) throws JajukException, IOException {
    Log.debug("Copying: {{" + file.getAbsolutePath() + "}}  to : " + fNew.getAbsolutePath());
    if (!file.exists() || !file.canRead()) {
      throw new JajukException(9, file.getAbsolutePath(), null);
    }

    FileUtils.copyFile(file, fNew);
    
    // Display a warning if copied file is void as it can happen with full
    // disks
    if (fNew.length() == 0) {
      Log.warn("Copied file is void: {{" + file.getAbsolutePath() + "}}");
    }
  }

  /**
   * Copy a file
   * 
   * @param file :
   *          source file
   * @param sNewName :
   *          dest file
   * @throws JajukException
   * @throws IOException
   */
  public static void copy(final File file, final String sNewName) throws JajukException,
      IOException {
    Log.debug("Renaming: {{" + file.getAbsolutePath() + "}}  to : " + sNewName);
    final File fileNew = new File(new StringBuilder(file.getParentFile().getAbsolutePath()).append(
        '/').append(sNewName).toString());
    if (!file.exists() || !file.canRead()) {
      throw new JajukException(9, file.getAbsolutePath(), null);
    }
    
    FileUtils.copyFile(file, fileNew);
  }

  /**
   * Copy a URL resource to a file We don't use nio but Buffered Reader / writer
   * because we can only get channels from a FileInputStream that can be or not
   * be in a Jar (production / test)
   * 
   * @param src
   *          source designed by URL
   * @param dest
   *          destination file full path
   * @throws IOException
   *           If the src or dest cannot be opened/created.
   */
  public static void copy(final URL src, final String dest) throws IOException {
    final BufferedReader br = new BufferedReader(new InputStreamReader(src.openStream()));
    try {
      final BufferedWriter bw = new BufferedWriter(new FileWriter(dest));
      try {
        String sLine = null;
        do {
          sLine = br.readLine();
          if (sLine != null) {
            bw.write(sLine);
            bw.newLine();
          }
        } while (sLine != null);
        bw.flush();
      } finally {
        bw.close();
      }
    } finally {
      br.close();
    }
  }

  /**
   * Copy recursively files and directories
   * 
   * @param str
   *          The source to copy from, can be a directory or a file
   * @param dst
   * @throws IOException
   * @throws JajukException
   * @throws IOException
   */
  public static void copyRecursively(final File src, final File dst) throws JajukException,
      IOException {
    if (src.isDirectory()) {
      if (!dst.mkdirs()) {
        Log.warn("Could not create directory structure " + dst.toString());
      }
      final String list[] = src.list();
      for (final String element : list) {
        final String dest1 = dst.getAbsolutePath() + '/' + element;
        final String src1 = src.getAbsolutePath() + '/' + element;
        UtilSystem.copyRecursively(new File(src1), new File(dest1));
      }
    } else {
      UtilSystem.copy(src, dst);
    }
  }

  /**
   * Copy a file to given directory
   * 
   * @param file :
   *          file to copy
   * @param directory :
   *          destination directory
   * @return destination file
   * @throws JajukException
   * @throws IOException
   */
  public static void copyToDir(final File file, final File directory) throws JajukException,
      IOException {
    Log.debug("Copying: {{" + file.getAbsolutePath() + "}}  to : " + directory.getAbsolutePath());
    if (!file.exists() || !file.canRead()) {
      throw new JajukException(9, file.getAbsolutePath(), null);
    }

    FileUtils.copyFileToDirectory(file, directory);
  }

  /**
   * Create empty file
   * 
   * @param sFullPath
   * @throws IOException
   */
  public static void createEmptyFile(final File file) throws IOException {
    final OutputStream fos = new FileOutputStream(file);
    try {
      fos.write(new byte[0]);
    } finally {
      fos.close();
    }
  }

  /**
   * Delete a directory
   * 
   * @param dir :
   *          source directory
   * @throws IOException
   */
  public static void deleteDir(final File dir) throws IOException {
    Log.debug("Deleting: {{" + dir.getAbsolutePath() + "}}");
    if (dir.isDirectory()) {
      for (final File file : dir.listFiles()) {
        if (file.isDirectory()) {
          UtilSystem.deleteDir(file);
        } else {
          UtilSystem.deleteFile(file);
        }
      }
      if (!dir.delete()) {
        Log.warn("Could not delete directory " + dir);
      }
    } else {
      UtilSystem.deleteFile(dir);
    }
    return;
  }

  /**
   * Delete a file
   * 
   * @param file :
   *          source file
   * @throws IOException
   */
  public static void deleteFile(final File file) throws IOException {
    Log.debug("Deleting: {{" + file.getAbsolutePath() + "}}");
    if (file.isFile() && file.exists()) {
      if (!file.delete()) {
        Log.warn("Could not delete file " + file);
      }
      // check that file has been really deleted (sometimes,
      // we get no exception)
      if (file.exists()) {
        throw new IOException("File" + file.getAbsolutePath() + " still exists");
      }
    } else {// not a file, must have a problem
      throw new IOException("File " + file.getAbsolutePath() + " didn't exist");
    }
    return;
  }

  /**
   * Extract files from current jar to "cache/internal" directory
   * <p>
   * Thanks several websites, especially
   * http://www.developer.com/java/other/article.php/607931
   * 
   * @param entryName
   *          name of the file to extract. Example: img.png
   * @param file
   *          destination PATH
   * @throws IOException
   */
  public static void extractFile(final String entryName, final String destName) throws IOException {
    JarFile jar = null;
    // Open the jar.
    try {
      final File dir = new File(UtilSystem.getJarLocation(Main.class).toURI()).getParentFile();
      // We have to call getParentFile() method because the toURI() method
      // returns an URI than is not always valid (contains %20 for spaces
      // for instance)
      final File jarFile = new File(dir.getAbsolutePath() + "/jajuk.jar");
      Log.debug("Open jar: " + jarFile.getAbsolutePath());
      jar = new JarFile(jarFile);
    } catch (final URISyntaxException e) {
      Log.error(e);
      return;
    }
    try {
      // Get the entry and its input stream.
      final JarEntry entry = jar.getJarEntry(entryName);
      // If the entry is not null, extract it. Otherwise, print a
      // message.
      if (entry != null) {
        // Get an input stream for the entry.
        final InputStream entryStream = jar.getInputStream(entry);
        try {
          // Create the output file (clobbering the file if it
          // exists).
          final OutputStream file = new FileOutputStream(SessionService
              .getConfFileByPath(Const.FILE_CACHE + '/' + Const.FILE_INTERNAL_CACHE + '/'
                  + destName));
          try {
            // Allocate a buffer for reading the entry data.
            final byte[] buffer = new byte[1024];
            int bytesRead;
            // Read the entry data and write it to the output file.
            while ((bytesRead = entryStream.read(buffer)) != -1) {
              file.write(buffer, 0, bytesRead);
            }
            file.flush();
          } catch (final IOException e) {
            Log.error(e);
          } finally {
            file.close();
          }
        } catch (final IOException e) {
          Log.error(e);
        } finally {
          entryStream.close();
        }
      } else {
        Log.debug(entryName + " not found.");
      } // end if
    } catch (final IOException e) {
      Log.error(e);
    } finally {
      jar.close();
    }
  }

  /**
   * Get a file extension
   * 
   * @param file
   * @return
   */
  public static String getExtension(final File file) {
    return UtilSystem.getExtension(file.getName());
  }

  /**
   * Get a file extension (without the dot!).
   * 
   * @param filename
   *          The file to examine.
   * 
   * @return The actual file extension or an empty string if no extension is
   *         found (i.e. no dot in the filename).
   */
  public static String getExtension(final String filename) {
    int dotIndex = filename.lastIndexOf('.');

    // File without point
    if (dotIndex == -1) {
      return "";
    }

    if (dotIndex > 0) {
      return filename.substring(dotIndex + 1, filename.length());
    } else {
      // File beginning by a point (unix hidden file)
      return filename;
    }
  }

  /**
   * Additional file checksum used to prevent bug #886098. Simply return some
   * bytes read at the middle of the file
   * <p>
   * uses nio api for performances
   * 
   * @return
   */
  public static String getFileChecksum(final File fio) throws JajukException {
    try {
      String sOut = "";
      final FileChannel fc = new FileInputStream(fio).getChannel();
      try {
        final ByteBuffer bb = ByteBuffer.allocate(500);
        fc.read(bb, fio.length() / 2);
        sOut = new String(bb.array());
      } finally {
        fc.close();
      }
      return MD5Processor.hash(sOut);
    } catch (final IOException e) {
      throw new JajukException(103, e);
    }
  }

  /**
   * 
   * @return This box hostname
   */
  public static String getHostName() {
    String sHostname = null;
    // Try to get hostname using the standard way
    try {
      sHostname = InetAddress.getLocalHost().getHostName();
    } catch (final Exception e) {
      Log.debug("Cannot get Hostname using the standard way");
    }
    if (sHostname == null) {
      // Try using IP now
      try {
        final java.net.InetAddress inetAdd = java.net.InetAddress.getByName(LOCAL_IP);
        sHostname = inetAdd.getHostName();
      } catch (final Exception e) {
        Log.debug("Cannot get Hostname by IP");
      }
    }
    // If still no hostname, return a default value
    if (sHostname == null) {
      sHostname = Const.DEFAULT_HOSTNAME;
    }
    return sHostname;
  }

  /**
   * Return url of jar we are executing
   * 
   * @return URL of jar we are executing
   */
  public static URL getJarLocation(final Class<?> cClass) {
    return cClass.getProtectionDomain().getCodeSource().getLocation();
  }

  /**
   * @return MPLayer binary MAC full path
   */
  public static String getMPlayerOSXPath() {
    final String forced = Conf.getString(Const.CONF_MPLAYER_PATH_FORCED);
    if (!StringUtils.isBlank(forced)) {
      return forced;
    } else if (UtilSystem.isUnderOSXintel()
        && new File(Const.FILE_DEFAULT_MPLAYER_X86_OSX_PATH).exists()) {
      return Const.FILE_DEFAULT_MPLAYER_X86_OSX_PATH;
    } else if (UtilSystem.isUnderOSXpower()
        && new File(Const.FILE_DEFAULT_MPLAYER_POWER_OSX_PATH).exists()) {
      return Const.FILE_DEFAULT_MPLAYER_POWER_OSX_PATH;
    } else {
      // Simply return mplayer from PATH, works if app is launch from CLI
      return "mplayer";
    }
  }

  public static UtilSystem.MPlayerStatus getMplayerStatus(final String mplayerPATH) {
    Process proc = null;
    UtilSystem.MPlayerStatus mplayerStatus = UtilSystem.MPlayerStatus.MPLAYER_STATUS_NOT_FOUND;
    try {
      String fullPath = null;
      if ("".equals(mplayerPATH)) {
        fullPath = "mplayer";
      } else {
        fullPath = mplayerPATH;
      }
      Log.debug("Testing path: " + fullPath);
      // check MPlayer release : 1.0pre8 min
      proc = Runtime.getRuntime().exec(new String[] { fullPath, "-input", "cmdlist" }); //$NON-NLS-2$ 
      final BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
      try {
        String line = null;
        mplayerStatus = UtilSystem.MPlayerStatus.MPLAYER_STATUS_WRONG_VERSION;
        for (;;) {
          line = in.readLine();
          if (line == null) {
            break;
          }

          if (line.matches("get_time_pos.*")) {
            mplayerStatus = UtilSystem.MPlayerStatus.MPLAYER_STATUS_OK;
            break;
          }
        }
      } finally {
        in.close();
      }
    } catch (final IOException e) {
      mplayerStatus = UtilSystem.MPlayerStatus.MPLAYER_STATUS_NOT_FOUND;
    }
    return mplayerStatus;
  }

  /**
   * @return MPlayer exe file
   */
  public static File getMPlayerWindowsPath() {
    // Use cache
    if (UtilSystem.mplayerPath != null) {
      return UtilSystem.mplayerPath;
    }
    File file = null;
    // Check in ~/.jajuk directory (used by webstart distribution
    // installers). Test exe size as well to detect unfinished downloads of
    // mplayer.exe in JNLP mode
    file = SessionService.getConfFileByPath(Const.FILE_MPLAYER_EXE);
    if (file.exists() && file.length() == Const.MPLAYER_EXE_SIZE) {
      UtilSystem.mplayerPath = file;
      return UtilSystem.mplayerPath;
    } else {
      // Check in the path where jajuk.jar is executed (all others
      // distributions)
      String sPATH = null;
      try {
        // Extract file name from URL. URI returns jar path, its parent
        // is the bin directory and the right dir is the parent of bin
        // dir
        // Note: When starting from jnlp, next line throws an exception
        // as URI is invalid (contains %20), the method returns null and
        // the file is downloaded again. This url is used only when
        // using stand-alone version
        if (SessionService.isIdeMode()) {
          // If under dev, take mplayer exe file from the packaging
          // directory
          sPATH = "./src/packaging";
        } else {
          sPATH = new File(getJarLocation(Main.class).toURI()).getParentFile().getParentFile()
              .getAbsolutePath();
        }
        // Add MPlayer file name
        file = new File(sPATH + '/' + Const.FILE_MPLAYER_EXE);
        if (file.exists() && file.length() == Const.MPLAYER_EXE_SIZE) {
          UtilSystem.mplayerPath = file;
        } else {
          // For bundle project, Jajuk should check if mplayer was
          // installed along with aTunes. In this case, mplayer is
          // found in sPATH\win_tools\ directory. Hence, changed sPATH
          // Note that we don't test mplayer.exe size in this case
          file = new File(sPATH + "/win_tools/" + Const.FILE_MPLAYER_EXE);
          if (file.exists()) {
            UtilSystem.mplayerPath = file;
          }
        }

      } catch (URISyntaxException e) {
        return UtilSystem.mplayerPath;
      }
    }
    return UtilSystem.mplayerPath; // can be null if none suitable file found
  }

  /**
   * This method intends to cleanup a future filename so it can be created on
   * all operating systems. Windows forbids characters : /\"<>|:*?
   * 
   * @param in
   *          filename
   * @return filename with forbidden characters replaced at best
   */
  public static String getNormalizedFilename(final String in) {
    String out = in.trim();
    // Replace / : < > and \ by -
    out = in.replaceAll("[/:<>\\\\]", "-");
    // Replace * and | by spaces
    out = out.replaceAll("[\\*|]", " ");
    // Remove " and ? characters
    out = out.replaceAll("[\"\\?]", "");
    return out;
  }

  /**
   * Return only the name of a file from a complete URL
   * 
   * @param sPath
   * @return
   */
  public static String getOnlyFile(final String sPath) {
    return new File(sPath).getName();
  }

  /**
   * Resource loading is done this way to meet the requirements for Web Start.
   * http
   * ://java.sun.com/j2se/1.5.0/docs/guide/javaws/developersguide/faq.html#211
   */
  public static URL getResource(final String name) {
    return UtilSystem.getClassLoader().getResource(name);
  }

  /**
   * @param file1
   *          potential ancestor
   * @param file2
   *          potential child
   * @return whether file1 is a file2 ancestor
   */
  public static boolean isAncestor(final File file1, final File file2) {
    File fParent = file2.getParentFile();
    boolean bOut = false;
    while (fParent != null) {
      if (fParent.equals(file1)) {
        bOut = true;
        break;
      }
      fParent = fParent.getParentFile();
    }
    return bOut;
  }

  /**
   * @param file1
   * @param file2
   * @return whether file1 is a file2 descendant
   */
  public static boolean isDescendant(final File file1, final File file2) {
    // a file is a descendant to another file if the other file is it's
    // ancestor...
    return isAncestor(file2, file1);
  }

  /**
   * @return whether we are under Linux
   */
  public static boolean isUnderLinux() {
    return UtilSystem.UNDER_LINUX;
  }

  /**
   * @return whether we are under OS X Intel
   */
  public static boolean isUnderOSXintel() {
    return UtilSystem.UNDER_OSX_INTEL;
  }

  /**
   * @return whether we are under OS X Power
   */
  public static boolean isUnderOSXpower() {
    return UtilSystem.UNDER_OSX_POWER;
  }

  /**
   * @return whether we are under Windows
   */
  public static boolean isUnderWindows() {
    return UtilSystem.UNDER_WINDOWS;
  }

  /**
   * @return whether we are under Windows 32 bits
   */
  public static boolean isUnderWindows32bits() {
    return UtilSystem.UNDER_WINDOWS_32BIT;
  }

  /**
   * @return whether we are under Windows 64 bits
   */
  public static boolean isUnderWindows64bits() {
    return UtilSystem.UNDER_WINDOWS_64BIT;
  }

  /**
   * @param parent
   *          parent directory
   * @param name
   *          file name
   * @return whether the file name is correct on the current filesystem
   */
  public static boolean isValidFileName(final File parent, final String name) {
    // General tests
    if ((parent == null) || (name == null)) {
      return false;
    }
    // only digits or letters, OK, no need to test
    if (!UtilString.containsNonDigitOrLetters(name)) {
      return true;
    }
    final File f = new File(parent, name);
    if (!f.exists()) {
      try {
        // try to create the file
        f.createNewFile();
        // test if the file is seen into the directory
        final File[] files = parent.listFiles();
        boolean b = false;
        for (final File element : files) {
          if (element.getName().equals(name)) {
            b = true;
            break;
          }
        }
        // remove test file
        if (f.exists()) {
          f.delete();
        }
        return b;
      } catch (final IOException ioe) {
        return false;
      }
    } else { // file already exists
      return true;
    }
  }

  /**
   * @return whether we need a full gc or not
   */
  public static boolean needFullFC() {
    final float fTotal = Runtime.getRuntime().totalMemory();
    final float fFree = Runtime.getRuntime().freeMemory();
    final float fLevel = (fTotal - fFree) / fTotal;
    return fLevel >= Const.NEED_FULL_GC_LEVEL;
  }

  /**
   * Open a file and return a string buffer with the file content.
   * 
   * @param path
   *          -File path
   * @return StringBuilder - File content.
   * @throws JajukException -
   *           Throws a JajukException if a problem occurs during the file
   *           access.
   */
  public static StringBuilder readFile(final String path) throws JajukException {
    // Read
    File file = new File(path);
    FileReader fileReader;
    try {
      fileReader = new FileReader(file);
    } catch (final FileNotFoundException e) {
      throw new JajukException(9, path, e);
    }

    try {
      final BufferedReader input = new BufferedReader(fileReader);
      try {
        // Read
        final StringBuilder strColl = new StringBuilder();
        String line = null;
        while ((line = input.readLine()) != null) {
          strColl.append(line);
        }

        return strColl;
      } finally {
        // Close the bufferedReader
        input.close();
      }
    } catch (final IOException e) {
      throw new JajukException(9, path, e);
    }
  }

  /**
   * Open a file from current jar and return a string buffer with the file
   * content.
   * 
   * @param sUrl :
   *          relative file url
   * @return StringBuilder - File content.
   * @throws JajukException
   *           -Throws a JajukException if a problem occurs during the file
   *           access.
   */
  public static StringBuilder readJarFile(final String sURL) throws JajukException {
    // Read
    InputStream is;
    StringBuilder sb = null;
    try {
      is = Main.class.getResourceAsStream(sURL);
      try {
        // Read
        final byte[] b = new byte[200];
        sb = new StringBuilder();
        int i = 0;
        do {
          i = is.read(b, 0, b.length);
          sb.append(new String(b));
        } while (i > 0);
      } finally {
        // Close the bufferedReader
        is.close();
      }
    } catch (final IOException e) {
      throw new JajukException(9, e);
    }
    return sb;

  }

  /**
   * Remove an extension from a file name
   * 
   * @param filename
   * @return filename without extension
   */
  public static String removeExtension(final String sFilename) {
    return sFilename.substring(0, sFilename.lastIndexOf('.'));
  }

  public static ClassLoader getClassLoader() {
    if (UtilSystem.classLoader == null) {
      UtilSystem.classLoader = Thread.currentThread().getContextClassLoader();
    }
    return UtilSystem.classLoader;
  }

  public static JajukFileFilter getDirFilter() {
    if (dirFilter == null) {
      dirFilter = new JajukFileFilter(DirectoryFilter.getInstance());
    }
    return dirFilter;
  }

  public static JajukFileFilter getFileFilter() {
    if (fileFilter == null) {
      fileFilter = new JajukFileFilter(KnownTypeFilter.getInstance());
    }
    return fileFilter;
  }

  /**
   * Replace a string inside a given file
   * 
   * @param file
   *          the file
   * @param oldS
   *          the string to replace
   * @param newS
   *          the new string
   * @param encoding
   *          the encoding of the file
   * @return whether some replacements occurred
   */
  public static boolean replaceInFile(File file, String oldS, String newS, String encoding) {
    try {
      String s = FileUtils.readFileToString(file);
      if (s.indexOf(oldS) != -1) {
        s = s.replaceAll(oldS, newS);
        Writer bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), encoding));
        try {
          bw.write(s);
          bw.flush();
        } finally {
          bw.close();
        }
        return true;
      }
    } catch (IOException e) {
      Log.error(e);
    }
    return false;
  }

  /**
   * This method returns a single random object that can be used anywhere in
   * jajuk It has to be a singleton to get a good shuffling. Indeed, Random()
   * object are seeded by default with current nano date but in some cases, two
   * random could be created at the same exact date in different threads or the
   * same.
   * 
   * 
   * @return Jajuk singleton random object
   */
  public static Random getRandom() {
    return UtilSystem.RANDOM;
  }

  /**
   * Opens a directory with the associated explorer program.
   * <li> Start by trying to open the directory with any provided explorer path
   * </li>
   * <li> Then, try to use the JDIC Desktop class if supported by the platform
   * </li>
   * 
   * Inspired from an aTunes method
   * 
   * @param file
   *          The file that should be opened
   */
  public static void openInExplorer(File directory) {
    final File directoryToOpen;
    /*
     * Needed for UNC filenames with spaces ->
     * http://bugs.sun.com/view_bug.do?bug_id=6550588
     */
    if (isUnderWindows()) {
      directoryToOpen = new File(NativeFunctionsUtils
          .getShortPathNameW(directory.getAbsolutePath()));
    } else {
      directoryToOpen = directory;
    }

    // Try to open the location using the forced explorer path of provided
    if (StringUtils.isNotBlank(Conf.getString(Const.CONF_EXPLORER_PATH))) {
      new Thread("Explorer Open Thread 1") {
        @Override
        public void run() {
          try {
            ProcessBuilder pb = new ProcessBuilder(Conf.getString(Const.CONF_EXPLORER_PATH),
                directoryToOpen.getAbsolutePath());
            pb.start();
          } catch (Exception e) {
            Log.error(e);
            Messages.showErrorMessage(179, directoryToOpen.getAbsolutePath());
          }
        }
      }.start();
    }
    // Try to open the location using the JDIC/JDK Desktop.open method
    // This is not supported on some platforms (Linux/XFCE for ie)
    else if (Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
      new Thread("Explorer Open Thread 2") {
        @Override
        public void run() {
          try {
            Desktop.getDesktop().open(directoryToOpen);
          } catch (Exception e) {
            Log.error(e);
            Messages.showErrorMessage(179, directoryToOpen.getAbsolutePath());
          }
        }
      }.start();
    }
    // Else, display a warning message: we don't support this platform
    else {
      Messages.showErrorMessage(179);
    }
  }

  /**
   * Return whether a process is still running
   * 
   * @param process
   *          the process
   * @return whether the process is still running
   */
  public static boolean isRunning(Process process) {
    try {
      process.exitValue();
      return false;
    } catch (IllegalThreadStateException itse) {
      return true;
    }
  }

  /**
   * Return a process exit value, -100 if the process is stopped
   * 
   * @param process
   *          the process
   * @return the process exit value, -100 if the process is running
   */
  public static int getExitValue(Process process) {
    try {
      return process.exitValue();
    } catch (IllegalThreadStateException itse) {
      return -100;
    }
  }
}
