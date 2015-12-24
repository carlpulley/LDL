package cakesolutions.syntax

import cakesolutions.antlr4
import cakesolutions.syntax.QueryLanguage._
import org.antlr.v4.runtime.{ANTLRInputStream, CommonTokenStream}

import scala.collection.JavaConverters._
import scala.util.Try

object QueryParser {

  def proposition(proposition: String): Try[Proposition] = {
    Try(new QueryParserImpl().visitPropositionUnit(parser(proposition).propositionUnit()).asInstanceOf[Proposition])
  }

  def path(path: String): Try[Path] = {
    Try(new QueryParserImpl().visitPathUnit(parser(path).pathUnit()).asInstanceOf[Path])
  }

  def query(query: String): Try[Query] = {
    Try(new QueryParserImpl().visitQueryUnit(parser(query).queryUnit()).asInstanceOf[Query])
  }

  private def parser(expression: String): antlr4.QueryParser = {
    new antlr4.QueryParser(new CommonTokenStream(new antlr4.QueryLexer(new ANTLRInputStream(expression))))
  }

  private class QueryParserImpl extends antlr4.QueryBaseVisitor[Any] {
    override def visitPropositionUnit(ctx: antlr4.QueryParser.PropositionUnitContext) = {
      visit(ctx.proposition).asInstanceOf[Proposition]
    }

    override def visitParensProp(ctx: antlr4.QueryParser.ParensPropContext) = {
      visit(ctx.proposition).asInstanceOf[Proposition]
    }

    override def visitTrueProp(ctx: antlr4.QueryParser.TruePropContext) = {
      True
    }

    override def visitFalseProp(ctx: antlr4.QueryParser.FalsePropContext) = {
      False
    }

    override def visitIDProp(ctx: antlr4.QueryParser.IDPropContext) = {
      Assert(GroundFact(ctx.ID.getText))
    }

    override def visitNotIDProp(ctx: antlr4.QueryParser.NotIDPropContext) = {
      Assert(Neg(GroundFact(ctx.ID.getText)))
    }

    override def visitConjunctionProp(ctx: antlr4.QueryParser.ConjunctionPropContext) = {
      val args = ctx.proposition.asScala.map(arg => visit(arg).asInstanceOf[Proposition])
      assume(args.length >= 2)

      Conjunction(args.head, args(1), args.drop(2): _*)
    }

    override def visitDisjunctionProp(ctx: antlr4.QueryParser.DisjunctionPropContext) = {
      val args = ctx.proposition.asScala.map(arg => visit(arg).asInstanceOf[Proposition])
      assume(args.length >= 2)

      Disjunction(args.head, args(1), args.drop(2): _*)
    }

    override def visitDefnNotProp(ctx: antlr4.QueryParser.DefnNotPropContext) = {
      not(visit(ctx.proposition).asInstanceOf[Proposition])
    }

    override def visitPathUnit(ctx: antlr4.QueryParser.PathUnitContext) = {
      visit(ctx.path).asInstanceOf[Path]
    }

    override def visitParensPath(ctx: antlr4.QueryParser.ParensPathContext) = {
      visit(ctx.path).asInstanceOf[Path]
    }

    override def visitPropositionPath(ctx: antlr4.QueryParser.PropositionPathContext) = {
      AssertFact(visit(ctx.proposition).asInstanceOf[Proposition])
    }

    override def visitQueryPath(ctx: antlr4.QueryParser.QueryPathContext) = {
      Test(visit(ctx.query).asInstanceOf[Query])
    }

    override def visitRepeatPath(ctx: antlr4.QueryParser.RepeatPathContext) = {
      Repeat(visit(ctx.path).asInstanceOf[Path])
    }

    override def visitChoicePath(ctx: antlr4.QueryParser.ChoicePathContext) = {
      val args = ctx.path.asScala.map(arg => visit(arg).asInstanceOf[Path])
      assume(args.length >= 2)

      Choice(args.head, args(1), args.drop(2): _*)
    }

    override def visitSequencePath(ctx: antlr4.QueryParser.SequencePathContext) = {
      val args = ctx.path.asScala.map(arg => visit(arg).asInstanceOf[Path])
      assume(args.length >= 2)

      Sequence(args.head, args(1), args.drop(2): _*)
    }

    override def visitQueryUnit(ctx: antlr4.QueryParser.QueryUnitContext) = {
      visit(ctx.query).asInstanceOf[Query]
    }

    override def visitParensQuery(ctx: antlr4.QueryParser.ParensQueryContext) = {
      visit(ctx.query).asInstanceOf[Query]
    }

    override def visitPropositionQuery(ctx: antlr4.QueryParser.PropositionQueryContext) = {
      Formula(visit(ctx.proposition).asInstanceOf[Proposition])
    }

    override def visitTTQuery(ctx: antlr4.QueryParser.TTQueryContext) = {
      TT
    }

    override def visitFFQuery(ctx: antlr4.QueryParser.FFQueryContext) = {
      FF
    }

    override def visitAndQuery(ctx: antlr4.QueryParser.AndQueryContext) = {
      val args = ctx.query.asScala.map(arg => visit(arg).asInstanceOf[Query])
      assume(args.length >= 2)

      And(args.head, args(1), args.drop(2): _*)
    }

    override def visitOrQuery(ctx: antlr4.QueryParser.OrQueryContext) = {
      val args = ctx.query.asScala.map(arg => visit(arg).asInstanceOf[Query])
      assume(args.length >= 2)

      Or(args.head, args(1), args.drop(2): _*)
    }

    override def visitDefnNotQuery(ctx: antlr4.QueryParser.DefnNotQueryContext) = {
      not(visit(ctx.query).asInstanceOf[Query])
    }

    override def visitExistsQuery(ctx: antlr4.QueryParser.ExistsQueryContext) = {
      Exists(visit(ctx.path).asInstanceOf[Path], visit(ctx.query).asInstanceOf[Query])
    }

    override def visitAllQuery(ctx: antlr4.QueryParser.AllQueryContext) = {
      All(visit(ctx.path).asInstanceOf[Path], visit(ctx.query).asInstanceOf[Query])
    }
  }

}
