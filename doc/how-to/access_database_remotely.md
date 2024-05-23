# How to access the database remotely

**Important** - Accessing a live console is very risky and should only be
done as a last resort. This should ideally only be done in pairs, and
mutating any live data is STRONGLY discouraged. When data needs to be modified
create a tested [data migration job](/doc/how-to/run_migration_job_remotely.md).

Both methods detailed below use [Jaqy](https://teradata.github.io/jaqy/), which
is a Java-native universal database client. As well as carrying out simple
queries, you can also do exports of data (enabling, for example, one off reporting),
and other useful bits and pieces. For full details, check the
[documentation](https://teradata.github.io/jaqy/).
The Jaqy library is managed by a third party so we validate the checksum to
ensure the contents aren't unexpectedly modified.

There are two ways to do this:

## The easy way

For most use cases (i.e. running simple queries etc), this will be the way
you want to do it. The downside is you won't have access to things like
data exports from the local shell.

From your local machine run:

```bash
script/remote_db $ENVIRONMENT
```

Where $ENVIRONMENT is the environment you want to connect to.

When you're finished, type the `.quit` command to end the session.

By default this will set the session to be **read-only**. You can check this by
running:

```sql
SELECT setting FROM pg_settings WHERE name =
'default_transaction_read_only';`
```

Should you need write access in an
exceptional situation, you can set this back to false from within the session:

```sql
SET default_transaction_read_only = FALSE;
```

## The hard(er) way

This is more suitable if you need access to the bash shell, as well as
the ability to make SQL queries.

From your local machine run:

```bash
script/remote_shell $ENVIRONMENT
```

Once connected, you'll need to download [jaqy](https://teradata.github.io/jaqy/index.html)
to connect to the database, together with an appropriate Postgres driver.

To do this first go to the home directory in the image:

```bash
cd ~
```

Then download the appropriate `.jar` files:

```bash
curl -L https://github.com/Teradata/jaqy/releases/download/v1.2.0/jaqy-1.2.0.jar --output jaqy-1.2.0.jar

curl -L https://jdbc.postgresql.org/download/postgresql-42.5.1.jar --output postgresql-42.5.1.jar
```

You can then run jaqy and connect to the database with the following
command:

```bash
java -jar jaqy-1.2.0.jar -- .classpath postgresql postgresql-42.5.1.jar \; .open -u ${SPRING_DATASOURCE_USERNAME} -p ${SPRING_DATASOURCE_PASSWORD} postgresql://${DB_HOST}/${DB_NAME}
```

When you're finished, type the `.quit` command and then `exit` to quit
the remote shell session.
