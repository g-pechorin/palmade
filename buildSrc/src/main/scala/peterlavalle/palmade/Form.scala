package peterlavalle.palmade

object Form {

	sealed trait TFormat

	case object PROGRAM extends TFormat
	case object MODULE extends TFormat
	case object STATIC extends TFormat
	case object EXTERN extends TFormat
	case class REMOTE(url: String) extends TFormat


	def Module = MODULE
	def Static = STATIC
	def Program = PROGRAM
	def Extern = EXTERN
	def Remote(url: String) = REMOTE(url)

}
