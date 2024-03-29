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
package figure

import edu.umd.cs.piccolo._
import edu.umd.cs.piccolo.nodes._
import edu.umd.cs.piccolo.util._
import edu.umd.cs.piccolo.event._
import edu.umd.cs.piccolo.activities.PActivity
import edu.umd.cs.piccolo.activities.PActivity.PActivityDelegate

import javax.swing._
import java.awt.{Point => _, _}

import net.kogics.kojo.util.Utils
import core._

object Figure {
  def apply(canvas: SpriteCanvas, initX: Double = 0d, initY: Double = 0): Figure = {
    val fig = Utils.runInSwingThreadAndWait {
      new Figure(canvas, initX, initY)
    }
    fig
  }
}

class Figure private (canvas: SpriteCanvas, initX: Double, initY: Double) extends core.Figure {
  private val bgLayer = new PLayer
  private val fgLayer = new PLayer
  private var currLayer = bgLayer

  def dumpLastChild = currLayer.getChild(currLayer.getChildrenCount - 1)

  def dumpChild(n: Int): PNode = {
    try {
      currLayer.getChild(n)
    }
    catch { case e => throw e }
  }
  
  // if fgLayer is bigger than bgLayer, (re)painting does not happen very cleanly
  // needs a better fix than the one below
  bgLayer.setBounds(-500, -500, 1000, 1000)

  private val camera = canvas.getCamera
  val DefaultColor = Color.red
  val DefaultFillColor: Color = null
  val DefaultStroke = new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
  @volatile private var listener: SpriteListener = NoopSpriteListener

  private var figAnimation: PActivity = _
  private var _lineColor: Color = _
  private var _fillColor: Color = _
  private var _lineStroke: Stroke = _

  camera.addLayer(camera.getLayerCount-1, bgLayer)
  camera.addLayer(camera.getLayerCount-1, fgLayer)
  init()

  def init() {
    bgLayer.setOffset(initX, initY)
    fgLayer.setOffset(initX, initY)
    _lineColor = DefaultColor
    _fillColor = DefaultFillColor
    _lineStroke = DefaultStroke
  }

  def fillColor = Utils.runInSwingThreadAndWait {
    _fillColor
  }

  def lineColor = Utils.runInSwingThreadAndWait {
    _lineColor
  }

  def lineStroke = Utils.runInSwingThreadAndWait {
    _lineStroke
  }

  def repaint() {
    bgLayer.repaint()
    fgLayer.repaint()
  }

  def clear {
    Utils.runInSwingThread {
      bgLayer.removeAllChildren()
      fgLayer.removeAllChildren()
      init()
      repaint()
    }
  }

  def fgClear {
    Utils.runInSwingThread {
      fgLayer.removeAllChildren()
      repaint()
    }
  }

  def remove() {
    Utils.runInSwingThread {
      camera.removeLayer(bgLayer)
      camera.removeLayer(fgLayer)
    }
  }

  def setPenColor(color: java.awt.Color) {
    Utils.runInSwingThread {
      _lineColor = color
    }
  }

