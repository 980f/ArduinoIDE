/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.app;

import cc.arduino.packages.BoardPort;

import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

@SuppressWarnings("serial")
public class SerialMonitor extends SerialMonitorBase {

  public SerialMonitor(BoardPort port) {
    super(port);

    onSendCommand((ActionEvent event) -> {
      String command = textField.getText();
      if(!unbuffered) {//[980f] don't resend, but do pass into history mechanism.
        send(command);
      }
      commandHistory.addCommand(command);
      textField.setText("");
    });

    onClearCommand((ActionEvent event) -> textArea.setText(""));

    // Add key listener to UP, DOWN, ESC keys for command history traversal.
    textField.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {

          // Select previous command.
          case KeyEvent.VK_UP:
            if (commandHistory.hasPreviousCommand()) {
              textField.setText(
                  commandHistory.getPreviousCommand(textField.getText()));
            }
            break;

          // Select next command.
          case KeyEvent.VK_DOWN:
            if (commandHistory.hasNextCommand()) {
              textField.setText(commandHistory.getNextCommand());
            }
            break;

          // Reset history location, restoring the last unexecuted command.
          case KeyEvent.VK_ESCAPE:
            textField.setText(commandHistory.resetHistoryLocation());
            break;
        }
        if(unbuffered){
          if(serial!=null){
            serial.write(e.getKeyChar());
          }
        }
      }
    });
  }

}
