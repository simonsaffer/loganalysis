package controllers

import java.net.URLDecoder

import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import processing.LogProcessor

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

object LogController extends Controller {

  private val UTF8 = "UTF-8"

  private var nextId = 1

  def upload(url: String) = Action {

    val decodedURL = URLDecoder.decode(url, UTF8)

    val processor = new LogProcessor

    val id = nextId
    Future { try {
      processor.doProcess(decodedURL, id)
    } catch {
      case e => e.printStackTrace()
    }}
    nextId += 1

    Ok(Json.toJson(id))
  }

}
