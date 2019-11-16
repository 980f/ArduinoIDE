/** Copyright 2019 Andrew L. Heilveil (github/980f)
 * (M.I.T. License)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * */
package cc.arduino.contributions.libraries.ui;

import org.fife.ui.rsyntaxtextarea.folding.FoldManager;
import processing.app.Editor;
import processing.app.EditorTab;
import processing.app.syntax.SketchTextArea;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Segment;
import java.awt.*;
import java.awt.event.*;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static cc.arduino.contributions.libraries.ui.FindList.*;
import static cc.arduino.contributions.libraries.ui.GlobalFindall.FindAllGui.OptionsName.*;
import static com.sun.java.accessibility.util.AWTEventMonitor.addWindowListener;
import static processing.app.I18n.tr;

/**
 * search making a list of clickable links to all instances. The existing FindReplace likes to hide under the ui and has other unpleasant implementation details
 */
public class GlobalFindall {
  private final Editor editor;

  static class State {
    public boolean allTabs = false;
    public boolean ignoreCase = false;
    public boolean wordly = false;
    public String term = "if";
  }



  public ArrayList<Finding> findings = new ArrayList<>();

  public void eraseAll() {
    editor.removeAllLineHighlights();
    findings.clear();
    refreshGui();
  }



  /**
   * todo: is search per editor or per human?
   */
  public State state = new State();

  public GlobalFindall(Editor editor) {
    this.editor = editor;
  }

  ///////////////////////////////////////
  public interface FindWatcher {
    void refresh();
  }

  private WeakReference<FindWatcher> gui = null;

  public void refreshGui() {
    if (gui != null) {
      FindWatcher thegui = gui.get();
      if (thegui != null) {
        thegui.refresh();
      }
    }
  }

  public void linkRefresh(FindWatcher agui) {
    gui = new WeakReference<>(agui);
  }
  ////////////////////////////////////////////

  /**
   * @todo: replace this with a functional that user can provide
   */
  private static boolean inWord(char see) {
    return !Character.isWhitespace(see) && Character.isLetterOrDigit(see);
  }

  private void findInTab(EditorTab tab) {
    SketchTextArea area = tab.getTextArea();
    String text = tab.getText();
    String term = state.term;

    if (state.ignoreCase) {
      term = term.toLowerCase();
      text = text.toLowerCase();
    }
    final int length = term.length();
    if (length < 1) {
      return;
    }
    int foundAt = -1;
    while (true) {
      foundAt = text.indexOf(term, foundAt + 1);
      int end = foundAt + length;
      if (foundAt >= 0) {
        if (state.wordly) {
          if (foundAt != 0 && inWord(text.charAt(foundAt - 1))) {
            continue;
          }

          if ((end < text.length() && inWord(text.charAt(end)))) {
            continue;
          }
        }
        Finding finding = new Finding();
        finding.start = foundAt;
        finding.end = foundAt + length;
        finding.setTab(tab);
        addFinding(finding);
      } else {
        break;
      }
    }
  }
//retained for a few cycles, in case we need it elsewhere.
//  /**
//   * todo: move to a generic place
//   */
//  public static void exposeLine(int line, FoldManager foldManager) {
//    if (foldManager.isLineHidden(line)) {
//      //must open all containing folds, opening the direct containing one apparently doesn't open outer ones.
//      for (int i = 0; i < foldManager.getFoldCount(); i++) {
//        if (foldManager.getFold(i).containsLine(line)) {
//          foldManager.getFold(i).setCollapsed(false);
//        }
//      }
//    }
//  }

  private void addFinding(Finding finding) {
    findings.add(finding);
  }

  /**
   * public for 'rerun find' function
   */
  public void findem() {
    if (state.allTabs) {
      List<EditorTab> tabs = editor.getTabs();
      tabs.forEach(this::findInTab);
    } else {
      findInTab(editor.getCurrentTab());
    }
    refreshGui();
  }

