/*
 * Copyright (c) 2013-14 Miles Sabin 
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

package shapeless

import scala.language.dynamics
import scala.language.experimental.macros

/** Encodes a coproduct type, such as a sealed family of case classes.
  *
  * Each constructor from the family gets an encoding in terms of nested Inr and Inl.
  *
  * Which constructor is encoded as Inl() and which as Inr(Inl()) is determined by lexical order
  * of the subclasses. This example illustrates the encoding:
  *
  * {{{
  * scala> sealed trait Animal
  * defined trait Animal
  *
  * scala> case class Cat(name: String, livesLeft: Int) extends Animal
  * defined class Cat
  *
  * scala> case class Dog(name: String, bonesBuried: Int) extends Animal
  * defined class Dog
  *
  * scala> case class Koala(name: String, leavesEaten: Int) extends Animal
  * defined class Koala
  *
  * scala> case class Sloth(name: String, daysToClimbDownFromCurrentTree: Int) extends Animal
  * defined class Sloth
  *
  * scala> val garfield = Cat("Garfield", 9)
  * garfield: Cat = Cat(Garfield,9)
  *
  * scala> val odie = Dog("Odie", 3)
  * odie: Dog = Dog(Odie,3)
  *
  * scala> val koala = Koala("foo", 10)
  * koala: Koala = Koala(foo,10)
  *
  * scala> val sloth = Sloth("bar", 2)
  * sloth: Sloth = Sloth(bar,2)
  *
  * scala> val genAnimal = Generic[Animal]
  * genAnimal: shapeless.Generic[Animal]{type Repr = Cat :+: Dog :+: Koala :+: Sloth} = ...
  *
  * scala> def showCoproduct(o: Any) : String = o match {
  *      | case Inl(a) => "Inl(" + showCoproduct(a) + ")"
  *      | case Inr(a) => "Inr(" + showCoproduct(a) + ")"
  *      | case a => a.toString
  *      | }
  * showCoproduct: (o: Any)String
  *
  * scala> showCoproduct(genAnimal.to(garfield))
  * res5: String = Inl(Cat(Garfield,9))
  *
  * scala> showCoproduct(genAnimal.to(odie))
  * res6: String = Inr(Inl(Dog(Odie,3)))
  *
  * scala> showCoproduct(genAnimal.to(koala))
  * res7: String = Inr(Inr(Inl(Koala(foo,10))))
  *
  * scala> showCoproduct(genAnimal.to(sloth))
  * res8: String = Inr(Inr(Inr(Inl(Sloth(bar,2)))))
  *
  * scala>
  * }}}
  */
sealed trait Coproduct

sealed trait :+:[+H, +T <: Coproduct] extends Coproduct

final case class Inl[+H, +T <: Coproduct](head : H) extends :+:[H, T] {
  override def toString = head.toString
}

final case class Inr[+H, +T <: Coproduct](tail : T) extends :+:[H, T] {
  override def toString = tail.toString
}

sealed trait CNil extends Coproduct

object Coproduct extends Dynamic {
  import ops.coproduct.Inject
  import syntax.CoproductOps

  class MkCoproduct[C <: Coproduct] {
    def apply[T](t: T)(implicit inj: Inject[C, T]): C = inj(t) 
  }
  
  def apply[C <: Coproduct] = new MkCoproduct[C]

  implicit def cpOps[C <: Coproduct](c: C) = new CoproductOps(c) 

  def unsafeMkCoproduct(length: Int, value: Any) =
    (0 until length).foldLeft[Coproduct](Inl(value))((accum, _) => Inr(accum))

  def unsafeGet(c: Coproduct): Any = c match {
    case Inl(h) => h
    case Inr(c) => unsafeGet(c)
  }


  /**
   * Allows to specify a `Coproduct` type with a syntax similar to `Record` and `Union`, as follows,
   *
   * {{{
   * type ISB = Coproduct.`Int, String, Boolean`.T
   * }}}
   *
   * Literal types are allowed, so that the following is valid,
   *
   * {{{
   * type ABC = Coproduct.`'a, 'b, 'c`.T
   * type TwoTrueStr = Coproduct.`2, true, "str"`.T
   * }}}
   */
  def selectDynamic(tpeSelector: String): Any = macro LabelledMacros.coproductTypeImpl
}
