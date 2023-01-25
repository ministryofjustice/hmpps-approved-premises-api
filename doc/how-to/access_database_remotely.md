# How to access the database remotely

**Important** - Accessing a live console is very risky and should only be
done as a last resort. This should ideally only be done in pairs, and
mutating any live data is STRONGLY discouraged.

From your local machine run:

```bash
script/remote_shell $ENVIRONMENT
```

Where $ENVIRONMENT is the environment you want to connect to

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
