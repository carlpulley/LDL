package cakesolutions.syntax

import scala.annotation.varargs

object QueryLanguage {

  /**
   * Observations record the state of, for example, sensor outputs.
   */
  type Observation = Set[Fact]

  val keywords = Seq("true", "false", "TT", "FF")

  /**
   * Namespaces.
   */
  val nameType = Some("name")
  val messageType = Some("message")
  val roleType = Some("role")

  /**
   * Facts that may hold of sensor data.
   */
  sealed trait Fact

  final case class Neg(fact: GroundFact) extends Fact {
    override def toString = s"(!$fact)"
  }

  /**
   * Ground facts logically model named ground predicates.
   */
  final case class GroundFact(name: String, namespace: Option[String] = None) extends Fact {
    require(!keywords.contains(name), s"$name is a reserved keyword")
    require(namespace.forall(_.matches("^[_a-zA-Z0-9:-]+$")), s"$namespace does not match '^[_a-zA-Z0-9:-]+$$'")

    override def toString = s"${namespace.getOrElse("")}::$name" //name
  }

  /**
   * We query exercise models using a DSL based upon a linear-time dynamic logic. Exercise sessions define the finite
   * length trace over which our queries will be evaluated. Paths are used to define (logical) windows over which queries
   * are to hold.
   *
   * NOTE:
   *   1. we intentionally have negation as being defined (rather than as a language primitive) - that way queries et al
   *      may be kept in negation normal form (NNF).
   *   2. exercise models are responsible for interpreting query language semantics in a meaningful way!
   *
   * REFERENCE:
   *   [2014] LTLf and LDLf Monitoring by Giuseppe De Giacomo, Riccardo De Masellis, Marco Grasso, Fabrizio Maria Maggi
   *   and Marco Montali
   */

  sealed trait Proposition

  case object True extends Proposition {
    override def toString = "true"
  }

  case object False extends Proposition {
    override def toString = "false"
  }

  final case class Assert(fact: Fact) extends Proposition {
    override def toString = fact.toString
  }

  @varargs
  final case class Conjunction(fact1: Proposition, fact2: Proposition, remainingFacts: Proposition*) extends Proposition {
    override def toString = (fact1, fact2) match {
      case (Assert(GroundFact(msg, `messageType`)), Assert(GroundFact(ref, `nameType`))) if remainingFacts.isEmpty =>
        s"($ref ! $msg)"

      case (Assert(Neg(GroundFact(msg, `messageType`))), Assert(Neg(GroundFact(ref, `nameType`)))) if remainingFacts.isEmpty =>
        s"($ref ? $msg)"

      case _ =>
        (fact1 +: fact2 +: remainingFacts).mkString("(", " && ", ")")
    }
  }

  @varargs
  final case class Disjunction(fact1: Proposition, fact2: Proposition, remainingFacts: Proposition*) extends Proposition {
    override def toString = (fact1, fact2) match {
      case (Assert(GroundFact(msg, `messageType`)), Assert(GroundFact(ref, `nameType`))) if remainingFacts.isEmpty =>
        s"($ref ! $msg)"

      case (Assert(Neg(GroundFact(msg, `messageType`))), Assert(Neg(GroundFact(ref, `nameType`)))) if remainingFacts.isEmpty =>
        s"($ref ? $msg)"

      case _ =>
        (fact1 +: fact2 +: remainingFacts).mkString("(", " || ", ")")
    }
  }

  def not(fact: Proposition): Proposition = fact match {
    case True =>
      False

    case False =>
      True

    case Assert(Neg(fact1)) =>
      Assert(fact1)

    case Assert(fact1: GroundFact) =>
      Assert(Neg(fact1))

    case Conjunction(fact1, fact2, remaining @ _*) =>
      Disjunction(not(fact1), not(fact2), remaining.map(not): _*)

    case Disjunction(fact1, fact2, remaining @ _*) =>
      Conjunction(not(fact1), not(fact2), remaining.map(not): _*)
  }

  /**
   * Path language - we encode path regular expressions here
   */
  sealed trait Path

  final case class AssertFact(fact: Proposition) extends Path {
    override def toString = fact.toString
  }

  final case class Assume(query: Query) extends Path {
    override def toString = s"if($query)"
  }

  @varargs
  final case class Choice(path1: Path, path2: Path, remaining: Path*) extends Path {
    override def toString = (path1 +: path2 +: remaining).mkString("(", " + ", ")")
  }

  @varargs
  final case class Sequence(path1: Path, path2: Path, remaining: Path*) extends Path {
    override def toString = (path1 +: path2 +: remaining).mkString("(", "; ", ")")
  }

  final case class Repeat(path: Path) extends Path {
    override def toString = s"($path *)"
  }

  /**
   * Auxillary function that determines if a path only involves combinations of `Test` expressions (used by standard
   * model).
   *
   * @param path path to be tested
   */
  def testOnly(path: Path): Boolean = path match {
    case AssertFact(_) =>
      false

    case Assume(_) =>
      true

    case Choice(path1, path2, remainingPaths @ _*) =>
      (path1 +: path2 +: remainingPaths).map(testOnly).fold(true) { case (x, y) => x && y }

    case Sequence(path1, path2, remainingPaths @ _*) =>
      (path1 +: path2 +: remainingPaths).map(testOnly).fold(true) { case (x, y) => x && y }

    case Repeat(path1) =>
      testOnly(path1)
  }

  /**
   * Query language - we encode linear-time dynamic logic here
   */
  sealed trait Query

  final case class Formula(fact: Proposition) extends Query {
    override def toString = fact.toString
  }

  case object TT extends Query {
    override def toString = "TT"
  }

  case object FF extends Query {
    override def toString = "FF"
  }

  @varargs
  final case class And(query1: Query, query2: Query, remainingQueries: Query*) extends Query {
    override def toString = (query1 +: query2 +: remainingQueries).mkString("(", " && ", ")")
  }

  @varargs
  final case class Or(query1: Query, query2: Query, remainingQueries: Query*) extends Query {
    override def toString = (query1 +: query2 +: remainingQueries).mkString("(", " || ", ")")
  }

  /**
   * Logical expressions that operate on path prefixes:
   *   - `Exists` asserts existence of a path prefix;
   *   - `All` asserts query for all path prefixes.
   *
   * @param path  path prefix at end of which query should hold
   * @param query query that is to hold at end of a path prefix
   */
  final case class Exists(path: Path, query: Query) extends Query {
    override def toString = s"(<$path> $query)"
  }

  final case class All(path: Path, query: Query) extends Query {
    override def toString = s"([$path] $query)"
  }

  /**
   * Convenience function that provides negation on queries, whilst keeping them in NNF. Translation is linear in the
   * size of the query.
   */
  def not(query: Query): Query = query match {
    case Formula(fact) =>
      Formula(not(fact))

    case TT =>
      FF

    case FF =>
      TT

    case And(query1, query2, remaining @ _*) =>
      Or(not(query1), not(query2), remaining.map(not): _*)

    case Or(query1, query2, remaining @ _*) =>
      And(not(query1), not(query2), remaining.map(not): _*)

    case Exists(path, query1) =>
      All(path, not(query1))

    case All(path, query1) =>
      Exists(path, not(query1))
  }

}
