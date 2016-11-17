#!/usr/bin/env bash
#
# Sets up a tunnel through a recipeasy instance,
# then performs migrations using Flyway

rds_host=${1?RDS host missing}

dbname=recipeasy
dbuser=recipeasy

recipeasy_host=$(marauder -s stage=PROD stack=content-api-recipeasy app=recipeasy 2>/dev/null)

ssh -o StrictHostKeyChecking=no -N -L 7777:${rds_host}:5432 ubuntu@$recipeasy_host &
SSH_PID=$!

trap finish SIGINT EXIT
function finish {
    kill $SSH_PID
}

echo Waiting for SSH tunnel to settle
sleep 3

echo Running DB migrations using Flyway ...
read -s -p 'DB password: ' dbpassword
sbt -Dflyway.url="jdbc:postgresql://localhost:7777/$dbname" -Dflyway.user="$dbuser" -Dflyway.password="$dbpassword" common/flywayMigrate

