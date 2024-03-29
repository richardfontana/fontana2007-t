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

package net.kogics.kojo.core

import java.util.concurrent.CountDownLatch

trait CodeRunner {
  def interruptInterpreter(): Unit
  def runCode(code: String): Unit
  def methodCompletions(str: String): (List[String], Int)
  def varCompletions(str: String): (List[String], Int)
  def keywordCompletions(str: String): (List[String], Int)
}

trait RunContext {
  def onInterpreterInit(): Unit
  def onInterpreterStart(code: String): Unit
  def onRunError(): Unit
  def onRunSuccess(): Unit
  def onRunInterpError(): Unit

  def println(outText: String)
  def reportOutput(outText: String)
  def reportErrorMsg(errMsg: String)
  def reportErrorText(errText: String)

  def readInput(prompt: String): String

  def showScriptInOutput(): Unit
  def hideScriptInOutput(): Unit
  def showVerboseOutput(): Unit
  def hideVerboseOutput(): Unit
  def clearOutput(): Unit

  def inspect(obj: AnyRef): Unit
}

class ProxyCodeRunner(codeRunnerMaker: () => CodeRunner) extends CodeRunner {
  val latch = new CountDownLatch(1)
  @volatile var codeRunner: CodeRunner = _

  new Thread(new Runnable {
      def run {
        codeRunner = codeRunnerMaker()
        latch.countDown()
      }
    }).start()

  def interruptInterpreter() {
    latch.await()
    codeRunner.interruptInterpreter()
  }

  def runCode(code: String) {
    latch.await()
    codeRunner.runCode(code)
  }

  def methodCompletions(str: String): (List[String], Int) = {
    latch.await()
    codeRunner.methodCompletions(str)
  }

  def varCompletions(str: String): (List[String], Int) = {
    latch.await()
    codeRunner.varCompletions(str)
  }

  def keywordCompletions(str: String): (List[String], Int) = {
    latch.await()
    codeRunner.keywordCompletions(str)
  }
}
