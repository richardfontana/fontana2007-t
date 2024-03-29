/*
 * Copyright (C) 2009 Lalit Pant <pant.lalit@gmail.com>
 *
 * The contents of this file are subject to the GNU General Public License
 * Version 3 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.gnu.org/copyleft/gpl.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 */
package net.kogics.kojo

import javax.swing._
import java.awt.{List => AwtList, _}
import java.awt.event._
import javax.swing.event._

import java.util.concurrent.CountDownLatch
import java.util.logging._

import edu.umd.cs.piccolo._
import edu.umd.cs.piccolo.nodes._

import util._
import net.kogics.kojo.core.RunContext

import org.openide.windows._
import org.openide.awt.UndoRedo

object CodeExecutionSupport extends InitedSingleton[CodeExecutionSupport] {
  def initedInstance(codePane: JEditorPane, manager: UndoRedo.Manager) = synchronized {
    instanceInit()
    val ret = instance()
    ret.setCodePane(codePane)
    ret.undoRedoManager = manager
    ret
  }

  protected def newInstance = new CodeExecutionSupport()
}

class CodeExecutionSupport private extends core.CodeCompletionSupport {
  val Log = Logger.getLogger(getClass.getName);

  val tCanvas = SpriteCanvas.instance
  tCanvas.outputFn = showOutput _

  val geomCanvas = geogebra.GeoGebraCanvas.instance.geomCanvas
  val commandHistory = CommandHistory.instance
  val historyManager = new HistoryManager()
  @volatile var pendingCommands = false
  val findAction = new org.netbeans.editor.ext.ExtKit.FindAction()
  val replaceAction = new org.netbeans.editor.ext.ExtKit.ReplaceAction()

  val (toolbar, runButton, stopButton, hNextButton, hPrevButton, clearSButton, clearButton, undoButton, cexButton) = makeToolbar()

  @volatile var runMonitor: RunMonitor = new NoOpRunMonitor()
  var undoRedoManager: UndoRedo.Manager = _ 
  var codePane: JEditorPane = _

  val codeRunner = makeCodeRunner()
  
  val IO = makeOutput2()

  val statusStrip = new StatusStrip()
  
  val promptMarkColor = new Color(0x2fa600)
  val promptColor = new Color(0x883300)
  val codeColor = new Color(0x009b00)
  val outputColor = new Color(32, 32, 32)

  @volatile var showCode = false
  @volatile var verboseOutput = false
  val OutputDelimiter = "---\n"
  @volatile var lastOutput = ""


  setSpriteListener()
  doWelcome()

  def setCodePane(cp: JEditorPane) {
    codePane = cp;
    addCodePaneShortcuts()
    statusStrip.linkToPane()
    codePane.getDocument.addDocumentListener(new DocumentListener {
        def insertUpdate(e: DocumentEvent) {
          // runButton.setEnabled(true)
          clearSButton.setEnabled(true)
          // cexButton.setEnabled(true)

        }
        def removeUpdate(e: DocumentEvent) {
          if (codePane.getDocument.getLength == 0) {
            // runButton.setEnabled(false) // interferes with enabling/disabling of run button with interpreter start/stop
            clearSButton.setEnabled(false)
            // cexButton.setEnabled(false) // makes the icon look horrible
          }
        }
        def changedUpdate(e: DocumentEvent) {}
      })
  }

  def doWelcome() = {
    val msg = """Welcome to Kojo! 
    |* To access context-sensitive actions  ->  Right-click on (most) windows
    |* To Pan/Zoom within the Turtle window ->  Press the left/right mouse button and drag
    |  * To reset Pan and Zoom levels       ->  Resize the Turtle window
    |* To see a list of available commands  ->  Type help and press Ctrl+Enter in the script window
    |""".stripMargin
    
    showOutput(msg)
  }

  def makeOutput2(): org.openide.windows.InputOutput = {
    val ioc = IOContainer.create(OutputTopComponent.findInstance)
    val ret = IOProvider.getDefault().getIO("Script Output", Array[Action](), ioc)
    ret.setFocusTaken(false)
    ret.setInputVisible(false)
    ret
  }

