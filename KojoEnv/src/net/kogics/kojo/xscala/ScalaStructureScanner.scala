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

import org.netbeans.modules.csl.api._
import org.netbeans.modules.csl.spi._
import StructureScanner._

class ScalaStructureScanner extends StructureScanner {
  println("ScalaStructureScanner created")

  override def scan(result: ParserResult): java.util.List[StructureItem] = {
    println("scan")
    java.util.Collections.emptyList[StructureItem]
  }

  override def folds(result: ParserResult): java.util.Map[String, java.util.List[OffsetRange]] = {
    println("folds")
    java.util.Collections.emptyMap[String, java.util.List[OffsetRange]]  
  }

  override def getConfiguration: Configuration = null
}
