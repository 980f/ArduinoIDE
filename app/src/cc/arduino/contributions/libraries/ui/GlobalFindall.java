package cc.arduino.contributions.libraries.ui;

import org.fife.ui.rsyntaxtextarea.folding.Fold;
import org.fife.ui.rsyntaxtextarea.folding.FoldManager;
import processing.app.Editor;
import processing.app.EditorTab;
import processing.app.syntax.SketchTextArea;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

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
    public String term = "if";

  }

  static class Finding {
    public WeakReference<EditorTab> tab;

    public int line;
    /**
     * where text was when we found it, we will not track changes due to edits, we would have to get edit signals from the tab.
     */
    public int start;
    /**
     * record the end in case we change the search term without clearing previous finds
     */
    public int end;
    /** record what it looked like, for gui use. */
    public String fragment;
  }

  public ArrayList<Finding> findings = new ArrayList<>();

  void gotoFinding(Finding finding) {
    if (finding != null) {
      EditorTab tab = finding.tab.get();
      if (tab != null) {
        editor.selectTab(tab);
        tab.setSelection(finding.start, finding.end);
        if(finding.line>=0) {//not sure if we can get valid line numbers.
          tab.goToLine(finding.line);
        }
        SketchTextArea textarea = tab.getTextArea();
        textarea.getFoldManager().ensureOffsetNotInClosedFold(finding.start);
        textarea.getCaret().setSelectionVisible(true);
      }
    }
  }

  /**
   * todo: is search per editor or per human?
   */
  public State state = new State();

  public GlobalFindall(Editor editor) {
    this.editor = editor;
  }

  public interface FindWatcher {
    void refresh();
  }

  public FindWatcher gui=null;

  private void findInTab(EditorTab tab) {
    SketchTextArea area = tab.getTextArea();
    String text = tab.getText();
    String term = state.term;
    if (state.ignoreCase) {
      term = term.toLowerCase();
      text = text.toLowerCase();
    }

    int foundAt = -1;
    while (true) {
      foundAt = text.indexOf(term, foundAt + 1);
      if (foundAt >= 0) {
        Finding finding = new Finding();
        finding.tab = new WeakReference<>(tab);
        finding.start = foundAt;
        finding.end = foundAt + term.length();
        addFinding(finding);
        try {
          finding.line= area.getLineOfOffset(finding.start);
//this does some extra stuff:    editor.addLineHighlight(finding.line);
          exposeLine(finding.line, area.getFoldManager());
          area.addLineHighlight(finding.line, new Color(0, 0, 1, 0.2f));
        } catch (BadLocationException e) {
          finding.line=-1;
        }
      } else {
        break;
      }
    }
  }

  /** todo: move to a generic place */
  public static void exposeLine(int line, FoldManager foldManager) {
    if (foldManager.isLineHidden(line)) {
      //must open all containing folds, opening the direct containing one apparently doesn't open outer ones.
      for (int i = 0; i < foldManager.getFoldCount(); i++) {
        if (foldManager.getFold(i).containsLine(line)) {
          foldManager.getFold(i).setCollapsed(false);
        }
      }
    }
  }

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
  }

  /** hiding here for pull convenience */
  public static class FindAllGui extends JPanel implements FindWatcher {
    public GlobalFindall findall;

    private JTextField findField;
    private JCheckBox searchAllFilesBox;
    private JCheckBox ignoreCaseBox;

    private static class FindList extends JPanel {
      private GridBagLayout grid;
      GridBagConstraints cursor;

      FindList() {

      }

      private static class FindItem {
        Finding finding;//retain for debug
        private JCheckBox picked;
        private JTextField tabname;
        private JTextField linenumber;
        private JTextField image;

        FindItem(Finding finding){
          this.finding=finding;
          EditorTab tab = finding.tab.get();
          if(tab!=null) {
            picked = new JCheckBox();
            tabname = new JTextField();
            tabname.setText(tab.getSketchFile().getBaseName());
            linenumber = new JTextField();
            linenumber.setText(String.valueOf(finding.line));
            image = new JTextField();
            image.setText(finding.fragment);
          }
        }

      }

      private void addItem(FindItem item){
        grid.addLayoutComponent(item.picked,cursor);
        ++cursor.gridx;
        grid.addLayoutComponent(item.tabname,cursor);
        ++cursor.gridx;
        grid.addLayoutComponent(item.linenumber,cursor);
        ++cursor.gridx;
        grid.addLayoutComponent(item.image,cursor);
        ++cursor.gridy;
        cursor.gridx=0;
      }

      public void refresh(ArrayList<Finding> findings){
        grid = new GridBagLayout();//because we can't delete all components
        setLayout(grid);
        cursor = new GridBagConstraints();
        findings.forEach(finding -> addItem(new FindItem(finding)));
      }
    }

    FindList list=new FindList();

    /** called when a find has been refreshed */
    @Override public void refresh() {
      list.refresh(findall.findings);
    }

    /** matching original FindReplace internals, for familiarity */
    enum OptionsName{
      findText("Find:"), searchAllFiles("Search all Sketch Tabs"), ignoreCase("Ignore Case"), replaceText("Replace:"), wrapAround("Wrap Around"), GO("GO!");

      final String display;
      OptionsName(String display){
        this.display = display;
      }
    }

    public FindAllGui(GlobalFindall findall) {
      this.findall = findall;
      findall.gui=this;//to get refresh signal when find invoked from means other than this gui.

      JLabel findLabel = new JLabel();
      findLabel.setText(tr(findText.toString()));
      this.add(findLabel);

      findField = new JTextField();
      this.add(findField);
      findField.setColumns(20);
      findField.setComponentPopupMenu(makeCCP_Popup());
      addWindowListener(new WindowAdapter() {
        public void windowActivated(WindowEvent e) {
          findField.requestFocusInWindow();
          findField.selectAll();
        }
      });

      ignoreCaseBox = new JCheckBox();
      ignoreCaseBox.setText(tr(ignoreCase.toString()));
      this.add(ignoreCaseBox);

      searchAllFilesBox = new JCheckBox();
      searchAllFilesBox.setText(tr(searchAllFiles.toString()));
      this.add(searchAllFilesBox);
      JButton findButton = new JButton();

      findButton.setText(tr(GO.toString()));
      findButton.addActionListener(evt -> {
        findall.state.term = findField.getText();
        findall.state.ignoreCase=ignoreCaseBox.isSelected();
        findall.state.allTabs=searchAllFilesBox.isSelected();
        findall.findem();});
      this.add(findButton);

      this.add(list);
      refresh();
    }

    private JPopupMenu makeCCP_Popup() {
      JPopupMenu menu = new JPopupMenu();
      Action cut = new DefaultEditorKit.CutAction();
      cut.putValue(Action.NAME, tr("Cut"));
      menu.add( cut );

      Action copy = new DefaultEditorKit.CopyAction();
      copy.putValue(Action.NAME, tr("Copy"));
      menu.add( copy );

      Action paste = new DefaultEditorKit.PasteAction();
      paste.putValue(Action.NAME, tr("Paste"));
      menu.add( paste );
      return menu;
    }
  }

}