  def makeToolbar() = {
    val RunScript = "RunScript"
    val StopScript = "StopScript"
    val HistoryNext = "HistoryNext"
    val HistoryPrev = "HistoryPrev"
    val ClearEditor = "ClearEditor"
    val ClearOutput = "ClearOutput"
    val UndoCommand = "UndoCommand"
    val UploadCommand = "UploadCommand"

    val actionListener = new ActionListener {
      def actionPerformed(e: ActionEvent) = e.getActionCommand match {
        case RunScript =>
          runCode()
        case StopScript =>
          codeRunner.interruptInterpreter()
          tCanvas.stop()
        case HistoryNext =>
          loadCodeFromHistoryNext()
        case HistoryPrev =>
          loadCodeFromHistoryPrev()
        case ClearEditor =>
          clrEditor()
        case ClearOutput =>
          clrOutput()
        case UndoCommand =>
          smartUndo()
        case UploadCommand =>
          upload()
      }
    }

    def makeNavigationButton(imageFile: String, actionCommand: String,
                             toolTipText: String, altText: String): JButton = {
      val button = new JButton()
      button.setActionCommand(actionCommand)
      button.setToolTipText(toolTipText)
      button.addActionListener(actionListener)
      button.setIcon(Utils.loadIcon(imageFile, altText))
      // button.setMnemonic(KeyEvent.VK_ENTER)
      button;
    }


    val toolbar = new JToolBar
    toolbar.setPreferredSize(new Dimension(100, 24))

    val runButton = makeNavigationButton("/images/run24.png", RunScript, "Run Script (Ctrl + Enter)", "Run the Code")
    val stopButton = makeNavigationButton("/images/stop24.png", StopScript, "Stop Script/Animation", "Stop the Code")
    val hNextButton = makeNavigationButton("/images/history-next.png", HistoryNext, "Go to Next Script in History (Ctrl + Down Arrow)", "Next in History")
    val hPrevButton = makeNavigationButton("/images/history-prev.png", HistoryPrev, "Goto Previous Script in History (Ctrl + Up Arrow)", "Prev in History")
    val clearSButton = makeNavigationButton("/images/clears.png", ClearEditor, "Clear Editor", "Clear the Editor")
    val clearButton = makeNavigationButton("/images/clear24.png", ClearOutput, "Clear Output", "Clear the Output")
    val undoButton = makeNavigationButton("/images/undo.png", UndoCommand, "Undo Last Turtle Command", "Undo")
    val cexButton = makeNavigationButton("/images/upload.png", UploadCommand, "Upload to CodeExchange", "Upload")

    toolbar.add(runButton)

    stopButton.setEnabled(false)
    toolbar.add(stopButton)

    hPrevButton.setEnabled(false)
    toolbar.add(hPrevButton)

    hNextButton.setEnabled(false)
    toolbar.add(hNextButton)

    clearSButton.setEnabled(false)
    toolbar.add(clearSButton)

    clearButton.setEnabled(false)
    toolbar.add(clearButton)

    undoButton.setEnabled(false)
    toolbar.add(undoButton)

    toolbar.add(cexButton)

    (toolbar, runButton, stopButton, hNextButton, hPrevButton, clearSButton, clearButton, undoButton, cexButton)
  }

  def makeCodeRunner(): core.CodeRunner = {
    new core.ProxyCodeRunner(makeRealCodeRunner _)
  }

  def isSingleLine(code: String): Boolean = {
//    val n = code.count {c => c == '\n'}
//    if (n > 1) false else true

    val len = code.length
    var idx = 0
    var count = 0
    while(idx < len) {
      if (code.charAt(idx) == '\n') {
        count += 1
        if (count > 1) {
          return false
        }
      }
      idx += 1
    }
    if (count == 0) {
      return true
    }
    else {
      if (code.charAt(len-1) == '\n') {
        return true
      }
      else {
        return false
      }
    }
  }

