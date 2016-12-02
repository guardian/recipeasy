package com.gu.recipeasy

import java.util.concurrent.TimeUnit
import com.github.benmanes.caffeine.cache.Caffeine

class Cache[T <: Object, R <: Object](maximumSize: Long, expireAfterHours: Long) {

  private lazy val cache = Caffeine.newBuilder()
    .maximumSize(maximumSize)
    .expireAfterWrite(expireAfterHours, TimeUnit.HOURS)
    .build[T, R]()

  def get(key: T): Option[R] = Option(cache.getIfPresent(key))
  def put(key: T, value: R) { cache.put(key, value) }
}

object ProgressCache extends Cache[String, java.lang.Double](maximumSize = 2, expireAfterHours = 1)

