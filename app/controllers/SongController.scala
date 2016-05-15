package controllers

import model.Song
import play.api.libs.json._
import play.api.mvc.{Action, Controller}
import processing.LogProcessor

import scala.util.Try

object SongController extends Controller {

  implicit val songAndFrequencyWrites = new Writes[(Song, Int)] {
    override def writes(songAndFreq: (Song, Int)): JsValue = {
      Json.obj(
        "name" -> songAndFreq._1.artistName,
        "album" -> songAndFreq._1.trackName,
        "nbrOfTimesPlayed" -> songAndFreq._2
      )
    }
  }

  implicit val songWrites = new Writes[Song] {
    override def writes(song: Song): JsValue = {
      Json.obj(
        "name" -> song.artistName,
        "album" -> song.trackName
      )
    }
  }

  def mostFrequent(logId: Int, n: Option[Int]) = Action {
    Try(LogProcessor.songFrequency(logId))
      .map(songs => Ok(Json.toJson(songs take(n getOrElse songs.size))))
      .getOrElse(NotFound)
  }

}