  def makeRealCodeRunner: core.CodeRunner = {
    val codeRunner = new xscala.ScalaCodeRunner(new RunContext {

        @volatile var suppressInterpOutput = false

        def onInterpreterInit() = {
          showOutput(" " * 38 + "_____\n\n")
          lastOutput = ""
        }

        def onInterpreterStart(code: String) {
          if (verboseOutput || isSingleLine(code)) {
            suppressInterpOutput = false
          }
          else {
            suppressInterpOutput = true
          }

          showNormalCursor()
          runButton.setEnabled(false)
          stopButton.setEnabled(true)
          runMonitor.onRunStart()
        }

        def onRunError() {
          historyManager.codeRunError()
          interpreterDone()
          Utils.runInSwingThread {
            statusStrip.onError()
          }
        }

        def onRunSuccess() = {
          interpreterDone()
          Utils.runInSwingThread {
            statusStrip.onSuccess()
            undoRedoManager.discardAllEdits()
          }
        }

        def onRunInterpError() = interpreterDone()

        def println(outText: String) {
          showOutput(outText)
          runMonitor.reportOutput(outText)
        }

        def reportOutput(outText: String) {
          if (suppressInterpOutput) {
            return
          }

          println(outText)
        }

        def reportErrorMsg(errMsg: String) {
          showErrorMsg(errMsg)
          runMonitor.reportOutput(errMsg)
        }

        def reportErrorText(errText: String) {
          showErrorText(errText)
          runMonitor.reportOutput(errText)
        }

        private def interpreterDone() {
          runButton.setEnabled(true)
          if (!pendingCommands) {
            stopButton.setEnabled(false)
          }

          Utils.schedule(0.2) {
            OutputTopComponent.findInstance().scrollToEnd()
          }

          runMonitor.onRunEnd()
        }

        def showScriptInOutput() {showCode = true}
        def hideScriptInOutput() {showCode = false}
        def showVerboseOutput() {verboseOutput = true}
        def hideVerboseOutput() {verboseOutput = false}
        def readInput(prompt: String): String = CodeExecutionSupport.this.readInput(prompt)

        def clearOutput() = clrOutput()

        val inspecteePattern = java.util.regex.Pattern.compile("""inspect\s*\(\s*(.*)\s*\)""")

        def inspect(obj: AnyRef) {
          val inspectCmd = commandHistory.history.last
          val m = inspecteePattern.matcher(inspectCmd)

          val inspectee = if (m.find) {
            m.group(1)
          }
          else {
            obj.getClass().getName()
          }

          Utils.runInSwingThread {
            val itc = new net.kogics.kojo.inspect.InspectorTopComponent()
            itc.inspectObject(inspectee, obj)
            itc.open()
            itc.requestActive()
          }
        }

      }, tCanvas, geomCanvas)
    codeRunner
  }

  def isRunningEnabled = runButton.isEnabled

  def addCodePaneShortcuts() {
    codePane.addKeyListener(new KeyAdapter {
        override def keyPressed(evt: KeyEvent) {
          evt.getKeyCode match {
            case KeyEvent.VK_ENTER =>
              if(evt.isControlDown && isRunningEnabled) {
                runCode()
                evt.consume
              }
            case KeyEvent.VK_UP =>
              if(evt.isControlDown) {
                loadCodeFromHistoryPrev
                evt.consume
              }
            case KeyEvent.VK_DOWN =>
              if(evt.isControlDown) {
                loadCodeFromHistoryNext
                evt.consume
              }
            case _ => // do nothing special
          }
        }

      })
  }

  def setSpriteListener() {
    tCanvas.setTurtleListener(new turtle.AbstractTurtleListener {
        def interpreterDone = runButton.isEnabled
        override def hasPendingCommands {
          pendingCommands = true
          stopButton.setEnabled(true)
        }
        override def pendingCommandsDone {
          pendingCommands = false
          if (interpreterDone) stopButton.setEnabled(false)
          if (tCanvas.hasUndoHistory) undoButton.setEnabled(true) else undoButton.setEnabled(false)
        }
      })
  }

  def loadCodeFromHistoryPrev() = historyManager.historyMoveBack
  def loadCodeFromHistoryNext() = historyManager.historyMoveForward
  def loadCodeFromHistory(historyIdx: Int) = historyManager.setCode(historyIdx)

