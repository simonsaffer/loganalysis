package processing

import java.time.temporal.ChronoUnit
import java.time.{Instant, Duration, ZonedDateTime}

import model._
import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.sql.{Dataset, GroupedDataset, Row, SQLContext}
import org.apache.spark.sql.types.{StringType, StructField, StructType, TimestampType}
import play.api.Logger

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

@SerialVersionUID(100L)
class LogProcessor extends Serializable {

  def getSession(songsAndTimestampsInSession: Seq[(Song, Long)], user: User): Session = {
    val duration = LogProcessor.minutesBetween(songsAndTimestampsInSession.head._2, songsAndTimestampsInSession.last._2)
    Session(duration, songsAndTimestampsInSession.map(_._1), user)
  }

  def doProcess(filePath: String, logId: Int) = {

    import LogProcessor.sqlContext.implicits._

    val logDataset = LogProcessor.sqlContext.read
      .format("com.databricks.spark.csv")
      .option("header", "false")
      .option("delimiter", "\t")
      .option("quote", null)
      .schema(LogProcessor.logSchema)
      .load(filePath)
      .as[RawLogEntry] // Use Dataset API

    // Transform data to our domain model objects
    val logEntries = logDataset
      .map(rawLogEntry => {
        val song = Song(Option(rawLogEntry.trackId), rawLogEntry.trackName, Option(rawLogEntry.artistId), rawLogEntry.artistName)
        LogEntry(User(rawLogEntry.userId), ZonedDateTime.parse(rawLogEntry.timeStamp).toEpochSecond, song)
      }).toDF().sort("timeStamp").as[LogEntry]

    val logEntriesPerUser = logEntries groupBy (_.user)
    // List of n top users and the number of distinct songs played by each of them, sorted in decreasing order by the number of
    // distinct songs (i.e., the user with the highest number of distinct songs listened appearing at the top of the list).
    val usersAndUniqueSongs = logEntriesPerUser mapGroups {
      case (user, logEntryIterator) =>
        val songs = logEntryIterator.map(_.song).toStream.distinct.size
        (user, songs)
    }

    val usersSortedByNumberOfSongsPlayed =
      usersAndUniqueSongs.rdd.sortBy(_._2).map(_._1).saveAsObjectFile(s"/tmp/usersSortedByNumberOfSongsPlayed/$logId")

    // List of all unique users.
    val uniqueUsers = logEntries.map(_.user).distinct.rdd.saveAsObjectFile(s"/tmp/uniqueUsers/$logId")

    // List of n top most frequently listened songs and the number of times each of them was played,
    // sorted in decreasing order by the number of times a song was played.
    val songFrequency = logEntries
      .groupBy(_.song)
      .mapGroups {
        case (song, logEntryIterator) => (song, logEntryIterator.size)
      }
    songFrequency.rdd.sortBy(_._2).map(_._1).saveAsObjectFile(s"/tmp/songFrequency/$logId")

    // List of n top longest listening sessions, with information on their duration, the user, and the songs listened, sorted
    // decreasingly by session length.
    val sessions = logEntriesPerUser flatMapGroups {
      case (user, logEntryIterator) => {
        val songsWithTimestamps = logEntryIterator map(entry => (entry.song, entry.timeStamp))
        LogProcessor.splitIntoSessions(songsWithTimestamps) map(session => getSession(session, user))
      }
    }

    val sessionsSortedByDuration = sessions.rdd.sortBy(_.duration).saveAsObjectFile(s"/tmp/sessionsSortedByDuration/$logId")

    // Given a user ID, predict the next time the user will be listening to any content.)
    // Given a user ID, predict the next song(s) the user will be listening to.
    // Given a user ID, recommend songs (or artists) that the user has not listened to yet, but might want to.
  }

}

object LogProcessor {

  private val logSchema = StructType(Array(
    StructField("userId", StringType, false),
    StructField("timeStamp", StringType, false),
    StructField("artistId", StringType, true),
    StructField("artistName", StringType, false),
    StructField("trackId", StringType, true),
    StructField("trackName", StringType, false)
  ))

  private val conf = new SparkConf()
    .setAppName("LastFM logs processor")
    .setMaster("local[4]")
    .set("spark.driver.memory", "3g")
    .set("spark.executor.memory", "4g")
    .set("spark.driver.maxResultSize", "3g")
  private val sc = new SparkContext(conf)
  private val sqlContext = new SQLContext(sc)

  def minutesBetween(timeEpochSeconds1: Long, timeEpochSeconds2: Long) = {
    ChronoUnit.MINUTES.between(Instant.ofEpochSecond(timeEpochSeconds1), Instant.ofEpochSecond(timeEpochSeconds2))
  }

  private def doSplit(currentSession: ListBuffer[(Song, Long)],
                      remainingSongsAndDates: Iterator[(Song, Long)],
                      result: ListBuffer[Seq[(Song, Long)]]): Seq[Seq[(Song, Long)]] = {

    if (remainingSongsAndDates.isEmpty) {
      if (currentSession.isEmpty) result
      else {
        result += currentSession
        result
      }
    } else {
      val nextSongAndTimestamp: (Song, Long) = remainingSongsAndDates.next()
      if (currentSession.isEmpty || minutesBetween(currentSession.last._2, nextSongAndTimestamp._2) <= 20) {
        currentSession += nextSongAndTimestamp
        doSplit(currentSession, remainingSongsAndDates, result)
      } else {
        result += currentSession
        doSplit(ListBuffer[(Song, Long)](nextSongAndTimestamp), remainingSongsAndDates, result)
      }
    }
  }

  def splitIntoSessions(songsAndDates: Iterator[(Song, Long)]): Seq[Seq[(Song, Long)]] = {
    LogProcessor.doSplit(new ListBuffer, songsAndDates, new ListBuffer)
  }

  def uniqueUsers(logId: Int) = sc.objectFile[User](s"/tmp/uniqueUsers/$logId").collect()
  def usersSortedByNumberOfSongsPlayed(logId: Int) = sc.objectFile[User](s"/tmp/usersSortedByNumberOfSongsPlayed/$logId").collect()
  def songFrequency(logId: Int) = sc.objectFile[Song](s"/tmp/songFrequency/$logId").collect()
  def sessionsSortedByDuration(logId: Int) = sc.objectFile[Session](s"/tmp/sessionsSortedByDuration/$logId").collect()
}
