# Indexer

This sbt project indexes all 'curated'\* recipes (from PROD DB) to either capi
CODE or PROD.

```
$ sbt "indexer/run <stage>"
```

You need to connect to the DB by creating an ssh tunnel before you run the
project. If you run the script `./reindex.sh` this is done for you. ⚡️

## Running Indexer locally

1. From the resources directory create a conf file: `cp application.conf.tempate
   application.conf`. You only need to fill in the four missing fields, note the
   Arns need to be in double quotes. If you need help filling in the file ask a
   team member.

2. Get AWS credentials for capi and composer.

3. Run `./reindex.sh {STAGE}`.

\* Recipes which have been curated at least once.
