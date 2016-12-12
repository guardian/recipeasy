package services

import java.io.File

import com.amazonaws.auth.AWSCredentialsProviderChain
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider.Builder
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.{ Region, Regions }
import com.amazonaws.services.kinesis.AmazonKinesisClient
import com.gu.contentapi.client.GuardianContentClient
import play.api.Configuration

import scala.util.Try

case class ContentAtomConfig(streamName: String, stsRoleArn: String, kinesisClient: AmazonKinesisClient)
case class AuxiliaryAtomConfig(streamName: String, stsRoleArn: String, kinesisClient: AmazonKinesisClient)
case class PublisherConfig(contentAtomConfig: ContentAtomConfig, auxiliaryAtomConfig: AuxiliaryAtomConfig)

object PublisherConfig {

  def apply(config: Configuration, region: Region, stage: String): PublisherConfig = {

    val contentAtomStreamName = s"content-atom-events-live-${stage.toUpperCase}"

    val contentAtomStsRoleArn = config.getString("aws.atom.content.stsRoleArn").getOrElse("")

    val contentAtomConfig = ContentAtomConfig(
      contentAtomStreamName,
      contentAtomStsRoleArn,
      kinesisClient = {
      val kinesisCredentialsProvider = new AWSCredentialsProviderChain(
        new ProfileCredentialsProvider("composer"),
        new Builder(contentAtomStsRoleArn, "contentAtom").build()
      )

      val kinesisClient = new AmazonKinesisClient(kinesisCredentialsProvider)
      kinesisClient.setRegion(region)
      kinesisClient
    }
    )

    val auxiliaryAtomStreamName = s"auxiliary-atom-feed-${stage.toUpperCase}"

    val auxiliaryAtomStsRoleArn = config.getString("aws.atom.auxiliary.stsRoleArn").getOrElse("")

    val auxiliaryAtomConfig = AuxiliaryAtomConfig(
      auxiliaryAtomStreamName,
      auxiliaryAtomStsRoleArn,
      kinesisClient = {
      val kinesisCredentialsProvider = new AWSCredentialsProviderChain(
        new ProfileCredentialsProvider("composer"),
        new Builder(auxiliaryAtomStsRoleArn, "auxiliaryAtom").build()
      )

      val kinesisClient = new AmazonKinesisClient(kinesisCredentialsProvider)
      kinesisClient.setRegion(region)
      kinesisClient
    }
    )

    PublisherConfig(contentAtomConfig, auxiliaryAtomConfig)
  }

}