package cakesolutions.syntax

import scala.collection.JavaConverters._
import scala.util.Try

import org.antlr.v4.runtime.{ANTLRInputStream, CommonTokenStream}

import cakesolutions.antlr4
import cakesolutions.syntax.QueryLanguage._

object BehaviourParser {

  def location(location: String): Try[Proposition] = {
    Try(new BehaviourParserImpl().visitLocationUnit(parser(location).locationUnit()).asInstanceOf[Proposition])
  }

  def role(role: String): Try[Proposition] = {
    Try(new BehaviourParserImpl().visitRoleUnit(parser(role).roleUnit()).asInstanceOf[Proposition])
  }

  def path(path: String): Try[Path] = {
    Try(new BehaviourParserImpl().visitPathUnit(parser(path).pathUnit()).asInstanceOf[Path])
  }

  def behaviour(behaviour: String): Try[Query] = {
    Try(new BehaviourParserImpl().visitBehaviourUnit(parser(behaviour).behaviourUnit()).asInstanceOf[Query])
  }

  private def parser(expression: String): antlr4.BehaviourParser = {
    new antlr4.BehaviourParser(new CommonTokenStream(new antlr4.BehaviourLexer(new ANTLRInputStream(expression))))
  }

  private class BehaviourParserImpl extends antlr4.BehaviourBaseVisitor[Any] {

    override def visitIDName(ctx: antlr4.BehaviourParser.IDNameContext) = {
      s"name$$${ctx.ID.getText}"
    }

    override def visitParentName(ctx: antlr4.BehaviourParser.ParentNameContext) = {
      ".."
    }

    override def visitAnyName(ctx: antlr4.BehaviourParser.AnyNameContext) = {
      "*"
    }

    override def visitLocationUnit(ctx: antlr4.BehaviourParser.LocationUnitContext) = {
      visit(ctx.location).asInstanceOf[Proposition]
    }

    override def visitFixedLocation(ctx: antlr4.BehaviourParser.FixedLocationContext) = {
      Assert(GroundFact(s"name$$${ctx.ID.getText}"))
    }

// TODO:
//    override def visitAbsoluteLocation(ctx: antlr4.BehaviourParser.AbsoluteLocationContext) = {
//      return visitChildren(ctx);
//    }
//
//    override def visitRelativeLocation(ctx: antlr4.BehaviourParser.RelativeLocationContext) = {
//      return visitChildren(ctx);
//    }

    override def visitParensMessage(ctx: antlr4.BehaviourParser.ParensMessageContext) = {
      visit(ctx.message).asInstanceOf[Proposition]
    }

    override def visitIDMessage(ctx: antlr4.BehaviourParser.IDMessageContext) = {
      Assert(GroundFact(s"message$$${ctx.ID.getText}"))
    }

    override def visitRoleUnit(ctx: antlr4.BehaviourParser.RoleUnitContext) = {
      visit(ctx.role).asInstanceOf[Proposition]
    }

    override def visitParensRole(ctx: antlr4.BehaviourParser.ParensRoleContext) = {
      visit(ctx.role).asInstanceOf[Proposition]
    }

    override def visitTrueRole(ctx: antlr4.BehaviourParser.TrueRoleContext) = {
      True
    }

    override def visitNotIDRole(ctx: antlr4.BehaviourParser.NotIDRoleContext) = {
      Assert(Neg(GroundFact(s"role$$${ctx.ID.getText}")))
    }

    override def visitDisjunctionRole(ctx: antlr4.BehaviourParser.DisjunctionRoleContext) = {
      val args = ctx.role.asScala.map(arg => visit(arg).asInstanceOf[Proposition])
      assume(args.length >= 2)

      Disjunction(args.head, args(1), args.drop(2): _*)
    }

    override def visitFalseRole(ctx: antlr4.BehaviourParser.FalseRoleContext) = {
      False
    }

    override def visitIDRole(ctx: antlr4.BehaviourParser.IDRoleContext) = {
      Assert(GroundFact(s"role$$${ctx.ID.getText}"))
    }

    override def visitDefnNotRole(ctx: antlr4.BehaviourParser.DefnNotRoleContext) = {
      not(visit(ctx.role).asInstanceOf[Proposition])
    }

