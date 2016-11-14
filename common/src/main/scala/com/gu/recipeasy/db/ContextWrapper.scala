package com.gu.recipeas.db

import com.typesafe.config.{ Config => TypesafeConfig }
import io.getquill.{ ImplicitQuery, JdbcContext, PostgresDialect, SnakeCase }
import java.time.OffsetDateTime

trait ContextWrapper {
  val config: TypesafeConfig
  lazy val dbContext = new JdbcContext[PostgresDialect, SnakeCase](config.getConfig("db.ctx")) with ImplicitQuery with Quotes

  trait Quotes {
    this: JdbcContext[_, _] =>
    implicit class DateTimeQuotes(left: OffsetDateTime) {
      def >(right: OffsetDateTime) = quote(infix"$left > $right".as[Boolean])
      def <(right: OffsetDateTime) = quote(infix"$left < $right".as[Boolean])
    }
  }
}
