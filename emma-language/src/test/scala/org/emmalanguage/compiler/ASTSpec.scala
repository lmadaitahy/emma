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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ASTSpec extends BaseCompilerSpec {

  import compiler._
  object Bal

  "method calls should" - {
    "resolve overloaded symbols" in {
      val seq = api.Type.check(u.reify { Seq }.tree)
      val fill = api.Type[Seq.type].member(api.TermName("fill")).asTerm
      val examples = api.Type.check(u.reify {(
        Seq.fill(1)('!'),
        Seq.fill(1, 2)('!'),
        Seq.fill(1, 2, 3)('!')
      )}.tree).children.tail

      for ((dim, exp) <- Seq(Seq(1), Seq(1, 2), Seq(1, 2, 3)) zip examples) {
        val argss = Seq(dim.map(api.Lit(_)), Seq(api.Lit('!')))
        val act = api.DefCall(Some(seq))(fill, api.Type.char)(argss: _*)
        act shouldBe alphaEqTo (exp)
      }
    }
  }

  "static objects should" - {
    "be unqualified" in {
      val bar = u.rootMirror.staticModule("org.example.Foo.Bar")
      val ref = api.ModuleRef(bar)
      val qual = api.Tree.resolveStatic(bar)
      unQualifyStatics(qual) shouldBe alphaEqTo (ref)
      qualifyStatics(ref) shouldBe alphaEqTo (qual)
    }
  }

  "static classes should" - {
    "be unqualified" in {
      val bar = u.rootMirror.staticClass("java.lang.Object")
      val ref = api.Ref(bar)
      val qual = api.Tree.resolveStatic(bar)
      unQualifyStatics(qual) shouldBe alphaEqTo (ref)
      qualifyStatics(ref) shouldBe alphaEqTo (qual)
    }
  }

  "dynamic objects should" - {
    "remain qualified" in {
      val bal = api.Type.check(u.reify { Bal }.tree)
      val ref = api.TermRef(api.TermSym.of(bal))
      val act = unQualifyStatics(bal)
      act should not be alphaEqTo (ref)
    }
  }
}
