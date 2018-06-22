/*
 * Copyright © 2014 TU Berlin (emma@dima.tu-berlin.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.emmalanguage
package compiler

import api.CSVConverter
import api.Meta
import api.alg.Alg

trait LabyrinthCompilerBase extends Compiler {

  import UniverseImplicits._

  lazy val StreamExecutionEnvironment = api.Type[org.apache.flink.streaming.api.scala.StreamExecutionEnvironment]

  val core = Core.Lang

  val Seq(_1, _2) = {
    val tuple2 = api.Type[(Nothing, Nothing)]
    for (i <- 1 to 2) yield tuple2.member(api.TermName(s"_$i")).asTerm
  }

  val Seq(_3_1, _3_2, _3_3) = {
    val tuple3 = api.Type[(Nothing, Nothing, Nothing)]
    for (i <- 1 to 3) yield tuple3.member(api.TermName(s"_$i")).asTerm
  }

  def prePrint(t: u.Tree) : Boolean = {
//    print("\nprePrint: ")
//    print(t)
//    print("   type: ")
//    print(t.tpe)
//    t match {
//      case core.ValDef(lhs, rhs) =>
//        print("   isFun: ")
//        println(isFun(lhs))
//      case _ => ()
//    }
    true
  }

  def postPrint(t: u.Tree) : Unit = {
    print("postPrint: ")
    print(t)
    print("   type: ")
    println(t.tpe.widen)
//    t match {
//      case core.Let(vals, _, expr) =>
//        //        print("vals: ")
//        //        println(vals)
//        //        print(" expression: ")
//        //        println(expr)
//        ()
//      case _ => ()
//    }
  }

  def countSeenRefs(t: u.Tree, m: scala.collection.mutable.Map[u.TermSymbol, u.TermSymbol]) : Int = {
    val refs = t.collect{
      case vr @ core.ValRef(_) => vr.name
      case pr @ core.ParRef(_) => pr.name
    }
    refs.foldLeft(0)((a,b) => a + (if (m.keys.toList.map(_.name).contains(b)) 1 else 0))
  }

  def refsSeen(t: u.Tree, m: scala.collection.mutable.Map[u.TermSymbol, u.TermSymbol]) : Boolean = {
    val refNames = t.collect{
      case vr @ core.ValRef(_) => vr
      case pr @ core.ParRef(_) => pr
    }.map(_.name)
    val seenNames = m.keys.toSeq.map(_.name)
    refNames.foldLeft(false)((a,b) => a || seenNames.contains(b))
  }

  def refSeen(t: Option[u.Tree], m: scala.collection.mutable.Map[u.TermSymbol, u.TermSymbol]): Boolean = {
    if (t.nonEmpty) {
      t.get match {
        case core.ValRef(sym) => {
          println
          m.keys.toList.contains(sym)
        }
        case _ => false
      }
    } else {
      false
    }
  }

  // check if a tree is of type databag
  def isDatabag(tree: u.Tree) : Boolean = {
    tree.tpe.widen.typeConstructor =:= API.DataBag.tpe
  }

  def isAlg(tree: u.Tree) : Boolean = {
    val out = tree.tpe.widen.typeConstructor.baseClasses.contains(Alg$.sym)
    out
  }

  // check if a symbol refers to a function
  def isFun(sym: u.TermSymbol) = api.Sym.funs(sym.info.dealias.widen.typeSymbol)
  def isFun(sym: u.Symbol) = api.Sym.funs(sym.info.dealias.widen.typeSymbol)

  def newValSym(own: u.Symbol, name: String, rhs: u.Tree): u.TermSymbol = {
    api.ValSym(own, api.TermName.fresh(name), rhs.tpe.widen)
  }

  def newParSym(own: u.Symbol, name: String, rhs: u.Tree): u.TermSymbol = {
    api.ParSym(own, api.TermName.fresh(name), rhs.tpe.widen)
  }

  def valRefAndDef(own: u.Symbol, name: String, rhs: u.Tree): (u.Ident, u.ValDef) = {
    val sbl = api.ValSym(own, api.TermName.fresh(name), rhs.tpe.widen)
    (core.ValRef(sbl), core.ValDef(sbl, rhs))
  }

  def valRefAndDef(sbl: u.TermSymbol, rhs: u.Tree): (u.Ident, u.ValDef) = {
    (core.ValRef(sbl), core.ValDef(sbl, rhs))
  }

  object Seq$ extends ModuleAPI {

    lazy val sym = api.Sym[Seq.type].asModule

    val apply = op("apply")

    override def ops = Set()
  }

  object Alg$ extends ClassAPI {
    lazy val sym = api.Sym[org.emmalanguage.api.alg.Alg[Any, Any]].asClass
    override def ops = Set()
  }

  object DB$ extends ModuleAPI {

    lazy val sym = api.Sym[DB.type].asModule

    val collect = op("collect")
    val singSrc = op("singSrc")
    val fold1 = op("fold1")
    val fold2 = op("fold2")
    val fromSingSrcApply = op("fromSingSrcApply")
    val fromSingSrcReadText = op("fromSingSrcReadText")
    val fromSingSrcReadCSV = op("fromSingSrcReadCSV")
    val fromDatabagWriteCSV = op("fromDatabagWriteCSV")

    val cross3 = op("cross3")

    override def ops = Set()

  }

  case class SkipTraversal()
  def skip(t: u.Tree): Unit = {
    meta(t).update(SkipTraversal)
  }

}

object DB {

  def singSrc[A: org.emmalanguage.api.Meta](l: () => A): org.emmalanguage.api.DataBag[A] = {
    org.emmalanguage.api.DataBag(Seq(l()))
  }

  def fromSingSrcApply[A: org.emmalanguage.api.Meta](db: org.emmalanguage.api.DataBag[Seq[A]]):
  org.emmalanguage.api.DataBag[A] = {
    org.emmalanguage.api.DataBag(db.collect().head)
  }

  def fromSingSrcReadText(db: org.emmalanguage.api.DataBag[String]):
  org.emmalanguage.api.DataBag[String] = {
    org.emmalanguage.api.DataBag.readText(db.collect().head)
  }

  def fromSingSrcReadCSV[A: Meta : CSVConverter](
    path: org.emmalanguage.api.DataBag[String],format: org.emmalanguage.api.DataBag[org.emmalanguage.api.CSV]
  ): org.emmalanguage.api.DataBag[A] = {
    org.emmalanguage.api.DataBag.readCSV[A](path.collect().head, format.collect().head)
  }

  def fromDatabagWriteCSV[A: org.emmalanguage.api.Meta](
    db: org.emmalanguage.api.DataBag[A],
    path: org.emmalanguage.api.DataBag[String],
    format: org.emmalanguage.api.DataBag[org.emmalanguage.api.CSV])(
    implicit converter: CSVConverter[A]
  ) : org.emmalanguage.api.DataBag[Unit] = {
    singSrc( () => db.writeCSV(path.collect()(0), format.collect()(0))(converter) )
  }

  // fold Alg
  def fold1[A: org.emmalanguage.api.Meta, B: org.emmalanguage.api.Meta]
  (db: org.emmalanguage.api.DataBag[A], alg: Alg[A,B])
  : org.emmalanguage.api.DataBag[B] = {
    org.emmalanguage.api.DataBag(Seq(db.fold[B](alg)))
  }

  // fold classic
  def fold2[A: org.emmalanguage.api.Meta, B: org.emmalanguage.api.Meta]
  ( db: org.emmalanguage.api.DataBag[A], zero: B, init: A => B, plus: (B,B) => B )
  : org.emmalanguage.api.DataBag[B] = {
    org.emmalanguage.api.DataBag(Seq(db.fold(zero)(init, plus)))
  }

  // fold2 from zero singSrc
  def fold2FromSingSrc[A: org.emmalanguage.api.Meta, B: org.emmalanguage.api.Meta]
  ( db: org.emmalanguage.api.DataBag[A], zero: org.emmalanguage.api.DataBag[B], init: A => B, plus: (B,B) => B )
  : org.emmalanguage.api.DataBag[B] = {
    org.emmalanguage.api.DataBag(Seq(db.fold(zero.collect().head)(init, plus)))
  }

  def collect[A: org.emmalanguage.api.Meta](db: org.emmalanguage.api.DataBag[A]) :
  org.emmalanguage.api.DataBag[Seq[A]] = {
    org.emmalanguage.api.DataBag(Seq(db.collect()))
  }

  def cross3[A: org.emmalanguage.api.Meta, B: org.emmalanguage.api.Meta, C: org.emmalanguage.api.Meta](
    xs: org.emmalanguage.api.DataBag[A], ys: org.emmalanguage.api.DataBag[B], zs: org.emmalanguage.api.DataBag[C]
  )(implicit env: org.emmalanguage.api.LocalEnv): org.emmalanguage.api.DataBag[(A, B, C)] = for {
    x <- xs
    y <- ys
    z <- zs
  } yield (x, y, z)

}

