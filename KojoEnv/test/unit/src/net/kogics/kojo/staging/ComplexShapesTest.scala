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
package staging

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert._

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{CountDownLatch, TimeUnit}

import net.kogics.kojo.util._

class ComplexShapesTest extends StagingTestBase {
  val f = SpriteCanvas.instance.figure0
  def peekPNode = f.dumpLastChild

  @Test
  // lalit sez: if we have more than five tests, we run out of heap space - maybe a leak in the Scala interpreter/compiler
  // subsystem. So we run (mostly) everything in one test
  def test1 = {
    //W
    //W==Complex Shapes==
    //W
    //W===Polylines===
    //W
    //W{{{
    //Wpolyline(points)
    Tester("import Staging._ ; polyline(List((15, 15), (25, 35), (40, 20), (45, 25), (50, 10)))")
    assertEquals("PolyLine(15,10 L25.0000,35.0000 L40.0000,20.0000 " +
                 "L45.0000,25.0000 L50.0000,10.0000 M0.00000,0.00000 )",
                 makeString(peekPNode))

    //WaPolyline.toPolyline
    Tester("import Staging._ ; polyline(List((15, 15), (25, 35))).toPolyline")

    //WaPolygon.toPolyline
    Tester("import Staging._ ; polygon(List((15, 15), (25, 35))).toPolyline")
    
    //W}}}
    //W

    //W
    //W===Polygons===
    //W
    //W{{{
    //Wpolygon(points)
    Tester("""import Staging._
             |polygon(List((15, 15), (25, 35),
             |(40, 20), (45, 25), (50, 10)))""".stripMargin)
    assertEquals("PolyLine(15,10 L25.0000,35.0000 L40.0000,20.0000 " +
                 "L45.0000,25.0000 L50.0000,10.0000 z M0.00000,0.00000 )",
                 makeString(peekPNode))

    //WaPolyline.toPolygon
    Tester("""import Staging._
             |polyline(List((15, 15), (25, 35),
             |(40, 20), (45, 25), (50, 10))).toPolygon""".stripMargin)

    //WaPolygon.toPolygon
    Tester("""import Staging._
             |polygon(List((15, 15), (25, 35),
             |(40, 20), (45, 25), (50, 10))).toPolygon""".stripMargin)

    //W}}}
    //W
    //W{{{
    //Wtriangle(point1, point2, point3)
    Tester("import Staging._ ; triangle((15, 15), (25, 35), (35, 15))")
    assertEquals("PolyLine(15,15 L25.0000,35.0000 L35.0000,15.0000 " +
                 "z M0.00000,0.00000 )",
                 makeString(peekPNode))

    //W}}}
    //W
    //W{{{
    //Wquad(point1, point2, point3, point4)
    Tester("import Staging._ ; quad((15, 15), (25, 35), (40, 20), (35, 10))")
    assertEquals("PolyLine(15,10 L25.0000,35.0000 L40.0000,20.0000 " +
                 "L35.0000,10.0000 z M0.00000,0.00000 )",
                 makeString(peekPNode))

    //W}}}
    //W

    //W
    //W===Line pattern===
    //W
    //W{{{
    //WlinesShape(points)
    val points = """List((10, 20), (10, 50),
       |(20, 50), (20, 20),
       |(30, 20), (30, 50),
       |(40, 50), (40, 20),
       |(50, 20), (50, 50),
       |(60, 50), (60, 20))""".stripMargin
    Tester("import Staging._ ; linesShape(" + points + ")")
    assertEquals("PolyLine(10,20 L10.0000,50.0000 " +
                 "M20.0000,50.0000 L20.0000,20.0000 " +
                 "M30.0000,20.0000 L30.0000,50.0000 " +
                 "M40.0000,50.0000 L40.0000,20.0000 " +
                 "M50.0000,20.0000 L50.0000,50.0000 " +
                 "M60.0000,50.0000 L60.0000,20.0000 " +
                 "M0.00000,0.00000 )",
                 makeString(peekPNode))

    //W}}}
    //W
    //Wdraws one line for each two points.

    //W
    //W===Triangles pattern===
    //W
    //W{{{
    //WtrianglesShape(points)
    Tester("import Staging._ ; trianglesShape(" + points + ")")
    assertEquals("PolyLine(10,20 L10.0000,50.0000 L20.0000,50.0000 z " +
                 "M20.0000,20.0000 L30.0000,20.0000 L30.0000,50.0000 z " +
                 "M40.0000,50.0000 L40.0000,20.0000 L50.0000,20.0000 z " +
                 "M50.0000,50.0000 L60.0000,50.0000 L60.0000,20.0000 z " +
                 "M0.00000,0.00000 )",
                 makeString(peekPNode))

    //W}}}
    //W
    //Wdraws one triangle for each three points.
  }

