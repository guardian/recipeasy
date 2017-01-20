#!/bin/bash
#
# Connects localhost to PROD database and reindexes
# Requires application.conf to be correctly populated

aws rds describe-db-instances 1>/dev/null
if [ "$?" -eq 255 ]
then
    echo You need to have valid capi and composer AWS credentials
    exit
else
    rds_host=$(aws rds describe-db-instances --db-instance-identifier recipeasy-rds-primary-prod | jq -r .DBInstances[].Endpoint.Address 2>/dev/null)
    ec2=$(marauder -s stage=PROD stack=content-api-recipeasy app=recipeasy 2>/dev/null)
    stage=${1?Stage missing, use CODE or PROD}

    echo Setting up SSH tunnel
    ssh -o StrictHostKeyChecking=no -N -L 7777:${rds_host}:5432 ubuntu@${ec2} &
    SSH_PID=$!

    trap finish SIGINT EXIT
    function finish {
        kill $SSH_PID
    }

    sleep 3

    echo Running reindex

    sbt "indexer/run $stage"
fi
