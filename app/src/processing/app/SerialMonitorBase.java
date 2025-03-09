package processing.app;

import cc.arduino.packages.BoardPort;
import processing.app.legacy.PApplet;

import java.awt.*;
import java.awt.event.ActionEvent;

import static processing.app.I18n.tr;

/** [980f]
 *  making a base class for the serial text and serial plotter allows us to remove quite a bit of conflict code
 * If we only have one object then we don't need to negotiate which has control of the serial port.
 * A more major rework would be to have a serial monitor object that sends bytes to a serial display object, allowing both to exist at the same time.
 *
 * Todo: extract line ending dialog from serial plotter and add to text viewer.
 * */

public class SerialMonitorBase extends AbstractTextMonitor {
  private static final int COMMAND_HISTORY_SIZE = 100;
  protected final CommandHistory commandHistory =
    new CommandHistory(SerialMonitorBase.COMMAND_HISTORY_SIZE);
  protected Serial serial;
  protected int serialRate;

  /**
   * until we harmonize all behavior of the two we wish to do a few tweaks in this base class rather than do virtual functions for relatively trivial things
   */
  public boolean isPlotter() {
    return this instanceof SerialPlotter;
  }

  public SerialMonitorBase(BoardPort boardPort) {
    super(boardPort);
    unbuffered = PreferencesData.getBoolean("serial.unbuffered", false);
    serialRate = PreferencesData.getInteger("serial.debug_rate");
    serialRates.setSelectedItem(serialRate + " " + tr("baud"));
    onSerialRateChange((ActionEvent event) -> {
      String wholeString = (String) serialRates.getSelectedItem();
      String rateString = wholeString.substring(0, wholeString.indexOf(' '));
      serialRate = Integer.parseInt(rateString);
      PreferencesData.set("serial.debug_rate", rateString);
      if (serial != null) {
        try {
          close();
          Thread.sleep(100); // Wait for serial port to properly close
          open();
        } catch (InterruptedException e) {
          // noop
        } catch (Exception e) {
          System.err.println(e);
        }
      }
    });

    onSendCommand((ActionEvent event) -> {
      String command = textField.getText();
      if(!unbuffered) {//[980f] don't resend, but do pass into history mechanism.
        send(command);
      }
      commandHistory.addCommand(command);
      textField.setText("");
    });

    onClearCommand((ActionEvent event) -> textArea.setText(""));
  }

  protected void send(String s) {
    if (serial != null) {
      switch (lineEndings.getSelectedIndex()) {
        case 1:
          s += "\n";
          break;
        case 2:
          s += "\r";
          break;
        case 3:
          s += "\r\n";
          break;
        default:
          break;
      }
      if ("".equals(s) && lineEndings.getSelectedIndex() == 0 && !PreferencesData.has("runtime.line.ending.alert.notified")) {
        noLineEndingAlert.setForeground(Color.RED);
        PreferencesData.set("runtime.line.ending.alert.notified", "true");
      }
      serial.write(s);
    }
  }

  @Override
  public void open() throws Exception {
    //980f:diff:this.open();
    super.open();
    if (serial != null) return;

    serial = new Serial(getBoardPort().getAddress(), serialRate) {
      @Override
      protected void message(char buff[], int n) {
        addToUpdateBuffer(buff, n);
      }
    };
  }

  @Override
  public void close() throws Exception {
    super.close();//plotter conditionalized this on serial!=null, seems that was a bug or this is a bug.
    if (serial != null) {
      int[] location = getPlacement();
      String locationStr = PApplet.join(PApplet.str(location), ",");
      PreferencesData.set("last.serial.location", locationStr);
      if (PreferencesData.getBoolean("serial.onclose.forget", true)) {//[980f]//why clear? I want to see what was going on just before the device cratered!
        if (textArea != null) {//todo: is this null for serialPlotter?
          textArea.setText("");
        }
      }
      serial.dispose();
      serial = null;
    }
  }

}
