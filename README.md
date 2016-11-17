# Recipeasy

Recipeasy is an app to crowdsource human-guided parsing/structurisation of recipe data.

The process is as follows:

1. An ETL step downloads recipe articles from the Content API, makes a best-effort pass to extract individual recipes and parse them, and writes them to a PostgreSQL DB.

2. A Play app selects semi-parsed recipes from the DB, displays them to users and guides them through the process of parsing/verifying them.

## Running ETL locally

First create and set up the PostgreSQL DB using the `init-dev-DB.sh` script. This will:

* Install PostgreSQL (using Homebrew) if necessary
* Start the PostgreSQL server if it is not already running
* Create the necessary DB schema and users
* Run any necessary DB migrations using the Flyway sbt plugin

Once you have a DB, you can run the ETL script:

```
$ sbt "etl/run <capi key>"
```

This should run for a few minutes. By the end, you will have a few thousand recipes in your DB.

## Upgrading the Database

If you want to upgrade the database, you need to write the corresponding migration file in `recipeasy/common/src/main/resources/db/migration` (and should respect the naming conventions) and then run

```
./flyway-mutation-dev-DB.sh
```

For a production update run (from your local machine)

```
./flyway-mutation-prod-DB.sh [RDS-INSTANCE-DNS-NAME(without port)]
```

and then provide the production database password when asked for it. (Ask for a member of the team where to find the password).

## Running play app

Ensure that Postgres is running

```
postgres -D /usr/local/var/postgres &
```

Start the play application

```
$ sbt "ui/run"
```

The user interface is then available at [http://localhost:9000/recipe/curate](http://localhost:9000/recipe/curate).
