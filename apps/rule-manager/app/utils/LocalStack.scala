package utils

import com.amazonaws.auth.{
  AWSCredentialsProvider,
  AWSStaticCredentialsProvider,
  BasicAWSCredentials
}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder

object LocalStack {
  lazy private val localStackBasicAWSCredentialsProviderV1: AWSCredentialsProvider =
    new AWSStaticCredentialsProvider(new BasicAWSCredentials("accessKey", "secretKey"))

  lazy val s3Client = AmazonS3ClientBuilder
    .standard()
    .withCredentials(localStackBasicAWSCredentialsProviderV1)
    .withEndpointConfiguration(
      new EndpointConfiguration("http://localhost:4566", Regions.EU_WEST_1.getName)
    )
    // This is needed for localstack
    .enablePathStyleAccess()
    .build()
}
