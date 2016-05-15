package model

import java.time.ZonedDateTime

import org.apache.spark.sql.types.TimestampType

case class RawLogEntry(userId: String, timeStamp: String, artistId: String, artistName: String, trackId: String, trackName: String)

case class LogEntry(user: User, timeStamp: Long, song: Song)
