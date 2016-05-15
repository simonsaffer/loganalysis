package model

@SerialVersionUID(10L)
case class Session(duration: Long, songs: Seq[Song], user: User) extends Serializable {

}
