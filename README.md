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

## Running play app

```
$ sbt "ui/run"
```

The user interface is then available at [http://localhost:9000/recipe/curate](http://localhost:9000/recipe/curate).
