package za.co.absa.statusboard.utils

import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json, ParsingFailure, jawn}
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.{AttributeValue, CreateTableRequest, DeleteTableRequest, DescribeTableRequest, ResourceInUseException, ResourceNotFoundException, TableStatus}
import zio.{IO, Schedule, Task, ZIO, durationInt}
import zio.ZIO.fromEither

import java.io.IOException
import java.util.UUID
import scala.jdk.CollectionConverters._

object DynamoDbUtils {
  def sAttributeValueFromPOJO[T](pojo: T)(implicit encoder: Encoder[T]): AttributeValue = sAttributeValueFromJson(pojo.asJson)
  def sAttributeValueFromJson(json: Json): AttributeValue = sAttributeValueFromString(json.toString)
  def sAttributeValueFromString(s: String): AttributeValue = AttributeValue.builder().s(s).build()
  def sAttributeValueFromUUID(uuid: UUID): AttributeValue = sAttributeValueFromString(uuid.toString)
  def nAttributeValueFromLong(l: Long): AttributeValue = AttributeValue.builder().n(l.toString).build()
  def nAttributeValueFromInt(n: Int): AttributeValue = AttributeValue.builder().n(n.toString).build()
  def boolAttributeValueFromBoolean(b: Boolean): AttributeValue = AttributeValue.builder().bool(b).build()
  def ssAttributeValueFromStringSet(ss: Set[String]): AttributeValue = AttributeValue.builder().ss(ss.asJava).build()

  def sAttributeValueToPOJO[T](item: java.util.Map[String, AttributeValue], field: String)(implicit decoder: Decoder[T]): IO[io.circe.Error, T] = fromEither(jawn.parse(sAttributeValueToString(item, field)).flatMap(_.as[T]))
  def sAttributeValueToJson(item: java.util.Map[String, AttributeValue], field: String): IO[ParsingFailure, Json] = fromEither(jawn.parse(sAttributeValueToString(item, field)))
  def sAttributeValueToString(item: java.util.Map[String, AttributeValue], field: String): String = item.get(field).s()
  def sAttributeValueToUUID(item: java.util.Map[String, AttributeValue], field: String): UUID = UUID.fromString(sAttributeValueToString(item, field))
  def nAttributeValueToLong(item: java.util.Map[String, AttributeValue], field: String): Long = item.get(field).n().toLong
  def nAttributeValueToInt(item: java.util.Map[String, AttributeValue], field: String): Int = item.get(field).n().toInt
  def boolAttributeValueToBoolean(item: java.util.Map[String, AttributeValue], field: String): Boolean = item.get(field).bool()
  def ssAttributeValueToStringSet(item: java.util.Map[String, AttributeValue], field: String): Set[String] =  item.get(field).ss().asScala.toSet

  implicit class DynamoDbExtensions(dynamoDbClient: DynamoDbClient) {
    private def fetchTableStatus(tableName: String): Task[Option[TableStatus]] = for {
      request <- ZIO.attempt(DescribeTableRequest.builder().tableName(tableName).build())
      status <- ZIO.attempt {
        Some(dynamoDbClient.describeTable(request).table().tableStatus())
      }.catchSome {
        case _: ResourceNotFoundException => ZIO.none
      }
    } yield status

    private def awaitTableActive(tableName: String): Task[Unit] = {
      for {
        status <- fetchTableStatus(tableName)
        _ <- ZIO.logInfo(s"Awaiting DDB Table [$tableName] Active: $status")
        _ <- ZIO.when(!status.contains(TableStatus.ACTIVE))(ZIO.fail(new IOException(s"DDB table not ready yet: $status")))
      } yield ()
    }.retry(Schedule.exponential(1.second) && Schedule.recurs(6))

    private def awaitTableDeleted(tableName: String): Task[Unit] = {
      for {
        status <- fetchTableStatus(tableName)
        _ <- ZIO.logInfo(s"Awaiting DDB Table [$tableName] Not Present: $status")
        _ <- ZIO.when(status.isDefined)(ZIO.fail(new IOException(s"DDB table still present: $status")))
      } yield ()
    }.retry(Schedule.exponential(1.second) && Schedule.recurs(6))

    def createTableSafe(request: CreateTableRequest): Task[Unit] = for {
      _ <- ZIO.attempt {
        dynamoDbClient.createTable(request)
      }.catchSome { case _: ResourceInUseException => ZIO.unit }
      _ <- awaitTableActive(request.tableName())
    } yield ()

    def deleteTableSafe(tableName: String): Task[Unit] = for {
      request <- ZIO.attempt(DeleteTableRequest.builder().tableName(tableName).build())
      _ <- ZIO.attempt {
        dynamoDbClient.deleteTable(request)
      }.catchSome { case _: ResourceNotFoundException => ZIO.unit }
      _ <- awaitTableDeleted(tableName)
    } yield ()
  }
}
