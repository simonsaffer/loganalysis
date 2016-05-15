package controllers

import model.Session
import play.api.libs.json._
import play.api.mvc.{Action, Controller}
import processing.LogProcessor

import scala.util.Try

object SessionController extends Controller {

  implicit val userWrites = UserController.userWrites
  implicit val songWrites = SongController.songWrites

  implicit val sessionWrites = new Writes[Session] {
    override def writes(session: Session): JsValue = {
      Json.obj(
        "duration" -> session.duration.toString,
        "user" -> Json.toJson(session.user),
        "songs" -> Json.toJson(session.songs)
      )
    }
  }

  def longestSessions(logId: Int, n: Option[Int]) = Action {
    Try(LogProcessor.sessionsSortedByDuration(logId))
      .map(array => Ok(Json.toJson(array take(n getOrElse array.size))))
      .getOrElse(NotFound)
  }

}
