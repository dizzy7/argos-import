package app

sealed abstract class ContextSystem(val name: String)

object ContextSystem {
  case object Direct extends ContextSystem("direct")
  case object Adwords extends ContextSystem("adwords")

  def apply(system: String): Option[ContextSystem] = system.toLowerCase match {
    case Direct.`name` => Some(Direct)
    case Adwords.`name` => Some(Adwords)
    case _ => None
  }
}