  def smartUndo() {
    if (codePane.getText.trim() == "") {
      // if code pane is blank, do undo via the interp, so that we go back in
      // history to the last command/script (which we are trying to undo)
      codePane.setText("undo")
      runCode()
    }
    else {
      // if code pane is not blank, selected text was run or the user has loaded
      // something from history (or an error occurred - not relevant here)
      // call coderunner directly so that the buffer is retained
      codeRunner.runCode("undo")
    }
  }

  def upload() {
    val dlg = new codex.CodeExchangeForm(null, true)
    dlg.setCanvas(tCanvas)
    dlg.setCode(Utils.stripCR(codePane.getText()))
    dlg.centerScreen()
  }

  def showFindDialog() {
    findAction.actionPerformed(null, codePane)
    tweakFindReplaceDialog()
  }

  def showReplaceDialog() {
    replaceAction.actionPerformed(null, codePane)
    tweakFindReplaceDialog()
  }

  def tweakFindReplaceDialog() {
    // hacks to control appearance and behavior of Find/Replace Dialog
    import org.netbeans.editor.ext.FindDialogSupport
    try {
      // work around 'disabled find button' bug in Find Dialog
      if (codePane.getSelectedText != null) {
        val findBtnsField = classOf[FindDialogSupport].getDeclaredField("findButtons")
        findBtnsField.setAccessible(true)
        val findBtn = findBtnsField.get(null).asInstanceOf[Array[JButton]](0)
        findBtn.setEnabled(true)
      }

      // hide help button
      val findDialogField = classOf[FindDialogSupport].getDeclaredField("findDialog")
      findDialogField.setAccessible(true)
      val findDlg = findDialogField.get(null).asInstanceOf[Dialog]
      val rootPane = findDlg.getComponent(0).asInstanceOf[JRootPane]
      val pane = rootPane.getComponent(1).asInstanceOf[JLayeredPane]
      val panel = pane.getComponent(0).asInstanceOf[JPanel]
      val panel2 = panel.getComponent(1).asInstanceOf[JPanel]
      panel2.getComponents.foreach {c => c match {
          case button: JButton => if (button.getText == "Help") button.setVisible(false)
          case _ => // pass
        }
      }
    }
    catch {
      case t: Throwable => // pass
    }
  }

  def locateError(errorText0: String) {

    def showHelpMessage() {
      val msg = """
      |The error text is not present in your current script.
      |
      |This can happen - if you made a change to your script after seeing an
      |error message in the output window, and *then* tried to locate the error
      |by clicking on the error hyperlink.
      """.stripMargin
      JOptionPane.showMessageDialog(null, msg, "Error Locator", JOptionPane.INFORMATION_MESSAGE)
    }

    if (errorText0 == null || errorText0.trim() == "") {
      return
    }
    else {
      val errorText = errorText0.trim()
      val code = Utils.stripCR(codePane.getText)
      val idx = code.indexOf(errorText)
      if (idx == -1) {
        showHelpMessage()
      }
      else {
        switchFocusToCodeEditor()
        codePane.select(idx, idx + errorText.length)
//        codePane.setCaretPosition(idx)
        val idx2 = code.lastIndexOf(errorText)
        if (idx != idx2) showFindDialog()
      }
    }
  }

  def switchFocusToCodeEditor() {
    // Need to do this in roundabout way because calling directly
    // into CodeEditorTopComponent makes scalac barf
    OutputTopComponent.findInstance.switchFocusToCodeEditor()
  }

  def clrOutput() {
    Utils.runInSwingThread {
      IO.getOut().reset()
      clearButton.setEnabled(false)
      codePane.requestFocusInWindow
    }
    lastOutput = ""
  }

  def clrEditor() {
    Utils.runInSwingThread {
      this.codePane.setText(null)
      clearSButton.setEnabled(false)
      codePane.requestFocusInWindow
    }
  }

  val listener = new OutputListener() {
    def outputLineAction(ev: OutputEvent) {
      locateError(ev.getLine)
    }

    def outputLineSelected(ev: OutputEvent) {
    }

    def outputLineCleared(ev: OutputEvent) {
    }
  }

  def enableClearButton() = if (!clearButton.isEnabled) clearButton.setEnabled(true)

