package cakesolutions.syntax

import cakesolutions.model.ModelGenerators
import com.codecommit.gll.{LineNil, Success}
import org.scalatest.prop.PropertyChecks
import org.scalatest.{BeforeAndAfterAll, Matchers, PropSpec, concurrent}

class QueryParserTest
  extends PropSpec
  with PropertyChecks
  with Matchers
  with BeforeAndAfterAll
  with concurrent.ScalaFutures
  with ModelGenerators {

  property("GroundFact parsing") {
    forAll(GroundFactGen) { fact =>
      assert(QueryParser.GroundFact(fact.toString).headOption.contains(Success(fact, LineNil)))
    }
  }

  property("Proposition parsing") {
    forAll(PropositionGen()) { prop =>
      assert(QueryParser.Proposition(prop.toString).headOption.contains(Success(prop, LineNil)))
    }
  }

  property("Path parsing") {
    forAll(PathGen()) { path =>
      assert(QueryParser.Path(path.toString).headOption.contains(Success(path, LineNil)))
    }
  }

  property("Query parsing") {
    forAll(QueryGen()) { query =>
      assert(QueryParser.Query(query.toString).headOption.contains(Success(query, LineNil)))
    }
  }

}
