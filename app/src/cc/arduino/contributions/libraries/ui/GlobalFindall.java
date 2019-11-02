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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.ref.WeakReference;
import java.text.MessageFormat;
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
    public boolean wordly = false;
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
    /**
     * record what it looked like, for gui use.
     */
    public String fragment;
  }

  public ArrayList<Finding> findings = new ArrayList<>();

  public void eraseAll() {
    editor.removeAllLineHighlights();
    findings.clear();
    refreshGui();
  }

  public void gotoFinding(Finding finding) {
    if (finding != null) {
      EditorTab tab = finding.tab.get();
      if (tab != null) {
        editor.selectTab(tab);
        tab.setSelection(finding.start, finding.end);
        if (finding.line >= 0) {//not sure if we can get valid line numbers.
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
        finding.tab = new WeakReference<>(tab);
        finding.start = foundAt;
        finding.end = foundAt + length;
        addFinding(finding);
        try {
          finding.line = area.getLineOfOffset(finding.start);
          //this does some extra stuff:    editor.addLineHighlight(finding.line);
          exposeLine(finding.line, area.getFoldManager());
          area.addLineHighlight(finding.line, new Color(0, 0, 1, 0.2f));//todo: color changes with each invocation
          Segment segment = new Segment();
          area.getTextLine(finding.line, segment);
          finding.fragment = segment.toString().trim();
        } catch (BadLocationException e) {
          finding.line = -1;
        }
      } else {
        break;
      }
    }
  }

  /**
   * todo: move to a generic place
   */
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
    GlobalFindall.FindList list;

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

    private void addComp(Component comp) {
      comp.setVisible(true);
      this.add(comp);
    }

    public FindAllGui(GlobalFindall findall) {
      this.findall = findall;
      findall.linkRefresh(this);

      JLabel findLabel = new JLabel();
      findLabel.setText(tr(findText.pretty));
      addComp(findLabel);

      findField = new JTextField();
      addComp(findField);
      findField.setColumns(20);
      findField.setComponentPopupMenu(makeCCP_Popup());
      addWindowListener(new WindowAdapter() {
        public void windowActivated(WindowEvent e) {
          findField.requestFocusInWindow();
          findField.selectAll();
        }
      });

      ignoreCaseBox = new JCheckBox();
      ignoreCaseBox.setText(tr(ignoreCase.pretty));
      addComp(ignoreCaseBox);

      wordlyBox = new JCheckBox();
      wordlyBox.setText(tr(wordly.pretty));
      addComp(wordlyBox);

      searchAllFilesBox = new JCheckBox();
      searchAllFilesBox.setText(tr(searchAllFiles.pretty));
      addComp(searchAllFilesBox);

      JButton findButton = new JButton();
      findButton.setText(tr(GO.pretty));
      findButton.addActionListener(evt -> {
        findall.state.term = findField.getText();
        findall.state.ignoreCase = ignoreCaseBox.isSelected();
        findall.state.wordly = wordlyBox.isSelected();
        findall.state.allTabs = searchAllFilesBox.isSelected();
        findall.findem();
      });
      addComp(findButton);

      JButton clearButton = new JButton();
      clearButton.setText(tr(clearFinds.pretty));
      clearButton.addActionListener(evt -> findall.eraseAll());
      addComp(clearButton);

      list = new GlobalFindall.FindList();

      JScrollPane scrollPane = new JScrollPane(list);
      addComp(scrollPane);

      // This must be run in the GUI thread
      SwingUtilities.invokeLater(this::refresh);
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

  private static class FindList extends JTextArea {

    private void addFinding(Finding finding) {
      EditorTab tab = finding.tab.get();
      if (tab != null) {
        String name = tab.getSketchFile().getBaseName();
        append(MessageFormat.format("\n[{0}:{1}] {3}", name, finding.line, finding.start, finding.fragment));
      }
    }

    public void refresh(ArrayList<Finding> findings) {
      setText(null);
      setWrapStyleWord(true);
      setLineWrap(true);
      findings.forEach(this::addFinding);
      revalidate();
    }
  }
//I could not get any variant of the following to actually display anything other than background color. I have no clue as to why this particular JPanel doesn't show nested content.
  //  private static class FindList extends JPanel {
  //    static boolean gridly=false;
  //    private GridBagLayout grid;
  //    GridBagConstraints cursor;
  //
  //    FindList() {
  //      setPreferredSize(new Dimension(200,200));
  //    }
  //
  //    private static class FindItem {
  //      Finding finding;//retain for debug
  //      private JCheckBox picked;
  //      private JTextField tabname;
  //      private JTextField linenumber;
  //      private JTextField image;
  //
  //      FindItem(Finding finding) {
  //        this.finding = finding;
  //        EditorTab tab = finding.tab.get();
  //        if (tab != null) {
  //          picked = new JCheckBox();
  //          tabname = new JTextField();
  //          tabname.setText(tab.getSketchFile().getBaseName());
  //          linenumber = new JTextField();
  //          linenumber.setText(String.valueOf(finding.line));
  //          image = new JTextField();
  //          image.setText(finding.fragment);
  //        }
  //      }
  //
  //    }
  //
  //    private void addHeader(){
  //      if(gridly) {
  //        grid.addLayoutComponent(new JLabel("X"), cursor);
  //        ++cursor.gridx;
  //        grid.addLayoutComponent(new JLabel("File"), cursor);
  //        ++cursor.gridx;
  //        grid.addLayoutComponent(new JLabel("line"), cursor);
  //        ++cursor.gridx;
  //        grid.addLayoutComponent(new JLabel("context"), cursor);
  //        ++cursor.gridy;
  //        cursor.gridx = 0;
  //      } else {
  //        add(new JLabel("X"));
  //        add(new JLabel("File"));
  //        add(new JLabel("line"));
  //        add(new JLabel("context"));
  //      }
  //    }
  //
  //    private void addItem(FindItem item) {
  //
  //      if(gridly)  {
  //        grid.addLayoutComponent(item.picked, cursor);
  //        ++cursor.gridx;
  //        grid.addLayoutComponent(item.tabname, cursor);
  //        ++cursor.gridx;
  //        grid.addLayoutComponent(item.linenumber, cursor);
  //        ++cursor.gridx;
  //        grid.addLayoutComponent(item.image, cursor);
  //        ++cursor.gridy;
  //        cursor.gridx = 0;
  //      } else {
  //        add(item.picked);
  //        add(item.tabname);
  //        add(item.linenumber);
  //        add(item.image);
  //      }
  //    }
  //
  //    public void refresh(ArrayList<Finding> findings) {
  //
  //      if(gridly) {
  //        grid = new GridBagLayout();//because we can't delete all components
  //        setLayout(grid);
  //        cursor = new GridBagConstraints();
  //        addHeader();
  //        findings.forEach(finding -> addItem(new FindItem(finding)));
  //        addHeader();
  //      } else {
  //        removeAll();
  //        setLayout(new GridLayout(4,findings.size()));
  //        findings.forEach(finding -> addItem(new FindItem(finding)));
  //      }
  //    }
  //  }
}
