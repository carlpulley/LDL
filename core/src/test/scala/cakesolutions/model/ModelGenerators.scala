package cakesolutions.model

import org.scalacheck.Arbitrary._
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Gen._

import cakesolutions.model.QueryModel._
import cakesolutions.syntax.QueryLanguage
import cakesolutions.syntax.QueryLanguage._

trait ModelGenerators {

  val defaultWidth = 15

  def GroundFactGen(prefix: Option[String] = None): Gen[GroundFact] = for {
    name <- Gen.identifier if !QueryLanguage.keywords.contains(name)
  } yield new GroundFact(name, prefix)

  def FactGen(prefix: Option[String] = None): Gen[Fact] = frequency(
    1 -> (for { fact <- GroundFactGen(prefix) } yield fact),
    0 -> (for { fact <- GroundFactGen(prefix) } yield Neg(fact))
  )

  def NameGen(width: Int = defaultWidth): Gen[String] = frequency(
    1 -> Gen.identifier.filter(!QueryLanguage.keywords.contains(_)),
    1 -> Gen.oneOf("..", "*")
  )

  // TODO: flesh this out!
  def LocationGen(width: Int = defaultWidth): Gen[Proposition] = frequency(
    1 -> GroundFactGen(Some("name")).map(Assert)
  )

  def MessageGen(width: Int = defaultWidth): Gen[Proposition] = frequency(
    1 -> GroundFactGen(Some("message")).map(Assert)
  )

  def RoleGen(width: Int = defaultWidth): Gen[Proposition] = frequency(
    1 -> Gen.oneOf(True, False),
    5 -> (for { fact <- Gen.lzy(FactGen(Some("role"))) } yield Assert(fact)),
    1 -> (for {
      fact1 <- Gen.lzy(RoleGen(width-1))
      fact2 <- Gen.lzy(RoleGen(width-1))
      size <- Gen.lzy(Gen.oneOf(0 to width))
      facts <- Gen.lzy(Gen.listOfN(size, RoleGen(width-1)))
    } yield Conjunction(fact1, fact2, facts: _*)),
    1 -> (for {
      fact1 <- Gen.lzy(RoleGen(width-1))
      fact2 <- Gen.lzy(RoleGen(width-1))
      size <- Gen.lzy(Gen.oneOf(0 to width))
      facts <- Gen.lzy(Gen.listOfN(size, RoleGen(width-1)))
    } yield Disjunction(fact1, fact2, facts: _*))
  )

  def PathGen(width: Int = defaultWidth): Gen[Path] = frequency(
    5 -> (for {
      ref <- Gen.lzy(LocationGen(width-1))
      msg <- Gen.lzy(MessageGen(width-1))
      tellOrReceive <- Gen.lzy(Arbitrary.arbitrary[Boolean])
      result = if (tellOrReceive) {
        AssertFact(Conjunction(msg, ref))
      } else {
        AssertFact(not(Conjunction(msg, ref)))
      }
    } yield result),
    5 -> Gen.lzy(BehaviourGen(width-1)).map(Assume),
    1 -> (for {
      path1 <- Gen.lzy(PathGen(width-1))
      path2 <- Gen.lzy(PathGen(width-1))
      size <- Gen.lzy(Gen.oneOf(0 to width))
      paths <- Gen.lzy(Gen.listOfN(size, PathGen(width-1)))
    } yield Choice(path1, path2, paths: _*)),
    1 -> (for {
      path1 <- Gen.lzy(PathGen(width-1))
      path2 <- Gen.lzy(PathGen(width-1))
      size <- Gen.lzy(Gen.oneOf(0 to width))
      paths <- Gen.lzy(Gen.listOfN(size, PathGen(width-1)))
    } yield Sequence(path1, path2, paths: _*)),
    5 -> Gen.lzy(PathGen(width-1)).map(Repeat)
  )

  def BehaviourGen(width: Int = defaultWidth): Gen[Query] = frequency(
    5 -> (for { fact <- Gen.lzy(RoleGen(width-1)) } yield Formula(fact)),
    5 -> Gen.oneOf(TT, FF),
    1 -> (for {
      behaviour1 <- Gen.lzy(BehaviourGen(width-1))
      behaviour2 <- Gen.lzy(BehaviourGen(width-1))
      size <- Gen.lzy(Gen.oneOf(0 to width))
      behaviours <- Gen.lzy(Gen.listOfN(size, BehaviourGen(width-1)))
    } yield And(behaviour1, behaviour2, behaviours: _*)),
    1 -> (for {
      behaviour1 <- Gen.lzy(BehaviourGen(width-1))
      behaviour2 <- Gen.lzy(BehaviourGen(width-1))
      size <- Gen.lzy(Gen.oneOf(0 to width))
      behaviours <- Gen.lzy(Gen.listOfN(size, BehaviourGen(width-1)))
    } yield Or(behaviour1, behaviour2, behaviours: _*)),
    5 -> (for {
      path <- Gen.lzy(PathGen(width-1))
      behaviour <- Gen.lzy(BehaviourGen(width-1))
    } yield Exists(path, behaviour)),
    5 -> (for {
      path <- Gen.lzy(PathGen(width-1))
      behaviour <- Gen.lzy(BehaviourGen(width-1))
    } yield All(path, behaviour))
  )

  // FIXME: is this needed?
  val BehaviourValueGen: Gen[Notification] = frequency(
    1 -> arbitrary[Boolean].map(StableValue),
    1 -> (for { behaviour <- Gen.lzy(Gen.option(BehaviourGen())) } yield UnstableValue(behaviour))
  )

}
