package utils

import software.amazon.awssdk.auth.credentials.{
  AwsCredentialsProvider,
  StaticCredentialsProvider,
  AwsBasicCredentials
}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import java.net.URI

object LocalStack {
  lazy private val localStackBasicAWSCredentialsProvider: AwsCredentialsProvider =
    StaticCredentialsProvider.create(AwsBasicCredentials.create("accessKey", "secretKey"))

  lazy val s3Client = S3Client
    .builder()
    .credentialsProvider(localStackBasicAWSCredentialsProvider)
    .region(Region.EU_WEST_1)
    .endpointOverride(URI.create("http://localhost:4566"))
    // This is needed for localstack
    .forcePathStyle(true)
    .build()
}
