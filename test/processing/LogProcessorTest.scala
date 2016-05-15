package processing

import model.Song
import org.specs2.mutable.Specification

import scala.collection.mutable.ListBuffer

class LogProcessorTest extends Specification {

  "Log processor method specs" >> {

    val dummy = Song(Some(""), "", Some(""), "")

    "If ALL songs are within 20 minutes of eachother, they should all be in one single session" >> {
      val allInOneSession: Seq[(Song, Long)] = Seq((dummy, 0L), (dummy, 20*60L), (dummy, 40*60L))
      LogProcessor.splitIntoSessions(allInOneSession) must be equalTo(Seq(allInOneSession))
    }

    "If NO songs are within 20 minutes of eachother, they should all be in separate sessions" >> {
      val allInDifferentSessions: Seq[(Song, Long)] = Seq((dummy, 0L), (dummy, 21*60L), (dummy, 42*60L))
      LogProcessor.splitIntoSessions(allInDifferentSessions) must be equalTo(
          Seq(Seq((dummy, 0L)), ListBuffer((dummy, 21*60L)), ListBuffer((dummy, 42*60L)))
        )
    }

    "If SOME songs are within 20 minutes of eachother, those songs should be in the same sessions" >> {
      val mix: Seq[(Song, Long)] =
        Seq(
          (dummy, 0L),
          (dummy, 21*60L), (dummy, 41*60L), (dummy, 42*60L), (dummy, 43*60L),
          (dummy, 64*60L),
          (dummy, 85*60L), (dummy, 86*60L)
        )
      LogProcessor.splitIntoSessions(mix) must be equalTo(
        Seq(
          Seq((dummy, 0L)),
          Seq((dummy, 21*60L), (dummy, 41*60L), (dummy, 42*60L), (dummy, 43*60L)),
          Seq((dummy, 64*60L)),
          Seq((dummy, 85*60L), (dummy, 86*60L)))
        )
      val firstSongAlone: Seq[(Song, Long)] =
        Seq(
          (dummy, 0L),
          (dummy, 21*60L), (dummy, 41*60L), (dummy, 42*60L), (dummy, 43*60L)
        )
      LogProcessor.splitIntoSessions(firstSongAlone) must be equalTo(
        Seq(
          Seq((dummy, 0L)),
          Seq((dummy, 21*60L), (dummy, 41*60L), (dummy, 42*60L), (dummy, 43*60L)))
        )
      val lastSongAlone: Seq[(Song, Long)] =
        Seq(
          (dummy, 21*60L), (dummy, 41*60L), (dummy, 42*60L), (dummy, 43*60L),
          (dummy, 64*60L)
        )
      LogProcessor.splitIntoSessions(lastSongAlone) must be equalTo(
        Seq(
          Seq((dummy, 21*60L), (dummy, 41*60L), (dummy, 42*60L), (dummy, 43*60L)),
          Seq((dummy, 64*60L)))
        )
    }

  }

}
