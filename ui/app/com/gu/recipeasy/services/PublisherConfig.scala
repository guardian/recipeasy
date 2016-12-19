package services

import java.io.File

import com.amazonaws.auth.{AWSCredentialsProviderChain, STSAssumeRoleSessionCredentialsProvider}
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider.Builder
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.kinesis.AmazonKinesisClient
import com.gu.contentapi.client.GuardianContentClient
import com.typesafe.scalalogging.StrictLogging
import play.api.Configuration

import scala.util.Try

case class ContentAtomConfig(streamName: String, stsRoleArn: String, kinesisClient: AmazonKinesisClient)
case class AuxiliaryAtomConfig(streamName: String, stsRoleArn: String, kinesisClient: AmazonKinesisClient)
case class PublisherConfig(contentAtomConfig: ContentAtomConfig, auxiliaryAtomConfig: AuxiliaryAtomConfig)

object PublisherConfig extends StrictLogging {

  def apply(config: Configuration, region: Region, stage: String): PublisherConfig = {

    val contentAtomStreamName = s"content-atom-events-live-${stage.toUpperCase}"

    logger.info("contentAtomStreamName = " + contentAtomStreamName)

    val contentAtomStsRoleArn = config.getString("aws.atom.content.stsRoleArn").getOrElse("")

    logger.info("contentAtomStsRoleArn = " + contentAtomStsRoleArn)

    val contentAtomConfig = ContentAtomConfig(
      contentAtomStreamName,
      contentAtomStsRoleArn,
      kinesisClient = {
      val kinesisCredentialsProvider = new AWSCredentialsProviderChain(
        new STSAssumeRoleSessionCredentialsProvider(contentAtomStsRoleArn, "contentAtom"),
        new ProfileCredentialsProvider("composer")
      )

      val kinesisClient = new AmazonKinesisClient(kinesisCredentialsProvider)
      kinesisClient.setRegion(region)
      kinesisClient
    }
    )

    val auxiliaryAtomStreamName = s"auxiliary-atom-feed-${stage.toUpperCase}"

    logger.info("auxiliaryAtomStreamName = " + auxiliaryAtomStreamName)

    val auxiliaryAtomStsRoleArn = config.getString("aws.atom.auxiliary.stsRoleArn").getOrElse("")

    logger.info("auxiliaryAtomStsRoleArn = " + auxiliaryAtomStsRoleArn)

    val auxiliaryAtomConfig = AuxiliaryAtomConfig(
      auxiliaryAtomStreamName,
      auxiliaryAtomStsRoleArn,
      kinesisClient = {
      val kinesisCredentialsProvider = new AWSCredentialsProviderChain(
        new STSAssumeRoleSessionCredentialsProvider(auxiliaryAtomStsRoleArn, "auxiliaryAtom"),
        new ProfileCredentialsProvider("composer")
      )

      val kinesisClient = new AmazonKinesisClient(kinesisCredentialsProvider)
      kinesisClient.setRegion(region)
      kinesisClient
    }
    )

    PublisherConfig(contentAtomConfig, auxiliaryAtomConfig)
  }

}