  @Test
  def test2 = {
    f.clear

    val points = """List((10, 20), (10, 50),
       |(20, 50), (20, 20),
       |(30, 20), (30, 50),
       |(40, 50), (40, 20),
       |(50, 20), (50, 50),
       |(60, 50), (60, 20))""".stripMargin
    //W
    //W===Triangle strip pattern===
    //W
    //W{{{
    //WtriangleStripShape(points)
    val tssPoints = """List((10, 20), (10, 50),
       |(20, 20), (20, 50),
       |(30, 20), (30, 50),
       |(40, 20), (40, 50),
       |(50, 20))""".stripMargin
    Tester("import Staging._ ; triangleStripShape(" + tssPoints + ")")
    assertEquals("PolyLine(10,20 L10.0000,50.0000 L20.0000,20.0000 z " +
                 "M10.0000,50.0000 L20.0000,20.0000 L20.0000,50.0000 z " +
                 "M20.0000,20.0000 L20.0000,50.0000 L30.0000,20.0000 z " +
                 "M20.0000,50.0000 L30.0000,20.0000 L30.0000,50.0000 z " +
                 "M30.0000,20.0000 L30.0000,50.0000 L40.0000,20.0000 z " +
                 "M30.0000,50.0000 L40.0000,20.0000 L40.0000,50.0000 z " +
                 "M40.0000,20.0000 L40.0000,50.0000 L50.0000,20.0000 z " +
                 "M0.00000,0.00000 )",
                 makeString(peekPNode))

    //W}}}
    //W
    //Wdraws a contiguous pattern of triangles.

    //W
    //W===Quads pattern===
    //W
    //W{{{
    //WquadsShape(points)
    Tester("import Staging._ ; quadsShape(" + points + ")")
    assertEquals("PolyLine(10,20 L10.0000,50.0000 L20.0000,50.0000 L20.0000,20.0000 z " +
                 "M30.0000,20.0000 L30.0000,50.0000 L40.0000,50.0000 L40.0000,20.0000 z " +
                 "M50.0000,20.0000 L50.0000,50.0000 L60.0000,50.0000 L60.0000,20.0000 z " +
                 "M0.00000,0.00000 )",
                 makeString(peekPNode))

    //W}}}
    //W
    //Wdraws one quad for each four points.

    //W
    //W===Quad strip pattern===
    //W
    //W{{{
    //WquadStripShape(points)
    val points2 = """List((10, 20), (10, 50),
       |(20, 50), (20, 20),
       |(30, 20), (30, 50),
       |(40, 50), (40, 20))""".stripMargin
    Tester("import Staging._ ; quadStripShape(" + points2 + ")")
    assertEquals("PolyLine(10,20 L10.0000,50.0000 L20.0000,50.0000 L20.0000,20.0000 z " +
                 "M20.0000,50.0000 L20.0000,20.0000 L30.0000,20.0000 L30.0000,50.0000 z " +
                 "M30.0000,20.0000 L30.0000,50.0000 L40.0000,50.0000 L40.0000,20.0000 z " +
                 "M0.00000,0.00000 )",
                 makeString(peekPNode))

    //W}}}
    //W
    //Wdraws a contiguous pattern of quads.

    //W
    //W===Triangle fan pattern===
    //W
    //W{{{
    //WtriangleFanShape(points)
    val tfsPoints = """List(
       |(30, 45), (40, 40),
       |(40, 40), (45, 30),
       |(45, 30), (40, 20),
       |(40, 20), (30, 15),
       |(30, 15), (20, 20),
       |(20, 20), (15, 30),
       |(15, 30), (20, 40))""".stripMargin
    Tester("import Staging._ ; triangleFanShape((30, 30), " + tfsPoints + ")")
    println(makeString(peekPNode)) ; fail
  /*

    assertEquals("" +
                 "",
                 makeString(peekPNode))

    /* TODO find problem: code works but the test fails
    testPolyLine(f.dumpChild(n), "L30,45 L40,40 M30,30 L40,40 L45,30 M30,30 " +
                 "L45,30 L40,20 M30,30 L40,20 L30,15 M30,30 L30,15 L20,20 " +
                 "M30,30 L20,20 L15,30 M30,30 L15,30 L20,40 M0.0,0.0 ")
*/
    n += 1

    //W}}}
    //W
    //Wdraws a pattern of triangles around a central point.

//    println(ppathToString(f.dumpChild(n).asInstanceOf[edu.umd.cs.piccolo.nodes.PPath]))
    */
  }
}