  def readInput(prompt: String): String = {
    Utils.runInSwingThreadAndWait {
      val outText = prompt + " : "
      val promptSpaces = if (outText.length - 12 > 0) outText.length - 12 else 0

      OutputTopComponent.findInstance.requestActive
      IOColorPrint.print(IO, " " * promptSpaces + "Provide Input Below\n", promptMarkColor);
      IOColorPrint.print(IO, " " * outText.length + "V\n", promptMarkColor);
      IOColorPrint.print(IO, outText, promptColor);
    }

    IO.setInputVisible(true)
    val line = new java.io.BufferedReader(IO.getIn()).readLine()
    IO.setInputVisible(false)
    line
  }

  def showOutput(outText: String): Unit = showOutput(outText, outputColor)

  def showOutput(outText: String, color: Color): Unit = {
    Utils.runInSwingThread {
      IOColorPrint.print(IO, outText, color);
      enableClearButton()
    }
    lastOutput = outText
  }

  def showErrorMsg(errMsg: String) {
    Utils.runInSwingThread {
      IOColorPrint.print(IO, errMsg, Color.red);
      enableClearButton()
    }
    lastOutput = errMsg
  }

  def showErrorText(errText: String) {
    Utils.runInSwingThread {
      IOColorPrint.print(IO, errText, listener, true, Color.red);
      enableClearButton()
    }
    lastOutput = errText
  }

  def showWaitCursor() {
    val wc = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
    codePane.setCursor(wc)
    tCanvas.setCursor(wc)
  }

  def showNormalCursor() {
    val nc = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
    codePane.setCursor(nc);
    tCanvas.setCursor(nc)
  }

  def runCode() {
    // Runs on swing thread
    
    val code = codePane.getText()
    if (code == null || code.trim.length == 0) return
    if (code.contains(CommandHistory.Separator)) {
      showOutput(
        """|Sorry, you can't have the word %s in your script. This is an
           |internal reserved word within Kojo.
           |Please change %s to something else and rerun your script.""".stripMargin.format(CommandHistory.Separator, CommandHistory.Separator))
      return
    }

    // now that we use the proxy code runner, disable the run button right away and change
    // the cursor so that the user gets some feedback the first time he runs something
    // - relevant if the proxy is still loading the real runner
    runButton.setEnabled(false)
    showWaitCursor()

    val selStart = codePane.getSelectionStart
    val selEnd = codePane.getSelectionEnd

    val selectedCode = codePane.getSelectedText
    val codeToRun = if (selectedCode == null) code else selectedCode

    try {
      // always add full code to history
      historyManager.codeRun(code, (selectedCode != null) || !isSingleLine(code), (selStart, selEnd))
    }
    catch {
      case ioe: java.io.IOException => showOutput("Unable to save history to disk: %s\n" format(ioe.getMessage))
    }

    if (showCode) {
      showOutput("\n>>>\n", promptColor)
      showOutput(codeToRun, codeColor)
      showOutput("\n<<<\n", promptColor)
    }
    else {
      maybeOutputDelimiter()
    }

    codeRunner.runCode(codeToRun)
  }

  def maybeOutputDelimiter() {
    if (lastOutput.length > 0 && !lastOutput.endsWith(OutputDelimiter))
      showOutput(OutputDelimiter, promptColor)
  }

  def codeFragment(caretOffset: Int) = {
    val cpt = codePane.getText
    if (caretOffset > cpt.length) ""
    else Utils.stripCR(cpt).substring(0, caretOffset)
  }
  def methodCompletions(caretOffset: Int) = codeRunner.methodCompletions(codeFragment(caretOffset))
  def varCompletions(caretOffset: Int) = codeRunner.varCompletions(codeFragment(caretOffset))
  def keywordCompletions(caretOffset: Int) = codeRunner.keywordCompletions(codeFragment(caretOffset))

  def loadFrom(file: java.io.File) {
    import util.RichFile._
    val script = file.readAsString
    codePane.setText(script)
  }

  def saveTo(file0: java.io.File) {
    import util.RichFile._
    val script = codePane.getText()

    val file = if (file0.getName.endsWith(".kojo")) file0
    else new java.io.File(file0.getAbsolutePath + ".kojo")

    file.write(script)
  }

  class HistoryManager {
    var _selRange = (0, 0)

