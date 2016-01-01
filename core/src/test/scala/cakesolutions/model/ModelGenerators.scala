package cakesolutions.model

import cakesolutions.syntax.QueryLanguage
import cakesolutions.syntax.QueryLanguage._
import org.scalacheck.Arbitrary._
import org.scalacheck.Gen
import org.scalacheck.Gen._
import QueryModel._

trait ModelGenerators {

  val defaultWidth = 15

  val GroundFactGen: Gen[GroundFact] = for {
    name <- Gen.identifier if !QueryLanguage.keywords.contains(name)
  } yield new GroundFact(name)

  val FactGen: Gen[Fact] = frequency(
    1 -> (for { fact <- GroundFactGen } yield fact),
    0 -> (for { fact <- GroundFactGen } yield Neg(fact))
  )

  def PropositionGen(width: Int = defaultWidth): Gen[Proposition] = frequency(
    1 -> Gen.oneOf(True, False),
    5 -> (for { fact <- Gen.lzy(FactGen) } yield Assert(fact)),
    1 -> (for {
      fact1 <- Gen.lzy(PropositionGen(width-1))
      fact2 <- Gen.lzy(PropositionGen(width-1))
      size <- Gen.lzy(Gen.oneOf(0 to width))
      facts <- Gen.lzy(Gen.listOfN(size, PropositionGen(width-1)))
    } yield Conjunction(fact1, fact2, facts: _*)),
    1 -> (for {
      fact1 <- Gen.lzy(PropositionGen(width-1))
      fact2 <- Gen.lzy(PropositionGen(width-1))
      size <- Gen.lzy(Gen.oneOf(0 to width))
      facts <- Gen.lzy(Gen.listOfN(size, PropositionGen(width-1)))
    } yield Disjunction(fact1, fact2, facts: _*))
  )

  def PathGen(width: Int = defaultWidth): Gen[Path] = frequency(
    5 -> Gen.lzy(PropositionGen(width-1)).map(AssertFact),
    5 -> Gen.lzy(QueryGen(width-1)).map(Test),
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

  def QueryGen(width: Int = defaultWidth): Gen[Query] = frequency(
    5 -> (for { fact <- Gen.lzy(PropositionGen(width-1)) } yield Formula(fact)),
    5 -> Gen.oneOf(TT, FF),
    1 -> (for {
      query1 <- Gen.lzy(QueryGen(width-1))
      query2 <- Gen.lzy(QueryGen(width-1))
      size <- Gen.lzy(Gen.oneOf(0 to width))
      queries <- Gen.lzy(Gen.listOfN(size, QueryGen(width-1)))
    } yield And(query1, query2, queries: _*)),
    1 -> (for {
      query1 <- Gen.lzy(QueryGen(width-1))
      query2 <- Gen.lzy(QueryGen(width-1))
      size <- Gen.lzy(Gen.oneOf(0 to width))
      queries <- Gen.lzy(Gen.listOfN(size, QueryGen(width-1)))
    } yield Or(query1, query2, queries: _*)),
    5 -> (for {
      path <- Gen.lzy(PathGen(width-1))
      query <- Gen.lzy(QueryGen(width-1))
    } yield Exists(path, query)),
    5 -> (for {
      path <- Gen.lzy(PathGen(width-1))
      query <- Gen.lzy(QueryGen(width-1))
    } yield All(path, query))
  )

  val QueryValueGen: Gen[Notification] = frequency(
    1 -> arbitrary[Boolean].map(StableValue),
    1 -> (for { query <- QueryGen() } yield UnstableValue(query))
  )

}
