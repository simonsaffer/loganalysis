package ml

import model.LogEntry
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.linalg.{Vectors, Vector}

class ListeningTimePredictor {

  def prepareFeaturesForPerson(logEntries: Iterator[LogEntry]): Seq[LabeledPoint] = {
    val entries = logEntries.toSeq
    val featureVector = (entries map (entry => Vectors.dense(entry.timeStamp.toDouble))) slice (0, logEntries.size-1)
    val labeledPoints =
      (entries slice (1, logEntries.size) map(_.timeStamp)) zip(featureVector) map(l => LabeledPoint(l._1, l._2))

    labeledPoints
  }

}
