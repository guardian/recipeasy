package services

import com.amazonaws.services.kinesis.model.PutRecordRequest
import com.amazonaws.services.kinesis.model.PutRecordResult
import com.gu.auxiliaryatom.model.auxiliaryatomevent.v1.AuxiliaryAtomEvent
import com.gu.contentatom.thrift.ContentAtomEvent

import scala.util.Try

object AtomPublisher {

  private object composerAuxiliaryAtomIntegration {

    def send(event: AuxiliaryAtomEvent)(config: PublisherConfig): Try[PutRecordResult] = {
      val data = ThriftSerializer.serializeEvent(event)
      val record = new PutRecordRequest()
        .withData(data)
        .withStreamName(config.auxiliaryAtomConfig.streamName)
        .withPartitionKey(event.contentId)

      Try(config.auxiliaryAtomConfig.kinesisClient.putRecord(record))
    }
  }

  private object porterAtomIntegration {

    def send(event: ContentAtomEvent)(config: PublisherConfig): Try[PutRecordResult] = {
      val data = ThriftSerializer.serializeEvent(event)
      val record = new PutRecordRequest()
        .withData(data)
        .withStreamName(config.contentAtomConfig.streamName)
        .withPartitionKey(event.atom.atomType.name)

      Try(config.contentAtomConfig.kinesisClient.putRecord(record))
    }

  }

  /**
   * Sends events to Porter and Composer to create new Content and Auxiliary atoms respectively.
   * @param atomEvents
   * @param config
   */
  def send(atomEvents: (AuxiliaryAtomEvent, ContentAtomEvent))(config: PublisherConfig): Try[List[PutRecordResult]] = {
    val auxiliaryAtomEvent = atomEvents._1
    val contentAtomEvent = atomEvents._2
    for {
      composerPutResult <- composerAuxiliaryAtomIntegration.send(auxiliaryAtomEvent)(config)
      porterPutResult <- porterAtomIntegration.send(contentAtomEvent)(config)
    } yield List(composerPutResult, porterPutResult)
  }

}