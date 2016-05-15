package processing

import java.time.temporal.ChronoUnit
import java.time.{ZoneId, Instant, ZonedDateTime}

import model.Song
import org.scalacheck.{Arbitrary, Gen, Prop, Properties}

class LogProcessorProps extends Properties("LogProcessor") {

  val songGen: Gen[Song] =
    for {
      trackId <- Gen.oneOf(None, Some("Track"));
      trackName <- Gen.alphaStr;
      artistId <- Gen.oneOf(None, Some("Artist"));
      artistName <- Gen.alphaStr
    } yield Song(trackId, trackName, artistId, artistName)

  def songLongTupleGen(timestampGen: Gen[Long]): Gen[(Song, Long)] =
    for {
      song <- songGen;
      frequency <- timestampGen
    } yield (song, frequency)

  property("splitIntoSessions shouldn't loose songs") =
    Prop.forAll(Gen.listOf(songLongTupleGen(Gen.chooseNum(0L, Int.MaxValue.toLong))))
    { (input: Seq[(Song, Long)]) =>
      LogProcessor.splitIntoSessions(input).map(_.size).sum == input.size
    }

  property("splitIntoSessions should create only one Seq if all are less than 20 minutes apart ") =
    Prop.forAll(Gen.listOf(songLongTupleGen(Gen.chooseNum(0L, 19*60L))))
    { (input: Seq[(Song, Long)]) =>
      LogProcessor.splitIntoSessions(input).size == 1
    }

  val zoneDateTimeGen: Gen[ZonedDateTime] = for {
    timestamp <- Arbitrary.arbitrary[Long]
  } yield ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())

  property("minutesBetween") = Prop.forAll(zoneDateTimeGen, zoneDateTimeGen) {(d1: ZonedDateTime, d2: ZonedDateTime) =>
    LogProcessor.minutesBetween(d1.toEpochSecond, d2.toEpochSecond) == ChronoUnit.MINUTES.between(d1, d2)
  }

}
