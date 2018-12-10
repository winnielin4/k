package org.kframework.unparser

import java.util

import org.kframework.parser.{Ambiguity, Constant, Term, TermCons}
import org.kframework.utils.StringUtil

import scala.collection.JavaConverters._


object TreeNodesToK5AST {

  def apply(t: Term): String = t match {
    case c@Constant(s, p) => "#token(" + StringUtil.enquoteCString(s) + "," + StringUtil.enquoteCString(p.sort.name) + ")"
    case tc@TermCons(items, p) =>
      "`_" + p.klabel.get + "`(" + // KORE.parse tries to down some terms so prefix the label to keep it as a kapp
      (if (items.isEmpty)
        ".KList"
      else
        (new util.ArrayList(items).asScala.reverse map apply).mkString(",")) +
      ")"
    case Ambiguity(items) => //"amb(" + (items.asScala map apply).mkString(",") + ")"
      items.asScala.foldRight("bottom(.KList)") { (i, acc) => "amb(" + apply(i) + "," + acc + ")" }
  }
}
