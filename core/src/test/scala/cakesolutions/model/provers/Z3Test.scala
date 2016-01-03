package cakesolutions.model.provers

import cakesolutions.model.ModelGenerators
import cakesolutions.syntax.QueryLanguage
import com.typesafe.config.ConfigFactory
import org.scalatest._
import org.scalatest.prop._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Success

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
    forAll(QueryGen()) { (query: Query) =>
      assert(Future.fromTry(z3.valid(query)).isReadyWithin(delta))
    }
  }

  s"satisfiable(p) result computed within ${delta.toString()}" in {
    forAll(QueryGen()) { (query: Query) =>
      assert(Future.fromTry(z3.satisfiable(query)).isReadyWithin(delta))
    }
  }

  s"simplify(p) result computed within ${delta.toString()}" in {
    forAll(QueryGen()) { (query: Query) =>
      assert(Future.fromTry(z3.simplify(query)).isReadyWithin(delta))
    }
  }

  "valid(p --> p)" in {
    forAll(QueryGen()) { (query: Query) =>
      assert(z3.valid(Or(QueryLanguage.not(query), query)) == Success(true))
    }
  }

  "satisfiable(p --> p)" in {
    forAll(QueryGen()) { (query: Query) =>
      assert(z3.satisfiable(Or(QueryLanguage.not(query), query)) == Success(true))
    }
  }

  "valid(p & q --> p)" in {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(z3.valid(Or(QueryLanguage.not(And(query1, query2)), query1)) == Success(true))
    }
  }

  "satisfiable(p & q --> p)" in {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(z3.satisfiable(Or(QueryLanguage.not(And(query1, query2)), query1)) == Success(true))
    }
  }

  "valid(p & q --> q)" in {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(z3.valid(Or(QueryLanguage.not(And(query1, query2)), query2)) == Success(true))
    }
  }

  "satisfiable(p & q --> q)" in {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(z3.satisfiable(Or(QueryLanguage.not(And(query1, query2)), query2)) == Success(true))
    }
  }

  "valid(p --> p | q)" in {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(z3.valid(Or(QueryLanguage.not(query1), Or(query1, query2))) == Success(true))
    }
  }

  "satisfiable(p --> p | q)" in {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(z3.satisfiable(Or(QueryLanguage.not(query1), Or(query1, query2))) == Success(true))
    }
  }

  "valid(q --> p | q)" in {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(z3.valid(Or(QueryLanguage.not(query2), Or(query1, query2))) == Success(true))
    }
  }

  "satisfiable(q --> p | q)" in {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(z3.satisfiable(Or(QueryLanguage.not(query2), Or(query1, query2))) == Success(true))
    }
  }

  "valid(p & (p --> q) --> q)" in {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(z3.valid(Or(QueryLanguage.not(And(query1, Or(QueryLanguage.not(query1), query2))), query2)) == Success(true))
    }
  }

  "satisfiable(p & (p --> q) --> q)" in {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(z3.satisfiable(Or(QueryLanguage.not(And(query1, Or(QueryLanguage.not(query1), query2))), query2)) == Success(true))
    }
  }

  "valid(p <-> simplify(p))" in {
    forAll(QueryGen()) { (query: Query) =>
      for {
        simplifiedQuery <- z3.simplify(query)
        simplifiedNotQuery <- z3.simplify(QueryLanguage.not(query))
      } {
        assert(z3.valid(Or(QueryLanguage.not(query), simplifiedQuery)) == Success(true))
        assert(z3.valid(Or(query, simplifiedNotQuery)) == Success(true))
        assert(z3.valid(Or(query, QueryLanguage.not(simplifiedQuery))) == Success(true))
      }
    }
  }

}