    def historyMoveBack {
      // depend on history listener mechanism to move back
      val prevCode = commandHistory.previous
      hPrevButton.setEnabled(commandHistory.hasPrevious)
      hNextButton.setEnabled(true)
      commandHistory.ensureLastEntryVisible()
    }

    def historyMoveForward {
      // depend on history listener mechanism to move forward
      val nextCode = commandHistory.next
      if(!nextCode.isDefined) {
        hNextButton.setEnabled(false)
      }
      hPrevButton.setEnabled(true)
      commandHistory.ensureLastEntryVisible()
    }

    def setCode(historyIdx: Int, selRange: (Int, Int) = (0,0)) {
      if (commandHistory.size > 0 && historyIdx != 0)
        hPrevButton.setEnabled(true)
      else
        hPrevButton.setEnabled(false)

      if (historyIdx < commandHistory.size)
        hNextButton.setEnabled(true)
      else
        hNextButton.setEnabled(false)

      val codeAtIdx = commandHistory.toPosition(historyIdx)
      Utils.runInSwingThread {
        if(codeAtIdx.isDefined) {
          codePane.setText(codeAtIdx.get)
          if (selRange._1 != selRange._2) {
            codePane.setSelectionStart(selRange._1)
            codePane.setSelectionEnd(selRange._2)
          }
        }
        else {
          codePane.setText(null)
        }
        codePane.requestFocusInWindow
      }
    }

    def codeRunError() = {
      setCode(commandHistory.size-1, (_selRange._1, _selRange._2))
      _selRange = (0,0)
    }

    def codeRun(code: String, stayPut: Boolean, selRange: (Int, Int)) {
      _selRange = selRange
      val tcode = code.trim()
      val undo = (tcode == "undo"
                  || tcode == "undo()"
                  || tcode.endsWith(".undo")
                  || tcode.endsWith(".undo()"))

      val prevIndex = commandHistory.hIndex

      if (!undo) {
        commandHistory.add(code)
      }
      else {
        // undo
        _selRange = (0, 0)
      }
      if (stayPut || undo) {
        setCode(commandHistory.size-1, (_selRange._1, _selRange._2))
      }
      if (commandHistory.hIndex == prevIndex + 1) {
        // the last entry within history was selected
        commandHistory.ensureLastEntryVisible()
      }
      else {
        commandHistory.ensureVisible(prevIndex)
      }
    }
  }

  def runCodeWithOutputCapture(): String = {
    runMonitor = new OutputCapturingRunner()
    val ret = runMonitor.asInstanceOf[OutputCapturingRunner].go()
    runMonitor = new NoOpRunMonitor()
    ret
  }

  class OutputCapturingRunner extends RunMonitor {
    val outputx: StringBuilder = new StringBuilder()
    val latch = new CountDownLatch(1)

    def reportOutput(outText: String) = captureOutput(outText)
    def onRunStart() {}
    def onRunEnd() = latch.countDown()

    def go(): String = {
      runCode()
      latch.await()
      outputx.toString
    }

    def captureOutput(output: String) {
      outputx.append(output)
    }
  }

  class StatusStrip extends JPanel {
    val ErrorColor = new Color(0xff1a1a) // reddish
    val SuccessColor = new Color(0x33ff33) // greenish
    val NeutralColor = new Color(0xf0f0f0) // very light gray
    val StripWidth = 6

    setBackground(NeutralColor)
    setPreferredSize(new Dimension(StripWidth, 10))

    def linkToPane() {
      codePane.getDocument.addDocumentListener(new DocumentListener {
          def insertUpdate(e: DocumentEvent) = onDocChange()
          def removeUpdate(e: DocumentEvent) = onDocChange()
          def changedUpdate(e: DocumentEvent) {}
        })
    }

    def onSuccess() {
      setBackground(SuccessColor)
    }

    def onError() {
      setBackground(ErrorColor)
    }

    def onDocChange() {
      if (getBackground != NeutralColor) setBackground(NeutralColor)
    }
  }

}

trait RunMonitor {
  def reportOutput(outText: String)
  def onRunStart()
  def onRunEnd()
}

class NoOpRunMonitor extends RunMonitor {
  def reportOutput(outText: String) {}
  def onRunStart() {}
  def onRunEnd() {}
}