  def setPenThickness(t: Double) {
    Utils.runInSwingThread {
      _lineStroke = new BasicStroke(t.toFloat, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
    }
  }

  def setLineStroke(st: Stroke) {
    Utils.runInSwingThread {
      _lineStroke = st
    }
  }

  def setFillColor(color: java.awt.Color) {
    Utils.runInSwingThread {
      _fillColor = color
    }
  }

  type FPoint = FigPoint
  type FLine = FigLine
  type FEllipse = FigEllipse
  type FArc = FigArc
  type FText = FigText
  type FRectangle = FigRectangle
  type FRRectangle = FigRoundRectangle
  type FPolyLine = FigShape


  def point(x: Double, y: Double): FigPoint = {
    val pt = new FigPoint(canvas, x,y)
    Utils.runInSwingThread {
      pt.pPoint.setStroke(_lineStroke)
      pt.pPoint.setStrokePaint(_lineColor)
      currLayer.addChild(pt.pPoint)
      currLayer.repaint()
    }
    pt
  }

  def line(p1: Point, p2: Point): FigLine = {
    val line = new FigLine(canvas, p1, p2)
    Utils.runInSwingThread {
      line.pLine.setStroke(_lineStroke)
      line.pLine.setStrokePaint(_lineColor)
      currLayer.addChild(line.pLine)
      currLayer.repaint()
    }
    line
  }

  def line(x0: Double, y0: Double, x1: Double, y1: Double) = line(new Point(x0, y0), new Point(x1, y1))

  def ellipse(center: Point, w: Double, h: Double): FigEllipse = {
    val ell = new FigEllipse(canvas, center, w, h)
    Utils.runInSwingThread {
      ell.pEllipse.setStroke(_lineStroke)
      ell.pEllipse.setStrokePaint(_lineColor)
      ell.pEllipse.setPaint(_fillColor)
      currLayer.addChild(ell.pEllipse)
      currLayer.repaint()
    }
    ell
  }

  def ellipse(cx: Double, cy: Double, w: Double, h: Double): FigEllipse = {
    ellipse(new Point(cx, cy), w, h)
  }

  def circle(cx: Double, cy: Double, radius: Double) = ellipse(cx, cy, 2*radius, 2*radius)
  
  def circle(cp: Point, radius: Double) = circle(cp.x, cp.y, radius)


  def arc(onEll: Ellipse, start: Double, extent: Double): FigArc = {
    val arc = new FigArc(canvas, onEll, start, extent)
    Utils.runInSwingThread {
      arc.pArc.setStroke(_lineStroke)
      arc.pArc.setStrokePaint(_lineColor)
      arc.pArc.setPaint(_fillColor)
      currLayer.addChild(arc.pArc)
      currLayer.repaint()
    }
    arc

  }

  def arc(cx: Double, cy: Double, w: Double, h: Double, start: Double, extent: Double): FigArc = {
    arc(new Ellipse(new Point(cx, cy), w, h), start, extent)
  }

  def arc(cx: Double, cy: Double, r: Double, start: Double, extent: Double): FigArc = {
    arc(cx, cy, 2*r, 2*r, start, extent)
  }

  def arc(cp: Point, r: Double, start: Double, extent: Double): FArc = {
    arc(cp.x, cp.y, 2*r, 2*r, start, extent)
  }


  def rectangle(bLeft: Point, tRight: Point): FigRectangle = {
    val rect = new FigRectangle(canvas, bLeft, tRight)
    Utils.runInSwingThread {
      rect.pRect.setStroke(_lineStroke)
      rect.pRect.setStrokePaint(_lineColor)
      rect.pRect.setPaint(_fillColor)
      currLayer.addChild(rect.pRect)
      currLayer.repaint()
    }
    rect
  }

  def rectangle(x0: Double, y0: Double, w: Double, h: Double) = rectangle(new Point(x0, y0), new Point(x0+w, y0+h))

  def roundRectangle(p1: Point, p2: Point, rx: Double, ry: Double) = {
    val rrect = new FigRoundRectangle(canvas, p1, p2, rx, ry)
    Utils.runInSwingThread {
      rrect.pRect.setStroke(_lineStroke)
      rrect.pRect.setStrokePaint(_lineColor)
      rrect.pRect.setPaint(_fillColor)
      currLayer.addChild(rrect.pRect)
      currLayer.repaint()
    }
    rrect
  }

  def text(content: String, x: Double, y: Double): FigText = {
    val txt = new FigText(canvas, content, x, y)
    Utils.runInSwingThread {
      txt.pText.setTextPaint(_lineColor)
      currLayer.addChild(txt.pText)
      currLayer.repaint()
    }
    txt
  }

  def text(content: String, p: Point): FText = text(content, p.x, p.y)


  def polyLine(path: kgeom.PolyLine) = {
    val cv = canvas
    // fake a FigShape-derived class for the benefit of staging.Shape.shapes
    val poly = new FigShape {
      val pLine = path
      val canvas = cv
      val piccoloNode = pLine
    }
    Utils.runInSwingThread {
      poly.pLine.setStroke(_lineStroke)
      poly.pLine.setStrokePaint(_lineColor)
      poly.pLine.setPaint(_fillColor)
      currLayer.addChild(poly.pLine)
      currLayer.repaint()
    }
    poly
  }


  def pnode(node: PNode) = {
    Utils.runInSwingThread {
      if (node.isInstanceOf[PPath]) {
        val p = node.asInstanceOf[PPath]
        p.setPaint(_fillColor)
        p.setStroke(_lineStroke)
        p.setStrokePaint(_lineColor)
      }
      else if (node.isInstanceOf[PText]) {
        val t = node.asInstanceOf[PText]
        t.setTextPaint(_lineColor)
      }
      currLayer.addChild(node)
      currLayer.repaint
    }
    node
  }


  def refresh(fn: => Unit) {
    
    Utils.runInSwingThread {
      if (figAnimation != null ) {
        return
      }
      
      figAnimation = new PActivity(-1) {
        override def activityStep(elapsedTime: Long) {
          currLayer = fgLayer
          try {
            staging.Inputs.activityStep
            fn
            if (isStepping) {
              listener.hasPendingCommands()
            }
          }
          catch {
            case t: Throwable =>
              canvas.outputFn("Problem: " + t.toString())
              stop()
          }
          finally {
            repaint()
            currLayer = bgLayer
          }
        }
      }

      figAnimation.setDelegate(new PActivityDelegate {
          override def activityStarted(activity: PActivity) {}
          override def activityStepped(activity: PActivity) {}
          override def activityFinished(activity: PActivity) {
            listener.pendingCommandsDone()
          }
        })

      canvas.getRoot.addActivity(figAnimation)
    }
  }

  def stopRefresh() = stop()

  def stop() {
    Utils.runInSwingThread {
      if (figAnimation != null) {
        figAnimation.terminate(PActivity.TERMINATE_AND_FINISH)
        figAnimation = null
      }
    }
  }

  def onMouseMove(fn: (Double, Double) => Unit) {
    canvas.addInputEventListener(new PBasicInputEventHandler {
        override def mouseMoved(e: PInputEvent) {
          val pos = e.getPosition
          fn(pos.getX, pos.getY)
          currLayer.repaint()
        }
      })
  }

  private [kojo] def setSpriteListener(l: SpriteListener) {
    listener = l
  }
}

