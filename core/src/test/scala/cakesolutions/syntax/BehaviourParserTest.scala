package cakesolutions.syntax

import org.scalatest.FreeSpec
import org.scalatest.prop.PropertyChecks

import cakesolutions.model.ModelGenerators

class BehaviourParserTest
  extends FreeSpec
  with PropertyChecks
  with ModelGenerators {

  val staticBehaviours = Table(
    "Behaviour",
    "true",
    "false",
    "p",
    "! p",
    "p && q",
    "p || q",
    "~ p",
    "TT",
    "FF",
    "< true > p",
    "< false > p",
    "< _!Int > p",
    "< _?String > p",
    "< if q > p",
    "< _?String+ > p",
    "< _?String* > p",
    "< _!Int + _?String > p",
    "< _!Int ; _?String > p",
    "< p; (_?Int ; _!String)+ > q",
    "[ true ] p",
    "[ false ] p",
    "[ _!Int ] p",
    "[ _?String ] p",
    "[ if q ] p",
    "[ _?String+ ] p",
    "[ _?String* ] p",
    "[ _!Int + _?String ] p",
    "[ _!Int ; _?String ] p",
    "[ p; (_?Int ; _!String)+ ] q"
  )

  "Static behaviour parsing" in {
    forAll(staticBehaviours) { behaviour =>
      val left = BehaviourParser.behaviour(behaviour).toOption
      val right = left.flatMap(b => BehaviourParser.behaviour(b.toString).toOption)

      assert(left.isDefined)
      assert(right.isDefined)
      assert(left == right)
    }
  }

  "Location parsing" in {
    forAll(LocationGen()) { location =>
      val left = BehaviourParser.location(location.toString).toOption
      val right = left.flatMap(l => BehaviourParser.location(l.toString).toOption)

      assert(left.isDefined)
      assert(right.isDefined)
      assert(left == right)
    }
  }

  "Role parsing" in {
    forAll(RoleGen()) { role =>
      val left = BehaviourParser.role(role.toString).toOption
      val right = left.flatMap(r => BehaviourParser.role(r.toString).toOption)

      assert(left.isDefined)
      assert(right.isDefined)
      assert(left == right)
    }
  }

  "Path parsing" in {
    forAll(PathGen()) { path =>
      val left = BehaviourParser.path(path.toString).toOption
      val right = left.flatMap(p => BehaviourParser.path(p.toString).toOption)

      assert(left.isDefined)
      assert(right.isDefined)
      assert(left == right)
    }
  }

  "Behaviour parsing" in {
    forAll(BehaviourGen()) { behaviour =>
      val left = BehaviourParser.behaviour(behaviour.toString).toOption
      val right = left.flatMap(b => BehaviourParser.behaviour(b.toString).toOption)

      assert(left.isDefined)
      assert(right.isDefined)
      assert(left == right)
    }
  }

}
