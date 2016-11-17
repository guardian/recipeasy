#!/usr/bin/env bash

dbname=recipeasy

echo Running DB migrations using Flyway ...
sbt -Dflyway.url="jdbc:postgresql://localhost:5432/$dbname" common/flywayMigrate
