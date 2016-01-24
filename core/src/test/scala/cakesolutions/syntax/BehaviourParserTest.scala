package cakesolutions.syntax

import org.scalatest.{concurrent, BeforeAndAfterAll, Matchers, FreeSpec}
import org.scalatest.prop.PropertyChecks

import cakesolutions.model.ModelGenerators

class BehaviourParserTest
  extends FreeSpec
  with PropertyChecks
  with Matchers
  with BeforeAndAfterAll
  with concurrent.ScalaFutures
  with ModelGenerators {

  "Location parsing" in {
    forAll(LocationGen()) { location =>
      val left = BehaviourParser.location(location.toString).toOption
      val right = BehaviourParser.location(location.toString).toOption.flatMap(l => BehaviourParser.location(l.toString).toOption)

      assert(left.isDefined)
      assert(right.isDefined)
      assert(left == right)
    }
  }

  "Role parsing" in {
    forAll(RoleGen()) { role =>
      val left = BehaviourParser.role(role.toString).toOption
      val right = BehaviourParser.role(role.toString).toOption.flatMap(r => BehaviourParser.role(r.toString).toOption)

      assert(left.isDefined)
      assert(right.isDefined)
      assert(left == right)
    }
  }

  "Path parsing" in {
    forAll(PathGen()) { path =>
      val left = BehaviourParser.path(path.toString).toOption
      val right = BehaviourParser.path(path.toString).toOption.flatMap(p => BehaviourParser.path(p.toString).toOption)

      assert(left.isDefined)
      assert(right.isDefined)
      assert(left == right)
    }
  }

  "Behaviour parsing" in {
    forAll(BehaviourGen()) { behaviour =>
      val left = BehaviourParser.behaviour(behaviour.toString).toOption
      val right = BehaviourParser.behaviour(behaviour.toString).toOption.flatMap(b => BehaviourParser.behaviour(b.toString).toOption)

      assert(left.isDefined)
      assert(right.isDefined)
      assert(left == right)
    }
  }

}
