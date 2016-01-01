package cakesolutions.model.provers

import cakesolutions.model.ModelGenerators
import cakesolutions.syntax.QueryLanguage
import com.typesafe.config.ConfigFactory
import org.scalatest._
import org.scalatest.prop._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

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

  s"valid(p) result computed within ${delta.toNanos} nanosecond" in {
    forAll(QueryGen()) { (query: Query) =>
      assert(z3.valid(query).isReadyWithin(delta))
    }
  }

  s"satisfiable(p) result computed within ${delta.toNanos} nanosecond" in {
    forAll(QueryGen()) { (query: Query) =>
      assert(z3.satisfiable(query).isReadyWithin(delta))
    }
  }

  "valid(p --> p)" in {
    forAll(QueryGen()) { (query: Query) =>
      assert(z3.valid(Or(QueryLanguage.not(query), query)).futureValue)
    }
  }

  "satisfiable(p --> p)" in {
    forAll(QueryGen()) { (query: Query) =>
      assert(z3.satisfiable(Or(QueryLanguage.not(query), query)).futureValue)
    }
  }

  "valid(p & q --> p)" in {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(z3.valid(Or(QueryLanguage.not(And(query1, query2)), query1)).futureValue)
    }
  }

  "satisfiable(p & q --> p)" in {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(z3.satisfiable(Or(QueryLanguage.not(And(query1, query2)), query1)).futureValue)
    }
  }

  "valid(p & q --> q)" in {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(z3.valid(Or(QueryLanguage.not(And(query1, query2)), query2)).futureValue)
    }
  }

  "satisfiable(p & q --> q)" in {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(z3.satisfiable(Or(QueryLanguage.not(And(query1, query2)), query2)).futureValue)
    }
  }

  "valid(p --> p | q)" in {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(z3.valid(Or(QueryLanguage.not(query1), Or(query1, query2))).futureValue)
    }
  }

  "satisfiable(p --> p | q)" in {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(z3.satisfiable(Or(QueryLanguage.not(query1), Or(query1, query2))).futureValue)
    }
  }

  "valid(q --> p | q)" in {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(z3.valid(Or(QueryLanguage.not(query2), Or(query1, query2))).futureValue)
    }
  }

  "satisfiable(q --> p | q)" in {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(z3.satisfiable(Or(QueryLanguage.not(query2), Or(query1, query2))).futureValue)
    }
  }

  "valid(p & (p --> q) --> q)" in {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(z3.valid(Or(QueryLanguage.not(And(query1, Or(QueryLanguage.not(query1), query2))), query2)).futureValue)
    }
  }

  "satisfiable(p & (p --> q) --> q)" in {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(z3.satisfiable(Or(QueryLanguage.not(And(query1, Or(QueryLanguage.not(query1), query2))), query2)).futureValue)
    }
  }

  "valid(p <-> simplify(p))" in {
    forAll(QueryGen()) { (query: Query) =>
      assert(z3.valid(Or(QueryLanguage.not(query), z3.simplify(query).futureValue)).futureValue)
      assert(z3.valid(Or(query, z3.simplify(QueryLanguage.not(query)).futureValue)).futureValue)
      assert(z3.valid(Or(query, QueryLanguage.not(z3.simplify(query).futureValue))).futureValue)
    }
  }

}
