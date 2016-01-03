package cakesolutions.model.provers

import cakesolutions.model.ModelGenerators
import cakesolutions.syntax.QueryLanguage
import com.typesafe.config.ConfigFactory
import org.scalatest._
import org.scalatest.prop._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Success

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
    forAll(QueryGen()) { (query: Query) =>
      assert(Future.fromTry(cvc4.valid(query)).isReadyWithin(delta))
    }
  }

  s"satisfiable(p) result computed within ${delta.toString()}" in {
    forAll(QueryGen()) { (query: Query) =>
      assert(Future.fromTry(cvc4.satisfiable(query)).isReadyWithin(delta))
    }
  }

  s"simplify(p) result computed within ${delta.toString()}" in {
    forAll(QueryGen()) { (query: Query) =>
      assert(Future.fromTry(cvc4.simplify(query)).isReadyWithin(delta))
    }
  }

  "valid(p --> p)" in {
    forAll(QueryGen()) { (query: Query) =>
      assert(cvc4.valid(Or(QueryLanguage.not(query), query)) == Success(true))
    }
  }

  "satisfiable(p --> p)" in {
    forAll(QueryGen()) { (query: Query) =>
      assert(cvc4.satisfiable(Or(QueryLanguage.not(query), query)) == Success(true))
    }
  }

  "valid(p & q --> p)" in {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(cvc4.valid(Or(QueryLanguage.not(And(query1, query2)), query1)) == Success(true))
    }
  }

  "satisfiable(p & q --> p)" in {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(cvc4.satisfiable(Or(QueryLanguage.not(And(query1, query2)), query1)) == Success(true))
    }
  }

  "valid(p & q --> q)" in {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(cvc4.valid(Or(QueryLanguage.not(And(query1, query2)), query2)) == Success(true))
    }
  }

  "satisfiable(p & q --> q)" in {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(cvc4.satisfiable(Or(QueryLanguage.not(And(query1, query2)), query2)) == Success(true))
    }
  }

  "valid(p --> p | q)" in {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(cvc4.valid(Or(QueryLanguage.not(query1), Or(query1, query2))) == Success(true))
    }
  }

  "satisfiable(p --> p | q)" in {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(cvc4.satisfiable(Or(QueryLanguage.not(query1), Or(query1, query2))) == Success(true))
    }
  }

  "valid(q --> p | q)" in {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(cvc4.valid(Or(QueryLanguage.not(query2), Or(query1, query2))) == Success(true))
    }
  }

  "satisfiable(q --> p | q)" in {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(cvc4.satisfiable(Or(QueryLanguage.not(query2), Or(query1, query2))) == Success(true))
    }
  }

  "valid(p & (p --> q) --> q)" in {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(cvc4.valid(Or(QueryLanguage.not(And(query1, Or(QueryLanguage.not(query1), query2))), query2)) == Success(true))
    }
  }

  "satisfiable(p & (p --> q) --> q)" in {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(cvc4.satisfiable(Or(QueryLanguage.not(And(query1, Or(QueryLanguage.not(query1), query2))), query2)) == Success(true))
    }
  }

  "valid(p <-> simplify(p))" in {
    forAll(QueryGen()) { (query: Query) =>
      for {
        simplifiedQuery <- cvc4.simplify(query)
        simplifiedNotQuery <- cvc4.simplify(QueryLanguage.not(query))
      } {
        assert(cvc4.valid(Or(QueryLanguage.not(query), simplifiedQuery)) == Success(true))
        assert(cvc4.valid(Or(query, simplifiedNotQuery)) == Success(true))
        assert(cvc4.valid(Or(query, QueryLanguage.not(simplifiedQuery))) == Success(true))
      }
    }
  }

}
