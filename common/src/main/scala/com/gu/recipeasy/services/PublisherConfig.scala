package com.gu.recipeasy.services

import com.amazonaws.auth.{ AWSCredentialsProviderChain, STSAssumeRoleSessionCredentialsProvider }
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.Region
import com.amazonaws.services.kinesis.AmazonKinesisClient
import play.api.Configuration

case class ContentAtomConfig(streamName: String, stsRoleArn: String, kinesisClient: AmazonKinesisClient, capiKey: String)
case class AuxiliaryAtomConfig(streamName: String, stsRoleArn: String, kinesisClient: AmazonKinesisClient)
case class PublisherConfig(contentAtomConfig: ContentAtomConfig, auxiliaryAtomConfig: AuxiliaryAtomConfig)

object PublisherConfig {

  def apply(config: Configuration, region: Region, stage: String): PublisherConfig = {

    val contentAtomStreamName = s"content-atom-events-live-${stage.toUpperCase}"

    val contentAtomStsRoleArn = config.getString("aws.atom.content.stsRoleArn").getOrElse("")

    val contentApiKey = config.getString("capi.key").getOrElse("")

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
    },
      capiKey = contentApiKey
    )

    val auxiliaryAtomStreamName = s"auxiliary-atom-feed-${stage.toUpperCase}"

    val auxiliaryAtomStsRoleArn = config.getString("aws.atom.auxiliary.stsRoleArn").getOrElse("")

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