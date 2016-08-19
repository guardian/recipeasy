#!/usr/bin/env bash

dbname=recipeasy
dbuser=recipeasy

if ! hash psql 2>/dev/null
then 
  echo PostgreSQL is not installed. Installing using Homebrew ...
  brew update && brew install postgresql
else
  echo Looks like PostgreSQL is already installed.
fi

if [ -z "$(pgrep postgres)" ]
then 
  echo Postgres server does not appear to be running. Starting it as a background process ...
  postgres -D /usr/local/var/postgres &
  sleep 5
else
  echo Looks like PostgreSQL server is already running.
fi

if ! psql postgres -t -c '\du' | cut -d \| -f 1 | grep -qw $dbuser
then
  echo DB user $dbuser does not appear to exist. Creating user ...
  createuser $dbuser
else
  echo Looks like the DB user $dbuser already exists.
fi

if ! psql -lqt | cut -d \| -f 1 | grep -qw $dbname
then
  echo $dbname database does not appear to exist. Creating DB ...
  createdb --owner=$dbuser $dbname
else
  echo Looks like the $dbname DB already exists.
fi

echo Running DB migrations using Flyway ...
sbt -Dflyway.url="jdbc:postgresql://localhost:5432/$dbname" common/flywayMigrate