    override def visitConjunctionRole(ctx: antlr4.BehaviourParser.ConjunctionRoleContext) = {
      val args = ctx.role.asScala.map(arg => visit(arg).asInstanceOf[Proposition])
      assume(args.length >= 2)

      Conjunction(args.head, args(1), args.drop(2): _*)
    }

    override def visitPathUnit(ctx: antlr4.BehaviourParser.PathUnitContext) = {
      visit(ctx.path).asInstanceOf[Path]
    }

    override def visitParensPath(ctx: antlr4.BehaviourParser.ParensPathContext) = {
      visit(ctx.path).asInstanceOf[Path]
    }

    override def visitBehaviourPath(ctx: antlr4.BehaviourParser.BehaviourPathContext) = {
      Test(visit(ctx.behaviour).asInstanceOf[Query])
    }

    override def visitReceiveEvent(ctx: antlr4.BehaviourParser.ReceiveEventContext) = {
      val msg = visit(ctx.message).asInstanceOf[Proposition]
      val ref = visit(ctx.location).asInstanceOf[Proposition]

      not(Conjunction(msg, ref))
    }

    override def visitZeroOrMorePath(ctx: antlr4.BehaviourParser.ZeroOrMorePathContext) = {
      Repeat(visit(ctx.path).asInstanceOf[Path])
    }

    override def visitSequencePath(ctx: antlr4.BehaviourParser.SequencePathContext) = {
      val args = ctx.path.asScala.map(arg => visit(arg).asInstanceOf[Path])
      assume(args.length >= 2)

      Sequence(args.head, args(1), args.drop(2): _*)
    }

    override def visitChoicePath(ctx: antlr4.BehaviourParser.ChoicePathContext) = {
      val args = ctx.path.asScala.map(arg => visit(arg).asInstanceOf[Path])
      assume(args.length >= 2)

      Choice(args.head, args(1), args.drop(2): _*)
    }

    override def visitSendEvent(ctx: antlr4.BehaviourParser.SendEventContext) = {
      val msg = visit(ctx.message).asInstanceOf[Proposition]
      val ref = visit(ctx.location).asInstanceOf[Proposition]

      Conjunction(msg, ref)
    }

    override def visitOneOrMorePath(ctx: antlr4.BehaviourParser.OneOrMorePathContext) = {
      val path = visit(ctx.path).asInstanceOf[Path]

      Sequence(path, Repeat(path))
    }

    override def visitBehaviourUnit(ctx: antlr4.BehaviourParser.BehaviourUnitContext) = {
      visit(ctx.behaviour).asInstanceOf[Query]
    }

    override def visitTTBehaviour(ctx: antlr4.BehaviourParser.TTBehaviourContext) = {
      TT
    }

    override def visitParensBehaviour(ctx: antlr4.BehaviourParser.ParensBehaviourContext) = {
      visit(ctx.behaviour).asInstanceOf[Query]
    }

    override def visitDefnNotBehaviour(ctx: antlr4.BehaviourParser.DefnNotBehaviourContext) = {
      not(visit(ctx.behaviour).asInstanceOf[Query])
    }

    override def visitRoleQuery(ctx: antlr4.BehaviourParser.RoleQueryContext) = {
      Formula(visit(ctx.role).asInstanceOf[Proposition])
    }

    override def visitAllBehaviour(ctx: antlr4.BehaviourParser.AllBehaviourContext) = {
      All(visit(ctx.path).asInstanceOf[Path], visit(ctx.behaviour).asInstanceOf[Query])
    }

    override def visitFFBehaviour(ctx: antlr4.BehaviourParser.FFBehaviourContext) = {
      FF
    }

    override def visitOrBehaviour(ctx: antlr4.BehaviourParser.OrBehaviourContext) = {
      val args = ctx.behaviour.asScala.map(arg => visit(arg).asInstanceOf[Query])
      assume(args.length >= 2)

      Or(args.head, args(1), args.drop(2): _*)
    }

    override def visitExistsBehaviour(ctx: antlr4.BehaviourParser.ExistsBehaviourContext) = {
      Exists(visit(ctx.path).asInstanceOf[Path], visit(ctx.behaviour).asInstanceOf[Query])
    }

    override def visitAndBehaviour(ctx: antlr4.BehaviourParser.AndBehaviourContext) = {
      val args = ctx.behaviour.asScala.map(arg => visit(arg).asInstanceOf[Query])
      assume(args.length >= 2)

      And(args.head, args(1), args.drop(2): _*)
    }

  }

}
