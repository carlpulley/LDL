package cakesolutions.monitor.annotation

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.whitebox

private object Behaviour {

  def impl(c: whitebox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    val result = {
      annottees.map(_.tree).toList match {
        case q"$mods class $name[..$tparams](..$first)(...$rest) extends ..$parents { $self => ..$body }" :: tail =>
          val Literal(Constant(targetType: Type)) = c.typecheck(target)

          q"""
            $mods class $name[..$tparams](..$first)(...$rest) extends ..$parents {
              $self =>

              ..$body
            }
           """
      }
    }

    c.Expr[Any](result)
  }

}

class Behaviour(query: String) extends StaticAnnotation {

  def macroTransform(annottees: Any*): Any = macro Behaviour.impl

}
