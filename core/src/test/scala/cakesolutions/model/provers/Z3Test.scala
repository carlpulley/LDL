package cakesolutions.model.provers

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Success

import com.typesafe.config.ConfigFactory
import org.scalatest._
import org.scalatest.prop._

import cakesolutions.model.ModelGenerators
import cakesolutions.syntax.QueryLanguage

class Z3Test
  extends FreeSpec
  with PropertyChecks
  with Matchers
  with concurrent.ScalaFutures
  with BeforeAndAfter
  with ModelGenerators {

  import QueryLanguage._

  val z3 = new Z3(ConfigFactory.load("prover.conf"))

  val delta = 1.nanosecond

  after {
    z3.reset()
  }

  s"valid(p) result computed within ${delta.toString()}" in {
    forAll(BehaviourGen()) { (behaviour: Query) =>
      assert(Future.fromTry(z3.valid(behaviour)).isReadyWithin(delta))
    }
  }

  s"satisfiable(p) result computed within ${delta.toString()}" in {
    forAll(BehaviourGen()) { (behaviour: Query) =>
      assert(Future.fromTry(z3.satisfiable(behaviour)).isReadyWithin(delta))
    }
  }

  s"simplify(p) result computed within ${delta.toString()}" in {
    forAll(BehaviourGen()) { (behaviour: Query) =>
      assert(Future.fromTry(z3.simplify(behaviour)).isReadyWithin(delta))
    }
  }

  "valid(p --> p)" in {
    forAll(BehaviourGen()) { (behaviour: Query) =>
      assert(z3.valid(Or(QueryLanguage.not(behaviour), behaviour)) == Success(true))
    }
  }

  "satisfiable(p --> p)" in {
    forAll(BehaviourGen()) { (behaviour: Query) =>
      assert(z3.satisfiable(Or(QueryLanguage.not(behaviour), behaviour)) == Success(true))
    }
  }

  "valid(p & q --> p)" in {
    forAll(BehaviourGen(), BehaviourGen()) { (behaviour1: Query, behaviour2: Query) =>
      assert(z3.valid(Or(QueryLanguage.not(And(behaviour1, behaviour2)), behaviour1)) == Success(true))
    }
  }

  "satisfiable(p & q --> p)" in {
    forAll(BehaviourGen(), BehaviourGen()) { (behaviour1: Query, behaviour2: Query) =>
      assert(z3.satisfiable(Or(QueryLanguage.not(And(behaviour1, behaviour2)), behaviour1)) == Success(true))
    }
  }

  "valid(p & q --> q)" in {
    forAll(BehaviourGen(), BehaviourGen()) { (behaviour1: Query, behaviour2: Query) =>
      assert(z3.valid(Or(QueryLanguage.not(And(behaviour1, behaviour2)), behaviour2)) == Success(true))
    }
  }

  "satisfiable(p & q --> q)" in {
    forAll(BehaviourGen(), BehaviourGen()) { (behaviour1: Query, behaviour2: Query) =>
      assert(z3.satisfiable(Or(QueryLanguage.not(And(behaviour1, behaviour2)), behaviour2)) == Success(true))
    }
  }

  "valid(p --> p | q)" in {
    forAll(BehaviourGen(), BehaviourGen()) { (behaviour1: Query, behaviour2: Query) =>
      assert(z3.valid(Or(QueryLanguage.not(behaviour1), Or(behaviour2, behaviour2))) == Success(true))
    }
  }

  "satisfiable(p --> p | q)" in {
    forAll(BehaviourGen(), BehaviourGen()) { (behaviour1: Query, behaviour2: Query) =>
      assert(z3.satisfiable(Or(QueryLanguage.not(behaviour1), Or(behaviour1, behaviour2))) == Success(true))
    }
  }

  "valid(q --> p | q)" in {
    forAll(BehaviourGen(), BehaviourGen()) { (behaviour1: Query, behaviour2: Query) =>
      assert(z3.valid(Or(QueryLanguage.not(behaviour2), Or(behaviour1, behaviour2))) == Success(true))
    }
  }

  "satisfiable(q --> p | q)" in {
    forAll(BehaviourGen(), BehaviourGen()) { (behaviour1: Query, behaviour2: Query) =>
      assert(z3.satisfiable(Or(QueryLanguage.not(behaviour2), Or(behaviour1, behaviour2))) == Success(true))
    }
  }

  "valid(p & (p --> q) --> q)" in {
    forAll(BehaviourGen(), BehaviourGen()) { (behaviour1: Query, behaviour2: Query) =>
      assert(z3.valid(Or(QueryLanguage.not(And(behaviour1, Or(QueryLanguage.not(behaviour1), behaviour2))), behaviour2)) == Success(true))
    }
  }

  "satisfiable(p & (p --> q) --> q)" in {
    forAll(BehaviourGen(), BehaviourGen()) { (behaviour1: Query, behaviour2: Query) =>
      assert(z3.satisfiable(Or(QueryLanguage.not(And(behaviour1, Or(QueryLanguage.not(behaviour1), behaviour2))), behaviour2)) == Success(true))
    }
  }

  "valid(p <-> simplify(p))" in {
    forAll(BehaviourGen()) { (behaviour: Query) =>
      for {
        simplifiedQuery <- z3.simplify(behaviour)
        simplifiedNotQuery <- z3.simplify(QueryLanguage.not(behaviour))
      } {
        assert(z3.valid(Or(QueryLanguage.not(behaviour), simplifiedQuery)) == Success(true))
        assert(z3.valid(Or(behaviour, simplifiedNotQuery)) == Success(true))
        assert(z3.valid(Or(behaviour, QueryLanguage.not(simplifiedQuery))) == Success(true))
      }
    }
  }

}
