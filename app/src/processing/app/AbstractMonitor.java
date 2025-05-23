package processing.app;

import cc.arduino.packages.BoardPort;
import processing.app.legacy.PApplet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

@SuppressWarnings("serial")
public abstract class AbstractMonitor extends JFrame implements ActionListener {

  JFrame popout;
  private boolean closed;
  protected boolean paused;

  private StringBuffer updateBuffer;
  private Timer updateTimer;
  private Timer portExistsTimer;

  private BoardPort boardPort;

  protected String[] serialRateStrings = {"300", "1200", "2400", "4800", "9600", "19200", "38400", "57600", "74880", "115200", "230400", "250000", "460800","500000","921600", "1000000", "2000000"};

  public AbstractMonitor(BoardPort boardPort) {
    super();
    popout=new JFrame(boardPort.getLabel());

    this.boardPort = boardPort;

    popout.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent event) {
        try {
          closed = true;
          close();
        } catch (Exception e) {
          // ignore
        }
      }
    });

    // obvious, no?
    KeyStroke wc = Editor.WINDOW_CLOSE_KEYSTROKE;
    final JRootPane rootPane = popout.getRootPane();
    rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(wc, "close");
    rootPane.getActionMap().put("close", (new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent event) {
        try {
          close();
        } catch (Exception e) {
          // ignore
        }
        setVisible(false);
      }
    }));


    final Container contentPane = popout.getContentPane();
    onCreateWindow(contentPane);

    this.setMinimumSize(new Dimension(contentPane.getMinimumSize().width, this.getPreferredSize().height));

    popout.pack();

    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    String locationStr = PreferencesData.get("last.serial.location");
    if (locationStr != null) {
      int[] location = PApplet.parseInt(PApplet.split(locationStr, ','));
      if (location[0] + location[2] <= screen.width && location[1] + location[3] <= screen.height) {
        setPlacement(location);
      }
    }

    updateBuffer = new StringBuffer(1048576);
    updateTimer = new Timer(33, this);  // redraw serial monitor at 30 Hz
    updateTimer.start();

    ActionListener portExists = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent ae) {
        try {
          if (Base.getDiscoveryManager().find(boardPort.getAddress()) == null) {
            if (!closed) {
              suspend();
            }
          } else {
            if (closed && !Editor.isUploading() && !paused) {
              resume(boardPort);
            }
          }
        } catch (Exception e) {}
      }
    };

    portExistsTimer = new Timer(1000, portExists);  // check if the port is still there every second
    portExistsTimer.start();

    closed = false;
  }

  protected abstract void onCreateWindow(Container mainPane);

  public void enableWindow(boolean enable) {
    onEnableWindow(enable);
  }

  protected abstract void onEnableWindow(boolean enable);

  // Puts the window in suspend state, closing the serial port
  // to allow other entity (the programmer) to use it
  public void suspend() throws Exception {
    enableWindow(false);

    close();
  }

  public void dispose() {
    popout.dispose();
    portExistsTimer.stop();
  }

  public void resume(BoardPort boardPort) throws Exception {
    setBoardPort(boardPort);

    // Enable the window
    enableWindow(true);

    // If the window is visible, try to open the serial port
    if (!isVisible()) {
      return;
    }

    open();
  }

  protected void setPlacement(int[] location) {
    setBounds(location[0], location[1], location[2], location[3]);
  }

  protected int[] getPlacement() {
    int[] location = new int[4];

    // Get the dimensions of the Frame
    Rectangle bounds = getBounds();
    location[0] = bounds.x;
    location[1] = bounds.y;
    location[2] = bounds.width;
    location[3] = bounds.height;

    return location;
  }

  public abstract void message(final String s);

  public boolean requiresAuthorization() {
    return false;
  }

  public String getAuthorizationKey() {
    return null;
  }

  public boolean isClosed() {
    return closed;
  }

  public void open() throws Exception {
    closed = false;
  }

  public void close() throws Exception {
    closed = true;
  }

  public BoardPort getBoardPort() {
    return boardPort;
  }

  public void setBoardPort(BoardPort boardPort) {
    if (boardPort == null) {
      return;
    }
    popout.setTitle(boardPort.getLabel());
    this.boardPort = boardPort;
  }

  public synchronized void addToUpdateBuffer(char buff[], int n) {
    updateBuffer.append(buff, 0, n);
  }

  private synchronized String consumeUpdateBuffer() {
    String s = updateBuffer.toString();
    updateBuffer.setLength(0);
    return s;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    String s = consumeUpdateBuffer();
    if (s.isEmpty()) {
      return;
    } else {
      message(s);
    }
  }

  /**
   * Read and apply new values from the preferences, either because
   * the app is just starting up, or the user just finished messing
   * with things in the Preferences window.
   */
  public void applyPreferences() {
    // Empty.
  };
}
