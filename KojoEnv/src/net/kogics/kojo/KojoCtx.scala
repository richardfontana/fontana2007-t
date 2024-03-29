/*
 * Copyright (C) 2010 Lalit Pant <pant.lalit@gmail.com>
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

import net.kogics.kojo.util.Utils
import org.openide.windows.TopComponent


object KojoCtx extends Singleton[KojoCtx] {
  protected def newInstance = new KojoCtx
}


class KojoCtx extends core.KojoCtx {

  def activate(tc: TopComponent) {
    if (!tc.isOpened) {
      tc.open()
    }
    tc.requestActive()
  }

  def makeCanvasVisible() {
    Utils.runInSwingThread {
      val tc = SCanvasTopComponent.findInstance()
      activate(tc)
    }
    activateCodeEditor()
  }

  def makeMathWorldVisible() {
    Utils.runInSwingThread {
      val tc = GeoGebraTopComponent.findInstance()
      activate(tc)
    }
    activateCodeEditor()
  }

  def activateCodeEditor() {
    Utils.runInSwingThread {
      val tc = CodeEditorTopComponent.findInstance()
      activate(tc)
    }
  }
}
