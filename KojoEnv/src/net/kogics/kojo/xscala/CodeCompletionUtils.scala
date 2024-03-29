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

package net.kogics.kojo.xscala

object CodeCompletionUtils {
  val NotIdChars = """ .(){}!%&+\-<=>?@\\^`|~#:/*""" + "\n\r\t"

  import org.netbeans.modules.scala.core.lexer.ScalaTokenId
  val Keywords = 
    ScalaTokenId.values.filter { v =>
      v.asInstanceOf[ScalaTokenId.V].primaryCategory == "keyword"
    }.map { v =>
      v.asInstanceOf[ScalaTokenId.V].fixedText
    }.toList

  val KeywordTemplates = Map(
    "for" -> "for (i <- 1 to ${n}) {\n    ${cursor}\n}",
    "while" -> "while (${condition}) {\n    ${cursor}\n}",
    "if" -> "if (${condition}) {\n    ${cursor}\n}"
  )
  val MethodTemplates = collection.mutable.Map(
    "point" -> "point(${x}, ${y})",
    "line" -> "line(${x0}, ${y0}, ${x1}, ${y1})",
    "rectangle" -> "rectangle(${x0}, ${y0}, ${width}, ${height})",
    "text" -> "text(${content}, ${x}, ${y})",
    "circle" -> "circle(${cx}, ${cy}, ${radius})",
    "ellipse" -> "ellipse(${cx}, ${cy}, ${width}, ${height})",
    "arc" -> "arc(${cx}, ${cy}, ${rx}, ${ry}, ${startDegree}, ${extentDegree})",
    "refresh" -> "refresh {\n    ${cursor}\n}",
    "screenSize" -> "screenSize(${width}, ${height})",
    "background" -> "background(${color})",
    "dot" -> "dot(${x}, ${y})",
    "square" -> "square(${x}, ${y}, ${side})",
    "roundRectangle" -> "roundRectangle(${x}, ${y}, ${width}, ${height}, ${rx}, ${ry})",
    "pieslice" -> "pieslice(${cx}, ${cy}, ${rx}, ${ry}, ${startDegree}, ${extentDegree})",
    "openArc" -> "openArc(${cx}, ${cy}, ${rx}, ${ry}, ${startDegree}, ${extentDegree})",
    "chord" -> "chord(${cx}, ${cy}, ${rx}, ${ry}, ${startDegree}, ${extentDegree})",
    "vector" -> "vector(${x0}, ${y0}, ${x1}, ${y1}, ${headLength})",
    "star" -> "star(${cx}, ${cy}, ${inner}, ${outer}, ${numPoints})",
    "polyline" -> "polyline(${points})",
    "polygon" -> "polygon(${points})",
    "triangle" -> "triangle(${point1}, ${point2}, ${point3})",
    "quad" -> "quad(${point1}, ${point2}, ${point3}, ${point4})",
    "svgShape" -> "svgShape(${element})",
    "grayColors" -> "grayColors(${highGrayNum})",
    "grayColorsWithAlpha" -> "grayColorsWithAlpha(${highGrayNum}, ${highAlphaNum})",
    "rgbColors" -> "rgbColors(${highRedNum}, ${highGreenNum}, ${highBlueNum})",
    "rgbColorsWithAlpha" -> "rgbColorsWithAlpha(${highRedNum}, ${highGreenNum}, ${highBlueNum}, ${highAlphaNum})",
    "hsbColors" -> "hsbColors(${highHueNum}, ${highSaturationNum}, ${highBrightnessNum})",
    "namedColor" -> "namedColor(${colorName})",
    "lerpColor" -> "lerpColor(${colorFrom}, ${colorTo}, ${amount})",
    "fill" -> "fill(${color})",
    "noFill" -> "noFill()",
    "stroke" -> "stroke(${color})",
    "noStroke" -> "noStroke()",
    "strokeWidth" -> "strokeWidth(${width})",
    "withStyle" -> "withStyle (${fillColor}, ${strokeColor}, ${strokeWidth}) {\n    ${cursor}\n}",
    "constrain" -> "constrain(${value}, ${min}, ${max})",
    "norm" -> "norm(${value}, ${low}, ${high})",
    "map" -> "map(${value}, ${min1}, ${max1}, ${min2}, ${max2})",
    "sq" -> "sq(${value})",
    "dist" -> "dist(${x0}, ${y0}, ${x1}, ${y1})",
    "mag" -> "mag(${x}, ${y})",
    "lerp" -> "lerp(${low}, ${high}, ${value})",
    "loop" -> "loop {\n    ${cursor}\n}",
    "stop" -> "stop()",
    "reset" -> "reset()",
    "wipe" -> "wipe()"
  )
  
  val MethodDropFilter = List("turtle0")
  val VarDropFilter = List("builtins", "predef")
  val InternalVarsRe = java.util.regex.Pattern.compile("""res\d+""")

  def notIdChar(c: Char): Boolean =  NotIdChars.contains(c)

  def findLastIdentifier(rstr: String): Option[String] = {
    val str = " " + rstr
    var remaining = str.length
    while(remaining > 0) {
      if (notIdChar(str(remaining-1))) return Some(str.substring(remaining))
      remaining -= 1
    }
    None
  }

  def findIdentifier(str: String): (Option[String], Option[String]) = {
    if (str.length == 0) return (None, None)

    if (str.endsWith(".")) {
      (findLastIdentifier(str.substring(0, str.length-1)), None)
    }
    else {
      val lastDot = str.lastIndexOf('.')
      if (lastDot == -1) (None, findLastIdentifier(str))
      else {
        val tPrefix = str.substring(lastDot+1)
        if (tPrefix == findLastIdentifier(tPrefix).get)
          (findLastIdentifier(str.substring(0, lastDot)), Some(tPrefix))
        else
          (None, findLastIdentifier(tPrefix))
      }
    }
  }
}
