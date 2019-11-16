/**  Copyright 2019 Andrew L. Heilveil (github/980f)
 * (M.I.T. License)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * ********************************
 * This was originally coded for use by the GlobalFindall mechanism. It was made as separate module so that it could be used for error listings and maybe bookmarks.
 * */

package cc.arduino.contributions.libraries.ui;

import org.fife.ui.rsyntaxtextarea.folding.FoldManager;
import processing.app.Editor;
import processing.app.EditorTab;
import processing.app.syntax.SketchTextArea;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Segment;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.ArrayList;
public class FindList extends JTextArea {
  public static class Finding {
    public WeakReference<EditorTab> tab;

    public void setTab(EditorTab tab){
      this.tab = new WeakReference<>(tab);
      try {
        SketchTextArea area = tab.getTextArea();
        if(line>=0 && start<0){
          //then start is offset from start of line, not start of area
          start=area.getLineStartOffset(line)+~start;
        }
        if(start>=0) {
          line = area.getLineOfOffset(start);
          final FoldManager foldManager = area.getFoldManager();
          foldManager.ensureOffsetNotInClosedFold(start);
          foldManager.ensureOffsetNotInClosedFold(end);
          area.addLineHighlight(line, new Color(1, 0, 0, 0.2f));//todo: color changes with each invocation
          Segment segment = new Segment();
          area.getTextLine(line, segment);
          fragment = segment.toString().trim();
        }
      } catch (Exception ignored) {
        line = -1;
      }
    }

    /**
     * the line of the text is used for whole line highlighting, but is ambiguous if a find splits lines.
     */
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

    @Override
    public int hashCode() {
      return tab!=null?tab.hashCode()+line*57:0;
    }

    @Override
    public boolean equals(final Object obj) {
      if(this==obj){
        return true;
      }
      if(obj instanceof Finding){
        final Finding eff = (Finding) obj;
        return eff.tab.get()==this.tab.get() && eff.start==this.start;//ignoring the rest for now, may switch to 'line' if we only ever want one report per line.
      }
      return false;
    }
  }

  private final Editor editor;

  public void gotoFinding(Finding finding) {
    if (finding != null) {
      EditorTab tab = finding.tab.get();
      if (tab != null) {
        editor.selectTab(tab);
        tab.setSelection(finding.start, finding.end);
        if (finding.line >= 0) {//not sure if we can get valid line numbers.
          tab.goToLine(finding.line + 1);//the +1 was empirically determined, not sure which mechanism decided on 1 based vs 0 based counting here.
        }
        SketchTextArea textarea = tab.getTextArea();
        textarea.getFoldManager().ensureOffsetNotInClosedFold(finding.start);
        textarea.getCaret().setSelectionVisible(true);
      }
    }
  }

  private void addFinding(Finding finding) {
    EditorTab tab = finding.tab.get();
    if (tab != null) {
      String name = tab.getSketchFile().getBaseName();
      append(MessageFormat.format("\n[{0}:{1}] {3}", name, finding.line + 1, finding.start, finding.fragment));//+1 empirically determined.
    }
  }

  ArrayList<Finding> findings = null;

  public void refresh(ArrayList<Finding> findings) {
    this.findings = findings;
    setText(null);
    setWrapStyleWord(true);
    setLineWrap(true);
    findings.forEach(this::addFinding);
    revalidate();
  }

  public void navigate(MouseEvent e) {
    if (findings == null) {
      return;
    }
    final int position = viewToModel(e.getPoint());
    setCaretPosition(position);
    try {
      int line = getLineOfOffset(position);
      --line;//until we find out where the leading blank line in the listing window comes from
      gotoFinding(findings.get(line));
    } catch (Exception ex) {
      System.err.println("ignoring exception on mouse click on list of lines");
      ex.printStackTrace();
    }
    System.out.println(getCaretPosition());
  }

  public FindList(Editor editor) {
    this.editor = editor;
    this.addMouseListener(new MouseListener() {
      @Override
      public void mouseClicked(final MouseEvent e) {
        navigate(e);
      }

      @Override
      public void mousePressed(final MouseEvent e) {

      }

      @Override
      public void mouseReleased(final MouseEvent e) {

      }

      @Override
      public void mouseEntered(final MouseEvent e) {

      }

      @Override
      public void mouseExited(final MouseEvent e) {

      }
    });
  }
}
