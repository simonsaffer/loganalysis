package controllers

import model.User
import play.api.libs.json._
import play.api.mvc.{Action, Controller}
import processing.LogProcessor

import scala.util.Try

object UserController extends Controller {

  implicit val userWrites = new Writes[User] {
    override def writes(user: User): JsValue = {
      Json.obj(
        "name" -> user.userId
      )
    }
  }


  def uniqueUsers(logId: Int) = Action {
    Try(LogProcessor.uniqueUsers(logId)).map(users => Ok(Json.toJson(users))).getOrElse(NotFound)
  }


  def topUsers(logId: Int, n: Option[Int]) = Action {
    Try(LogProcessor.usersSortedByNumberOfSongsPlayed(logId))
      .map(users => Ok(Json.toJson(users take(n getOrElse users.size))))
      .getOrElse(NotFound)
  }
}
