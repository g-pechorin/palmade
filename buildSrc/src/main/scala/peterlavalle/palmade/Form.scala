package peterlavalle.palmade

object Form {

	sealed trait TFormat

	case object PROGRAM extends TFormat

	case object MODULE extends TFormat

	case object STATIC extends TFormat

	case object EXTERN extends TFormat

	case class REMOTE(url: String)(md5: String) extends TFormat

}
