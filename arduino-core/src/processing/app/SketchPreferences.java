/**
 * Copyright 2019 Andrew L. Heilveil (github/980f)
 * (M.I.T. License)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package processing.app;

import cc.arduino.packages.BoardPort;
import processing.app.debug.TargetBoard;
import processing.app.debug.TargetPackage;
import processing.app.debug.TargetPlatform;
import processing.app.helpers.PreferencesMap;
import processing.app.legacy.PApplet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import static processing.app.I18n.format;
import static processing.app.I18n.tr;

/**
 * Created to aid in per-sketch board selection via a file in each sketch folder.
 *
 * per-sketch preferences, use case: multiple arduino projects that work together but use different boards
 *
 * Extended from PreferencesMap to mimic PreferencesData global object
 */
public class SketchPreferences {

  private static final String target_package = "target_package";
  private static final String target_platform = "target_platform";
  private static final String board = "board";

  public static final String serial = "serial.";

  public static final String serial_port = serial + "port";
  public static final String serial_unbuffered = serial + "unbuffered";

  public static final String serial_baud = serial + "debug_rate";

  //these are not well supported, they also are pretty universal.
  public static final String serial_parity = serial + "parity";
  public static final String serial_bits = serial + "databits";
  public static final String serial_stops = serial + "stopbits";
  public static final String serial_RTS = serial + "disableRTS";
  public static final String serial_DTR = serial + "disableDTR";

  public PreferencesMap prefs;

  public TargetBoard getBoard() {
    return getBoard(prefs);
  }

  public void setBoard(TargetBoard board) {
    recordBoard(board, prefs);
  }

  /**
   * @returns whether this sketch has a name for a port.
   */
  public boolean hasPort() {
    final boolean havePort = prefs != null && prefs.containsKey(serial_port);
    if (havePort) {//set defaults
      prefs.putIfAbsent(serial_baud, "9600");
      prefs.putIfAbsent(serial_parity, "N");
      prefs.putIfAbsent(serial_bits, "8");
      prefs.putIfAbsent(serial_stops, "1");
      prefs.putIfAbsent(serial_RTS, "false");
      prefs.putIfAbsent(serial_DTR, "false");
    }
    return havePort;
  }

  /**
   * @returns an opened serial port using local parameters.
   */
  public Serial getSerial() throws SerialException {
    if (prefs == null) {
      return null;
    }
    //I tried subTree but that doesn't work well for setting defaults so ... just made a bunch of concatenated names.
    return new Serial(prefs.get(serial_port),
      Integer.parseInt(prefs.get(serial_baud)),
      prefs.get(serial_parity).charAt(0),
      Integer.parseInt(prefs.get(serial_bits)),
      Float.parseFloat(prefs.get(serial_stops)),
      !prefs.getBoolean(serial_RTS), //the '!' here are traditional.
      !prefs.getBoolean(serial_DTR)
    );//
  }

  public void setPort(BoardPort bort) {
    prefs.put(serial_port, bort.getAddress());
//    prefs.put(serial_baud,);
//    prefs.put(serial_parity,"N");
//    prefs.put(serial_bits ,"8");
//    prefs.put(serial_stops ,"1");
//    prefs.put(serial_RTS,"false");
//    prefs.put(serial_DTR,"false");
  }

  public void load(final File prefsfile) throws IOException {
    if (prefs == null) {
      prefs = new PreferencesMap();
    }
    prefs.load(prefsfile);
  }

  public void save(final File prefsfile) {
    save(prefsfile, prefs, false);//sorted:false: we don't care about order so keep what the user gave us.
  }

  public static TargetBoard getBoard(PreferencesMap prefs) {
    if (prefs == null) {
      prefs = PreferencesData.getMap();
    }
    String selPackage = prefs.get(target_package);
    String selPlatform = prefs.get(target_platform);
    String selBoard = prefs.get(board);

    try {
      final TargetPackage targetPackage = BaseNoGui.getTargetPackage(selPackage);
      final TargetPlatform targetPlatform = targetPackage.get(selPlatform);
      return targetPlatform.getBoard(selBoard);
    } catch (NullPointerException ignored) {
      //we progress as deeply as we can then live with a partial object if input is bad.
      return null;
    }
  }

  public static void recordBoard(TargetBoard board, PreferencesMap prefs) {
    if (prefs == null) {
      prefs = PreferencesData.getMap();
    }

    final TargetPlatform targetPlatform = board.getContainerPlatform();
    final TargetPackage targetPackage = targetPlatform.getContainerPackage();

    prefs.put(target_package, targetPackage.getId());
    prefs.put(target_platform, targetPlatform.getId());
    prefs.put(SketchPreferences.board, board.getId());
  }

  public static void save(File preferencesFile, PreferencesMap prefs, final boolean sorted) {
    try (PrintWriter writer = PApplet.createWriter(preferencesFile)) {
      if (sorted) {
        String[] keys = prefs.keySet().toArray(new String[0]);
        Arrays.sort(keys);
        for (String key : keys) {
          if (key.startsWith("runtime.")) {
            continue;
          }
          writer.println(key + "=" + prefs.get(key));
        }
      } else {
        prefs.forEach((key, v) -> {
          if (!key.startsWith("runtime.")) {//keeping with main preferences file convention so that we can share this code (someday)
            writer.println(key + "=" + v);
          }
        });
      }
    } catch (IOException e) {
      System.err.println(format(tr("Could not write preferences file: {0}"), e.getMessage()));
    }
  }
}
