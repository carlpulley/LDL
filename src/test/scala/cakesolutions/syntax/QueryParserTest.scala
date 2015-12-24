package cakesolutions.syntax

import cakesolutions.model.ModelGenerators
import org.scalatest.prop.PropertyChecks
import org.scalatest.{BeforeAndAfterAll, Matchers, PropSpec, concurrent}

class QueryParserTest
  extends PropSpec
  with PropertyChecks
  with Matchers
  with BeforeAndAfterAll
  with concurrent.ScalaFutures
  with ModelGenerators {

  property("Proposition parsing") {
    forAll(PropositionGen()) { prop =>
      val left = QueryParser.proposition(prop.toString).toOption
      val right = QueryParser.proposition(prop.toString).toOption.flatMap(p => QueryParser.proposition(p.toString).toOption)

      assert(left.isDefined)
      assert(right.isDefined)
      assert(left == right)
    }
  }

  property("Path parsing") {
    forAll(PathGen()) { path =>
      val left = QueryParser.path(path.toString).toOption
      val right = QueryParser.path(path.toString).toOption.flatMap(p => QueryParser.path(p.toString).toOption)

      assert(left.isDefined)
      assert(right.isDefined)
      assert(left == right)
    }
  }

  property("Query parsing") {
    forAll(QueryGen()) { query =>
      val left = QueryParser.query(query.toString).toOption
      val right = QueryParser.query(query.toString).toOption.flatMap(q => QueryParser.query(q.toString).toOption)

      assert(left.isDefined)
      assert(right.isDefined)
      assert(left == right)
    }
  }

}
