package cakesolutions.model.provers

import cakesolutions.model.ModelGenerators
import cakesolutions.syntax.QueryLanguage
import com.typesafe.config.ConfigFactory
import org.scalatest._
import org.scalatest.prop._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class Z3Test
  extends PropSpec
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

  property(s"valid(p) result computed within ${delta.toNanos} nanosecond") {
    forAll(QueryGen()) { (query: Query) =>
      assert(z3.valid(query).isReadyWithin(delta))
    }
  }

  property(s"satisfiable(p) result computed within ${delta.toNanos} nanosecond") {
    forAll(QueryGen()) { (query: Query) =>
      assert(z3.satisfiable(query).isReadyWithin(delta))
    }
  }

  property("valid(p --> p)") {
    forAll(QueryGen()) { (query: Query) =>
      assert(z3.valid(Or(QueryLanguage.not(query), query)).futureValue)
    }
  }

  property("satisfiable(p --> p)") {
    forAll(QueryGen()) { (query: Query) =>
      assert(z3.satisfiable(Or(QueryLanguage.not(query), query)).futureValue)
    }
  }

  property("valid(p & q --> p)") {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(z3.valid(Or(QueryLanguage.not(And(query1, query2)), query1)).futureValue)
    }
  }

  property("satisfiable(p & q --> p)") {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(z3.satisfiable(Or(QueryLanguage.not(And(query1, query2)), query1)).futureValue)
    }
  }

  property("valid(p & q --> q)") {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(z3.valid(Or(QueryLanguage.not(And(query1, query2)), query2)).futureValue)
    }
  }

  property("satisfiable(p & q --> q)") {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(z3.satisfiable(Or(QueryLanguage.not(And(query1, query2)), query2)).futureValue)
    }
  }

  property("valid(p --> p | q)") {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(z3.valid(Or(QueryLanguage.not(query1), Or(query1, query2))).futureValue)
    }
  }

  property("satisfiable(p --> p | q)") {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(z3.satisfiable(Or(QueryLanguage.not(query1), Or(query1, query2))).futureValue)
    }
  }

  property("valid(q --> p | q)") {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(z3.valid(Or(QueryLanguage.not(query2), Or(query1, query2))).futureValue)
    }
  }

  property("satisfiable(q --> p | q)") {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(z3.satisfiable(Or(QueryLanguage.not(query2), Or(query1, query2))).futureValue)
    }
  }

  property("valid(p & (p --> q) --> q)") {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(z3.valid(Or(QueryLanguage.not(And(query1, Or(QueryLanguage.not(query1), query2))), query2)).futureValue)
    }
  }

  property("satisfiable(p & (p --> q) --> q)") {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(z3.satisfiable(Or(QueryLanguage.not(And(query1, Or(QueryLanguage.not(query1), query2))), query2)).futureValue)
    }
  }

  property("valid(p <-> simplify(p))") {
    forAll(QueryGen()) { (query: Query) =>
      assert(z3.valid(Or(QueryLanguage.not(query), z3.simplify(query).futureValue)).futureValue)
      assert(z3.valid(Or(query, z3.simplify(QueryLanguage.not(query)).futureValue)).futureValue)
      assert(z3.valid(Or(query, QueryLanguage.not(z3.simplify(query).futureValue))).futureValue)
    }
  }

}
