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
package labyrinth.operators;

import labyrinth.BagOperatorOutputCollector
import api.DataBag
import labyrinth.util.SerializedBuffer

import java.util

object ScalaOps {

  def map[IN, OUT](f: IN => OUT): FlatMap[IN, OUT] = {

    new FlatMap[IN, OUT]() {
      override def pushInElement(e: IN, logicalInputId: Int): Unit = {
        super.pushInElement(e, logicalInputId)
        out.collectElement(f(e))
      }
    }
  }

  def flatMap[IN, OUT](f: (IN, BagOperatorOutputCollector[OUT]) => Unit): FlatMap[IN, OUT] = {

    new FlatMap[IN, OUT]() {
      override def pushInElement(e: IN, logicalInputId: Int): Unit = {
        super.pushInElement(e, logicalInputId)
        f(e, out)
      }
    }
  }

  def flatMapDataBagHelper[IN, OUT](f: IN => DataBag[OUT]): FlatMap[IN, OUT] = {

    val lbda = (e: IN, coll: BagOperatorOutputCollector[OUT]) =>
      for(elem <- f(e)) yield { coll.collectElement(elem) }

    new FlatMap[IN, OUT]() {
      override def pushInElement(e: IN, logicalInputId: Int): Unit = {
        super.pushInElement(e, logicalInputId)
        lbda(e, out)
      }
    }

  }

  def fromNothing[OUT](f: () => OUT ): BagOperator[org.emmalanguage.labyrinth.util.Nothing,OUT] = {

    new BagOperator[org.emmalanguage.labyrinth.util.Nothing,OUT]() {
      override def openOutBag() : Unit = {
        super.openOutBag()
        out.collectElement(f())
        out.closeBag()
      }
    }
  }

  def fromSingSrcApply[IN](): SingletonBagOperator[Seq[IN], IN] = {

    new SingletonBagOperator[Seq[IN], IN] {
      override def pushInElement(e: Seq[IN], logicalInputId: Int): Unit = {
        super.pushInElement(e, logicalInputId)
        e.foreach(x => out.collectElement(x))
      }
    }
  }

  def foldGroup[K,IN,OUT](keyExtractor: IN => K, i: IN => OUT, f: (OUT, OUT) => OUT): FoldGroup[K,IN,OUT] = {

    new FoldGroup[K, IN, OUT]() {

      override protected def keyExtr(e: IN): K = keyExtractor(e)

      override def openOutBag(): Unit = {
        super.openOutBag()
        hm = new util.HashMap[K, OUT]
      }

      override def pushInElement(e: IN, logicalInputId: Int): Unit = {
        super.pushInElement(e, logicalInputId)
        val key = keyExtr(e)
        val g = hm.get(key)
        if (g == null) {
          hm.put(key, i(e))
        } else {
          hm.replace(key, f(g, i(e)))
        }
      }

      override def closeInBag(inputId: Int): Unit = {
        super.closeInBag(inputId)

        import scala.collection.JavaConversions._

        for (e <- hm.entrySet) {
          out.collectElement(e.getValue)
        }
        hm = null
        out.closeBag()
      }
    }
  }

  def reduceGroup[K,A](keyExtractor: A => K, f: (A, A) => A): FoldGroup[K, A, A] = {
    foldGroup(keyExtractor, (x:A) => x, f)
  }

  def joinGeneric[IN, K](keyExtractor: IN => K): JoinGeneric[IN, K] = {
    new JoinGeneric[IN, K] {
      override protected def keyExtr(e: IN): K = keyExtractor(e)
    }
  }

  def cross[A,B]: BagOperator[Either[A, B], org.apache.flink.api.java.tuple.Tuple2[A, B]] =
    new Cross[A,B] {}

  def singletonBagOperator[IN, OUT](f: IN => OUT): SingletonBagOperator[IN, OUT] = {
    new SingletonBagOperator[IN, OUT] {
      override def pushInElement(e: IN, logicalInputId: Int): Unit = {
        super.pushInElement(e, logicalInputId)
        out.collectElement(f(e))
      }
    }
  }

  def union[T](): Union[T] = {
    new Union[T]
  }
}
