#!/usr/bin/env bash
#
# Sets up a tunnel through a recipeasy instance,
# creates a Postgres user and DB schema,
# then performs migrations using Flyway

rds_host=${1?RDS host missing}
rds_master_user=${2?DB master username missing}

read -s -p 'RDS master password: ' rds_master_password
export PGPASSWORD=$rds_master_password

dbname=recipeasy
dbuser=recipeasy

recipeasy_host=$(marauder -s stage=PROD stack=content-api-recipeasy app=recipeasy 2>/dev/null)

if ! hash psql 2>/dev/null
then 
  echo PostgreSQL is not installed. Installing using Homebrew ...
  brew update && brew install postgresql
else
  echo Looks like PostgreSQL is already installed.
fi

ssh -o StrictHostKeyChecking=no -N -L 7777:${rds_host}:5432 ubuntu@$recipeasy_host &
SSH_PID=$!

trap finish SIGINT EXIT
function finish {
    unset PGPASSWORD
    kill $SSH_PID
}

echo Waiting for SSH tunnel to settle
sleep 3

if ! psql -h localhost -p 7777 -U $rds_master_user postgres -t -c '\du' | cut -d \| -f 1 | grep -qw $dbuser
then
  echo DB user $dbuser does not appear to exist. Creating user ...
  createuser -h localhost -p 7777 -U $rds_master_user -P $dbuser
else
  echo Looks like the DB user $dbuser already exists.
fi

if ! psql -h localhost -p 7777 -U $rds_master_user -lqt | cut -d \| -f 1 | grep -qw $dbname
then
  echo $dbname database does not appear to exist. Creating DB ...
  # Because of some RDS weirdness, the DB has to be created by its owner
  psql -h localhost -p 7777 -U $rds_master_user -c "ALTER ROLE $dbuser CREATEDB" postgres
  createdb -h localhost -p 7777 -U $dbuser -W --owner=$dbuser $dbname
  psql -h localhost -p 7777 -U $rds_master_user -c "ALTER ROLE $dbuser NOCREATEDB" postgres
else
  echo Looks like the $dbname DB already exists.
fi

echo Running DB migrations using Flyway ...
read -s -p 'DB password: ' dbpassword
sbt -Dflyway.url="jdbc:postgresql://localhost:7777/$dbname" -Dflyway.user="$dbuser" -Dflyway.password="$dbpassword" common/flywayMigrate
