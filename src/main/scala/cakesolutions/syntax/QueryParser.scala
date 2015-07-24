package cakesolutions.syntax

import org.parboiled2.CharPredicate._
import org.parboiled2._
import scala.language.implicitConversions

class QueryParser(val input: ParserInput) extends Parser with StringBuilding {

  def ws(s: String): Rule0 = rule {
    str(s) ~ zeroOrMore(anyOf(" \n\r\t"))
  }

  def GroundFact: Rule1[QueryLanguage.GroundFact] = rule {
    capture((Alpha | '_') ~ zeroOrMore(AlphaNum | '_')) ~> { (w: Any) => QueryLanguage.GroundFact(w.toString) }
  }

  def Fact: Rule1[QueryLanguage.Fact] = rule {
    GroundFact ~> { (f: QueryLanguage.GroundFact) => f } |
      ws("~") ~ GroundFact ~> { (f: QueryLanguage.GroundFact) => QueryLanguage.Neg(f) }
  }

  def Proposition: Rule1[QueryLanguage.Proposition] = rule {
    Fact ~> { (f: QueryLanguage.Fact) => QueryLanguage.Assert(f) } |
      ws("true") ~ push(QueryLanguage.True) |
      ws("false") ~ push(QueryLanguage.False) |
      ws("~") ~ Proposition ~> { (p: QueryLanguage.Proposition) => QueryLanguage.not(p) } |
      Proposition ~ ws("&&") ~ oneOrMore(Proposition).separatedBy(ws("&&")) ~> { (p: QueryLanguage.Proposition, ps: Seq[QueryLanguage.Proposition]) => QueryLanguage.Conjunction(p, ps.head, ps.tail: _*) } |
      Proposition ~ ws("||") ~ oneOrMore(Proposition).separatedBy(ws("||")) ~> { (p: QueryLanguage.Proposition, ps: Seq[QueryLanguage.Proposition]) => QueryLanguage.Disjunction(p, ps.head, ps.tail: _*) } |
      ws("(") ~ Proposition ~ ws(")") ~> { (p: QueryLanguage.Proposition) => p }
  }

  def Path: Rule1[QueryLanguage.Path] = rule {
    Proposition ~> { (p: QueryLanguage.Proposition) => QueryLanguage.AssertFact(p) } |
      Query ~ ws("?") ~> { (q: QueryLanguage.Query) => QueryLanguage.Test(q) } |
      Path ~ ws("+") ~ oneOrMore(Path).separatedBy(ws("+")) ~> { (p: QueryLanguage.Path, ps: Seq[QueryLanguage.Path]) => QueryLanguage.Choice(p, ps.head, ps.tail: _*) } |
      Path ~ ws(";") ~ oneOrMore(Path).separatedBy(ws(";")) ~> { (p: QueryLanguage.Path, ps: Seq[QueryLanguage.Path]) => QueryLanguage.Sequence(p, ps.head, ps.tail: _*) } |
      Path ~ ws("*") ~> { (p: QueryLanguage.Path) => QueryLanguage.Repeat(p) } |
      ws("(") ~ Path ~ ws(")") ~> { (p: QueryLanguage.Path) => p }
  }

  def Query: Rule1[QueryLanguage.Query] = rule {
    Proposition ~> { (p: QueryLanguage.Proposition) => QueryLanguage.Formula(p) } |
      ws("TT") ~ push(QueryLanguage.TT) |
      ws("FF") ~ push(QueryLanguage.FF) |
      ws("~") ~ Query ~> { (q: QueryLanguage.Query) => QueryLanguage.not(q) } |
      Query ~ ws("&&") ~ oneOrMore(Query).separatedBy(ws("&&")) ~> { (q: QueryLanguage.Query, qs: Seq[QueryLanguage.Query]) => QueryLanguage.And(q, qs.head, qs.tail: _*) } |
      Query ~ ws("||") ~ oneOrMore(Query).separatedBy(ws("||")) ~> { (q: QueryLanguage.Query, qs: Seq[QueryLanguage.Query]) => QueryLanguage.Or(q, qs.head, qs.tail: _*) } |
      ws("<") ~ Path ~ ws(">") ~ Query ~> { (p: QueryLanguage.Path, q: QueryLanguage.Query) => QueryLanguage.Exists(p, q) } |
      ws("[") ~ Path ~ ws("]") ~ Query ~> { (p: QueryLanguage.Path, q: QueryLanguage.Query) => QueryLanguage.All(p, q) } |
      ws("(") ~ Query ~ ws(")") ~> { (q: QueryLanguage.Query) => q }
  }

}
