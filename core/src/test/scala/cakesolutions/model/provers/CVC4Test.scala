package cakesolutions.model.provers

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Success

import com.typesafe.config.ConfigFactory
import org.scalatest._
import org.scalatest.prop._

import cakesolutions.model.ModelGenerators
import cakesolutions.syntax.QueryLanguage

class CVC4Test
  extends FreeSpec
  with PropertyChecks
  with Matchers
  with concurrent.ScalaFutures
  with BeforeAndAfter
  with ModelGenerators {

  import QueryLanguage._

  val cvc4 = new CVC4(ConfigFactory.load("prover.conf"))

  val delta = 1.nanosecond

  after {
    cvc4.reset()
  }

  s"valid(p) result computed within ${delta.toString()}" in {
    forAll(BehaviourGen()) { (behaviour: Query) =>
      assert(Future.fromTry(cvc4.valid(behaviour)).isReadyWithin(delta))
    }
  }

  s"satisfiable(p) result computed within ${delta.toString()}" in {
    forAll(BehaviourGen()) { (behaviour: Query) =>
      assert(Future.fromTry(cvc4.satisfiable(behaviour)).isReadyWithin(delta))
    }
  }

  s"simplify(p) result computed within ${delta.toString()}" in {
    forAll(BehaviourGen()) { (behaviour: Query) =>
      assert(Future.fromTry(cvc4.simplify(behaviour)).isReadyWithin(delta))
    }
  }

  "valid(p --> p)" in {
    forAll(BehaviourGen()) { (behaviour: Query) =>
      assert(cvc4.valid(Or(QueryLanguage.not(behaviour), behaviour)) == Success(true))
    }
  }

  "satisfiable(p --> p)" in {
    forAll(BehaviourGen()) { (behaviour: Query) =>
      assert(cvc4.satisfiable(Or(QueryLanguage.not(behaviour), behaviour)) == Success(true))
    }
  }

  "valid(p & q --> p)" in {
    forAll(BehaviourGen(), BehaviourGen()) { (behaviour1: Query, behaviour2: Query) =>
      assert(cvc4.valid(Or(QueryLanguage.not(And(behaviour1, behaviour2)), behaviour1)) == Success(true))
    }
  }

  "satisfiable(p & q --> p)" in {
    forAll(BehaviourGen(), BehaviourGen()) { (behaviour1: Query, behaviour2: Query) =>
      assert(cvc4.satisfiable(Or(QueryLanguage.not(And(behaviour1, behaviour2)), behaviour1)) == Success(true))
    }
  }

  "valid(p & q --> q)" in {
    forAll(BehaviourGen(), BehaviourGen()) { (behaviour1: Query, behaviour2: Query) =>
      assert(cvc4.valid(Or(QueryLanguage.not(And(behaviour1, behaviour2)), behaviour2)) == Success(true))
    }
  }

  "satisfiable(p & q --> q)" in {
    forAll(BehaviourGen(), BehaviourGen()) { (behaviour1: Query, behaviour2: Query) =>
      assert(cvc4.satisfiable(Or(QueryLanguage.not(And(behaviour1, behaviour2)), behaviour2)) == Success(true))
    }
  }

  "valid(p --> p | q)" in {
    forAll(BehaviourGen(), BehaviourGen()) { (behaviour1: Query, behaviour2: Query) =>
      assert(cvc4.valid(Or(QueryLanguage.not(behaviour1), Or(behaviour1, behaviour2))) == Success(true))
    }
  }

  "satisfiable(p --> p | q)" in {
    forAll(BehaviourGen(), BehaviourGen()) { (behaviour1: Query, behaviour2: Query) =>
      assert(cvc4.satisfiable(Or(QueryLanguage.not(behaviour1), Or(behaviour1, behaviour2))) == Success(true))
    }
  }

  "valid(q --> p | q)" in {
    forAll(BehaviourGen(), BehaviourGen()) { (behaviour1: Query, behaviour2: Query) =>
      assert(cvc4.valid(Or(QueryLanguage.not(behaviour2), Or(behaviour1, behaviour2))) == Success(true))
    }
  }

  "satisfiable(q --> p | q)" in {
    forAll(BehaviourGen(), BehaviourGen()) { (behaviour1: Query, behaviour2: Query) =>
      assert(cvc4.satisfiable(Or(QueryLanguage.not(behaviour2), Or(behaviour1, behaviour2))) == Success(true))
    }
  }

  "valid(p & (p --> q) --> q)" in {
    forAll(BehaviourGen(), BehaviourGen()) { (behaviour1: Query, behaviour2: Query) =>
      assert(cvc4.valid(Or(QueryLanguage.not(And(behaviour1, Or(QueryLanguage.not(behaviour1), behaviour2))), behaviour2)) == Success(true))
    }
  }

  "satisfiable(p & (p --> q) --> q)" in {
    forAll(BehaviourGen(), BehaviourGen()) { (behaviour1: Query, behaviour2: Query) =>
      assert(cvc4.satisfiable(Or(QueryLanguage.not(And(behaviour1, Or(QueryLanguage.not(behaviour1), behaviour2))), behaviour2)) == Success(true))
    }
  }

  "valid(p <-> simplify(p))" in {
    forAll(BehaviourGen()) { (behaviour: Query) =>
      for {
        simplifiedQuery <- cvc4.simplify(behaviour)
        simplifiedNotQuery <- cvc4.simplify(QueryLanguage.not(behaviour))
      } {
        assert(cvc4.valid(Or(QueryLanguage.not(behaviour), simplifiedQuery)) == Success(true))
        assert(cvc4.valid(Or(behaviour, simplifiedNotQuery)) == Success(true))
        assert(cvc4.valid(Or(behaviour, QueryLanguage.not(simplifiedQuery))) == Success(true))
      }
    }
  }

}