  /**
   * hiding here for pull convenience
   */
  public static class FindAllGui extends JPanel implements FindWatcher {
    public GlobalFindall findall;

    //would be parts of a closure in other languages
    private JTextField findField;
    private JCheckBox searchAllFilesBox;
    private JCheckBox ignoreCaseBox;
    private JCheckBox wordlyBox;
    FindList list;

    /**
     * called when a find has been refreshed
     */
    @Override
    public void refresh() {
      list.refresh(findall.findings);
    }

    /**
     * matching original FindReplace internals, for familiarity
     */
    enum OptionsName {
      findText("Find:"), searchAllFiles("Search all Sketch Tabs"), ignoreCase(
        "Ignore Case"), wordly("Words"), replaceText("Replace:"),//NYI
      wrapAround("Wrap Around"), //not meaningful
      GO("GO!"), clearFinds("CLEAR");

      final String pretty;

      OptionsName(String pretty) {
        this.pretty = pretty;
      }
    }


    static class EzPanel extends JPanel {
      public EzPanel(){
        setVisible(true);
      }

      public EzPanel addComp(Component comp) {
        comp.setVisible(true);
        this.add(comp);
        return this;
      }

      public EzPanel addLabel(String keytext) {
        JLabel findLabel = new JLabel();
        findLabel.setText(tr(keytext));
        addComp(findLabel);
        return this;
      }

      public JTextField addTextEntry(String label,int width){
        addLabel(label);

        final JTextField field = new JTextField();
        addComp(field);
        field.setColumns(width);
        return field;
      }

      public JCheckBox addCheckBox(String label){
        final JCheckBox box = new JCheckBox();
        box.setText(tr(label));
        addComp(box);
        return box;
      }

      public JButton addButton(String legend, final ActionListener action){
        JButton button = new JButton();
        button.setText(tr(legend));
        button.addActionListener(action);
        addComp(button);
        return button;
      }
    }


    public FindAllGui(GlobalFindall findall) {
      this.findall = findall;
      findall.linkRefresh(this);

      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

      final EzPanel dialog = new EzPanel();
      findField=dialog.addTextEntry(findText.pretty,20);
      findField.setComponentPopupMenu(makeCCP_Popup());
      addWindowListener(new WindowAdapter() {
        public void windowActivated(WindowEvent e) {
          findField.requestFocusInWindow();
          findField.selectAll();
        }
      });

      ignoreCaseBox = dialog.addCheckBox(ignoreCase.pretty);
      wordlyBox = dialog.addCheckBox(wordly.pretty);
      searchAllFilesBox = dialog.addCheckBox(searchAllFiles.pretty);
      dialog.addButton(GO.pretty,evt -> {
        findall.state.term = findField.getText();
        findall.state.ignoreCase = ignoreCaseBox.isSelected();
        findall.state.wordly = wordlyBox.isSelected();
        findall.state.allTabs = searchAllFilesBox.isSelected();
        findall.findem();
      });
      dialog.addButton(clearFinds.pretty,evt -> findall.eraseAll());
      this.add(dialog);

      list = new FindList(findall.editor);

      JScrollPane scrollPane = new JScrollPane(list);
      add(scrollPane);
      scrollPane.setPreferredSize(new Dimension(Short.MAX_VALUE,Short.MAX_VALUE));

      SwingUtilities.invokeLater(this::refresh);//init contents on construction.
    }

    private JPopupMenu makeCCP_Popup() {
      JPopupMenu menu = new JPopupMenu();
      Action cut = new DefaultEditorKit.CutAction();
      cut.putValue(Action.NAME, tr("Cut"));
      menu.add(cut);

      Action copy = new DefaultEditorKit.CopyAction();
      copy.putValue(Action.NAME, tr("Copy"));
      menu.add(copy);

      Action paste = new DefaultEditorKit.PasteAction();
      paste.putValue(Action.NAME, tr("Paste"));
      menu.add(paste);
      return menu;
    }
  }
}
