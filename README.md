# DB Environment Setup

## Learning Goals

- Setting up a multi-node DB environment
- Configuring Spring Data applications for multi-path DB operations

## Instructions

Up until this point, we've been dealing with our database system as a single concrete entity. But, in real life, this layer itself will have all the same
High Availability and Scalability challenges to meet as the Web Application layers you may be developing. While all you may be doing is passing a single
connection string into your application, there is still a lot going on behind the scenes that you may need to be aware of. Especially if you are working
on larger projects that start pushing the boundaries of performance, availability, or application design. At these points, bottlenecks become readily visible,
so knowing how common Database environments may be constructed will go a long way.

In this lab, we will be constructing a local Database cluster, providing some failover and load balancing functionality similar to what you may run across
in HA DB clusters.

## Setting up a multi-node DB environment

### Single node master

We'll start first with setting up a Postgres Master node.

``` shell
# Stop our current instance first
docker stop postgres-lab
# Create dedicated docker database network
docker network create postgres
# Create the new master instance with all necessary cluster configurations
docker run -d --name postgres-master-1 --network postgres -p 5432:5432 \
    -e REPMGR_PARTNER_NODES=postgres-master-1,postgres-replica-1,postgres-replica-2,postgres-replica-3 \
    -e REPMGR_NODE_NAME=postgres-master-1 \
    -e REPMGR_NODE_NETWORK_NAME=postgres-master-1 \
    -e REPMGR_NODE_ID=1 \
    -e REPMGR_PRIMARY_HOST=postgres-master-1 \
    -e REPMGR_PASSWORD=repmgrpass \
    -e POSTGRES_PASSWORD=mysecretpassword \
    -e POSTGRES_DATABASE=db_test \
    bitnami/postgresql-repmgr:14.4.0-debian-11-r9
```

Now that we have a running Postgres system again, let's put together a simple application for testing.
We'll start from the simple REST example again.

``` shell
git clone https://github.com/spring-guides/gs-rest-service.git
cd gs-rest-service
git checkout 5cbc686
```

Modify the project file `pom.xml`

``` text
...
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
  </dependency>
  <dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
  </dependency>
...
```

Start with a simple counter again in `src/main/java/com/example/restservice/Counter.java`

``` java
package com.example.restservice;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Counter {
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long count;

    protected Counter() {}

    public Counter(Long count) {
        setCounter(count);
    }

    public Long getCounter() {
        return count;
    }

    public void setCounter(Long count) {
        this.count = count;
    }

}
```

A simple interface in `src/main/java/com/example/restservice/CounterRepository.java`

``` java
package com.example.restservice;

import org.springframework.data.repository.CrudRepository;

public interface CounterRepository extends CrudRepository<Counter, Long> {}
```

And add in some new REST endpoints to `src/main/java/com/example/restservice/GreetingController.java`

``` java
...
import org.springframework.beans.factory.annotation.Autowired;
...

    public Greeting greeting(...
    ...

    @Autowired
    private CounterRepository counterRepository;
    
    @GetMapping("/set_count")
    public Greeting setCount(@RequestParam(value = "value", defaultValue = "0") Long value) {
        Counter newCounter = counterRepository.findById(1L).orElseGet(() -> new Counter(0L));
        newCounter.setCounter(value);
        counterRepository.save(newCounter);
        return new Greeting(newCounter.getCounter(), "Count value set.");
    }
    
	@GetMapping("/get_count")
	public Greeting getCount() {
		Counter newCounter = counterRepository.findById(1L).orElseGet(() -> new Counter(0L));
		return new Greeting(newCounter.getCounter(), "Count value get.");
	}
```

And finally configure the properties for the database in `src/main/resources/application.properties`

``` text
spring.datasource.url=jdbc:postgresql://localhost:5432/db_test
spring.datasource.username=postgres
spring.datasource.password=mysecretpassword

spring.jpa.properties.hibernate.dialect = org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.database=POSTGRESQL
spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation=true
```

At this point, you should be able to set and get simple values, backed by a persistent Postgres database

``` shell
curl "http://localhost:8080/get_count"
{"id":0,"content":"Count value get."}%

curl "http://localhost:8080/set_count?value=10"
{"id":10,"content":"Count value set."}%

curl "http://localhost:8080/set_count?value=20"
{"id":20,"content":"Count value set."}%

curl "http://localhost:8080/get_count"
{"id":20,"content":"Count value get."}%
```

### Read-Only Replica

Since we are using containerized Postgres instances, adding a new Read-Only replica to the cluster is just as easy as setting up the initial Master.
We've already planned for a four node cluster in the configuration parameters previously used, and we can rerun that same command again with just
a few changes.

``` shell
# Create the new replica instance with all necessary cluster configurations
docker run -d --name postgres-replica-1 --network postgres -p 5433:5432 \
    -e REPMGR_PARTNER_NODES=postgres-master-1,postgres-replica-1,postgres-replica-2,postgres-replica-3 \
    -e REPMGR_NODE_NAME=postgres-replica-1 \
    -e REPMGR_NODE_NETWORK_NAME=postgres-replica-1 \
    -e REPMGR_NODE_ID=2 \
    -e REPMGR_PRIMARY_HOST=postgres-master-1 \
    -e REPMGR_PASSWORD=repmgrpass \
    -e POSTGRES_PASSWORD=mysecretpassword \
    bitnami/postgresql-repmgr:14.4.0-debian-11-r9
```

At this point, you can use the psql client in the new container to see that these records have already been replicated. You can also continue to call these
REST endpoints, and see continuous updating in real-time.

``` shell
docker exec -it postgres-replica-1 /bin/bash
I have no name!@5e36133c5838:/$ psql -U postgres
Password for user postgres: # <- mysecretpassword 
psql (14.4)
Type "help" for help.

postgres=# \l
                                  List of databases
   Name    |  Owner   | Encoding |   Collate   |    Ctype    |   Access privileges   
-----------+----------+----------+-------------+-------------+-----------------------
 db_test   | postgres | UTF8     | en_US.UTF-8 | en_US.UTF-8 | 
 postgres  | postgres | UTF8     | en_US.UTF-8 | en_US.UTF-8 | 
 repmgr    | postgres | UTF8     | en_US.UTF-8 | en_US.UTF-8 | 
 template0 | postgres | UTF8     | en_US.UTF-8 | en_US.UTF-8 | =c/postgres          +
           |          |          |             |             | postgres=CTc/postgres
 template1 | postgres | UTF8     | en_US.UTF-8 | en_US.UTF-8 | =c/postgres          +
           |          |          |             |             | postgres=CTc/postgres
(5 rows)

postgres=# \c db_test
You are now connected to database "db_test" as user "postgres".
db_test=# \dt
          List of relations
 Schema |  Name   | Type  |  Owner   
--------+---------+-------+----------
 public | counter | table | postgres
(1 row)

db_test=# SELECT * FROM counter;
 id | count 
----+-------
  1 |     20
(1 row)
```

Now, let's see what happens if we have our application use this new replica as is.
We'll just have to change the single connection string, and restart the application.

> Note: The best way to demonstrate all this further testing would be to configure our application with multiple DataSources and Repositories to view from a single
> client in real-time, but Spring Boot doesn't easily support this without additional poorly documented configuration options. You can take a crack at this if you
> finish early though.

``` text
spring.datasource.url=jdbc:postgresql://localhost:5433/db_test
spring.datasource.username=postgres
spring.datasource.password=mysecretpassword

spring.jpa.properties.hibernate.dialect = org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.database=POSTGRESQL
spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation=true
```

And send some example requests

``` shell
curl "http://localhost:8080/get_count"
{"id":20,"content":"Count value get."}%

curl "http://localhost:8080/set_count?value=21"
{"timestamp":"2022-07-15T22:34:42.673+00:00","status":500,"error":"Internal Server Error","path":"/set_count"}%

# IntelliJ log from the crash
...
2022-07-15 17:34:42.658  WARN 219042 --- [nio-8080-exec-4] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Error: 0, SQLState: 25006
2022-07-15 17:34:42.658 ERROR 219042 --- [nio-8080-exec-4] o.h.engine.jdbc.spi.SqlExceptionHelper   : ERROR: cannot execute UPDATE in a read-only transaction
2022-07-15 17:34:42.659  INFO 219042 --- [nio-8080-exec-4] o.h.e.j.b.internal.AbstractBatchImpl     : HHH000010: On release of batch it still contained JDBC statements
2022-07-15 17:34:42.668 ERROR 219042 --- [nio-8080-exec-4] o.a.c.c.C.[.[.[/].[dispatcherServlet]    : Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Request processing failed; nested exception is org.springframework.orm.jpa.JpaSystemException: could not execute statement; nested exception is org.hibernate.exception.GenericJDBCException: could not execute statement] with root cause

org.postgresql.util.PSQLException: ERROR: cannot execute UPDATE in a read-only transaction
...
```

As we see here, Postgres Replicas are Read-Only on the server end. This isn't just a convention, and nothing is needed to be done on the client side to enforce it.
If you think over the ACID Compliance guarantees from the other lessons, you'll probably be able to see where a RW/RO infrastructure split could violate some of these guarantees,
but let's look though an explicit example to demonstrate this.

Let's say we have a environment with one set of Business Logic reading from the Read-Only portion, and another writing to the Read-Write.
We'll temporarily make some server side modifications on `postgres-replica-1` to mimic networking latency or drops

``` shell
postgres=# ALTER SYSTEM SET recovery_min_apply_delay = '1min';
ALTER SYSTEM
postgres=# SELECT pg_reload_conf();
 pg_reload_conf 
----------------
 t
(1 row)
```

Now:

1. Revert back to using the DB connection string for the RW master `jdbc:postgresql://localhost:5432/db_test` 
2. Restart the application
3. Update the counter: `curl "http://localhost:8080/set_count?value=100"`
4. Change once again to the DB connection string for the RO replica `jdbc:postgresql://localhost:5433/db_test`
5. Restart the application
6. Read the counter: `curl "http://localhost:8080/set_count"`
7. Wait a minute, then read again: `curl "http://localhost:8080/set_count"`

What do we see when we do this?

``` shell
curl "http://localhost:8080/set_count?value=100"
{"id":100,"content":"Count value set."}%

curl "http://localhost:8080/get_count"
{"id":31,"content":"Count value get."}%

curl "http://localhost:8080/get_count"
{"id":100,"content":"Count value get."}%
```

It may seem self evident that data across two different database servers may not be in sync, by what may not be evident is that
the physical layout for a database environment may be quite different than expected due to use of Database load balancing. We'll be taking
a look at this in the next section, but before you go on, make sure to disable that temporary modification we put in place

``` shell
postgres=# ALTER SYSTEM RESET ALL;
ALTER SYSTEM
postgres=# SELECT pg_reload_conf();
 pg_reload_conf 
----------------
 t
(1 row)

```

### Connection Pooling and Load Balancing


Now for upcoming demonstration needs, let's implement a few more endpoints using native SQL queries

``` java
public interface CounterRepository extends CrudRepository<Counter, Long> {
    @Query(
            value = "SELECT id, count, cast(inet_server_addr() as text) FROM counter WHERE id = 1",
            nativeQuery = true)
    String getCounterAndTrace();

    @Query(
            value = "UPDATE counter SET count = ?1 WHERE id = 1 RETURNING id, count, cast(inet_server_addr() as text)",
            nativeQuery = true)
    String setCounterAndTrace(Long count);
}
```

``` java
    @GetMapping("/set_count_trace")
    public Greeting setCountTrace(@RequestParam(value = "value", defaultValue = "0") Long value) {
        Counter newCounter = counterRepository.findById(1L).orElseGet(() -> new Counter(0L));
        String counterRepositoryOutput = counterRepository.setCounterAndTrace(value);
        return new Greeting(Long.parseLong(counterRepositoryOutput.split(",")[1], 10),
                            String.format("Count value set, on instance %s",
                                            counterRepositoryOutput.split(",")[2]));
    }

	@GetMapping("/get_count_trace")
	public Greeting getCountTrace(@RequestParam() {
        String counterRepositoryOutput = counterRepository.getCounterAndTrace();
        return new Greeting(Long.parseLong(counterRepositoryOutput.split(",")[1], 10),
                            String.format("Count value get, on instance %s",
                                            counterRepositoryOutput.split(",")[2]));
	}
```

If you hit these endpoints now, you can see that they do in fact run on the docker container where we expect them to.
Coming up later, we'll be able to use this to inspect database routing.

``` shell
curl "http://localhost:8080/set_count_trace?value=1"
{"id":1,"content":"Count value set, on instance 172.18.0.2/32"}%

curl "http://localhost:8080/get_count_trace"
{"id":1,"content":"Count value get, on instance 172.18.0.2/32"}%

docker inspect --format '{{ .Name }} {{ .NetworkSettings.Networks.postgres.IPAddress }}' postgres-master-1
/postgres-master-1 172.18.0.2

docker inspect --format '{{ .Name }} {{ .NetworkSettings.Networks.postgres.IPAddress }}' postgres-replica-1
/postgres-replica-1 172.18.0.3
```

Before we move on, let's take a brief look at the built in connection pooling we have with Spring Boot. It will be using
[HikariCP]("https://github.com/brettwooldridge/HikariCP") by default, so go ahead and skim through all the options that gives you.
None of the options can be easily demonstrated though, so we'll move on.


A commonly used connection pooler with a larger feature set would be [Pgpool-II]("https://pgpool.net/").
Let's take a look at how this can be used on the server side infrastructure to more transparantly enable load balancing and read/write
splits on the server end.

We'll first add two more replica databases

``` shell
# Create the new replica instances with all necessary cluster configurations
docker run -d --name postgres-replica-2 --network postgres -p 5434:5432 \
    -e REPMGR_PARTNER_NODES=postgres-master-1,postgres-replica-1,postgres-replica-2,postgres-replica-3 \
    -e REPMGR_NODE_NAME=postgres-replica-2 \
    -e REPMGR_NODE_NETWORK_NAME=postgres-replica-2 \
    -e REPMGR_NODE_ID=3 \
    -e REPMGR_PRIMARY_HOST=postgres-master-1 \
    -e REPMGR_PASSWORD=repmgrpass \
    -e POSTGRES_PASSWORD=mysecretpassword \
    bitnami/postgresql-repmgr:14.4.0-debian-11-r9
    
docker run -d --name postgres-replica-3 --network postgres -p 5435:5432 \
    -e REPMGR_PARTNER_NODES=postgres-master-1,postgres-replica-1,postgres-replica-2,postgres-replica-3 \
    -e REPMGR_NODE_NAME=postgres-replica-3 \
    -e REPMGR_NODE_NETWORK_NAME=postgres-replica-3 \
    -e REPMGR_NODE_ID=4 \
    -e REPMGR_PRIMARY_HOST=postgres-master-1 \
    -e REPMGR_PASSWORD=repmgrpass \
    -e POSTGRES_PASSWORD=mysecretpassword \
    bitnami/postgresql-repmgr:14.4.0-debian-11-r9
```

Now go ahead and stand up a Pgpool-II instance in front of all these docker nodes

``` shell
docker run -d --name pgpool --network postgres -p 5436:5432 \
  -e PGPOOL_BACKEND_NODES=0:postgres-master-1:5432,1:postgres-replica-1:5432,2:postgres-replica-2:5432,3:postgres-replica-3:5432 \
  -e PGPOOL_SR_CHECK_USER=postgres \
  -e PGPOOL_SR_CHECK_PASSWORD=mysecretpassword \
  -e PGPOOL_POSTGRES_USERNAME=postgres \
  -e PGPOOL_POSTGRES_PASSWORD=mysecretpassword \
  -e PGPOOL_ADMIN_USERNAME=postgres \
  -e PGPOOL_ADMIN_PASSWORD=mysecretpassword \
  -e PGPOOL_ENABLE_STATEMENT_LOAD_BALANCING=yes \
bitnami/pgpool:4.3.2-debian-11-r16
```

Reconfigure the application to point to this new connection string `jdbc:postgresql://localhost:5436/db_test`, and start hitting some of the endpoints


``` shell
curl "http://localhost:8080/set_count?value=20"
{"id":20,"content":"Count value set."}%
curl "http://localhost:8080/get_count_trace"
{"id":20,"content":"Count value get, on instance 172.19.0.3/32"}%
curl "http://localhost:8080/get_count_trace"
{"id":20,"content":"Count value get, on instance 172.19.0.4/32"}%
curl "http://localhost:8080/get_count_trace"
{"id":20,"content":"Count value get, on instance 172.19.0.3/32"}%
curl "http://localhost:8080/get_count_trace"
{"id":20,"content":"Count value get, on instance 172.19.0.4/32"}%
curl "http://localhost:8080/get_count_trace"
{"id":20,"content":"Count value get, on instance 172.19.0.2/32"}%
...

curl "http://localhost:8080/set_count_trace?value=30"
{"id":30,"content":"Count value set, on instance 172.19.0.2/32"}%
curl "http://localhost:8080/set_count_trace?value=40"
{"id":40,"content":"Count value set, on instance 172.19.0.2/32"}%
...

docker inspect --format '{{ .Name }} {{ .NetworkSettings.Networks.postgres.IPAddress }}' postgres-master-1
/postgres-master-1 172.19.0.2
docker inspect --format '{{ .Name }} {{ .NetworkSettings.Networks.postgres.IPAddress }}' postgres-replica-1
/postgres-replica-1 172.19.0.3
docker inspect --format '{{ .Name }} {{ .NetworkSettings.Networks.postgres.IPAddress }}' postgres-replica-2
/postgres-replica-2 172.19.0.4
docker inspect --format '{{ .Name }} {{ .NetworkSettings.Networks.postgres.IPAddress }}' postgres-replica-3
/postgres-replica-3 172.19.0.5
```

We can see now that writes(UPDATE) are being sent to only the R/W master, while reads(SELECT) are being load balanced between all nodes. In this way, addressing a complex
DB environment can be abstracted to a single endpoint.

TODO - reads not load balancing that great for demo. Maybe still a bug to be fixed here.

### Cluster Failover

One final item we can inspect is the behavior of a cluster failover.

Try running the following, and see what happens when you hit all those previous endpoints again

``` shell
docker rm -f postgres-master-1
```

``` shell
curl "http://localhost:8080/set_count_trace?value=50"
{"id":50,"content":"Count value set, on instance 172.19.0.3/32"}%
curl "http://localhost:8080/set_count_trace?value=60"
{"id":60,"content":"Count value set, on instance 172.19.0.3/32"}%
curl "http://localhost:8080/set_count_trace?value=70"
{"id":70,"content":"Count value set, on instance 172.19.0.3/32"}%
...

curl "http://localhost:8080/get_count_trace"
{"id":70,"content":"Count value get, on instance 172.19.0.4/32"}%
curl "http://localhost:8080/get_count_trace"
{"id":70,"content":"Count value get, on instance 172.19.0.4/32"}%
curl "http://localhost:8080/get_count_trace"
{"id":70,"content":"Count value get, on instance 172.19.0.4/32"}%
curl "http://localhost:8080/get_count_trace"
{"id":70,"content":"Count value get, on instance 172.19.0.3/32"}%
curl "http://localhost:8080/get_count_trace"
{"id":70,"content":"Count value get, on instance 172.19.0.3/32"}%
curl "http://localhost:8080/get_count_trace"
{"id":70,"content":"Count value get, on instance 172.19.0.3/32"}%
...
```

We can see that one of the previous replicas was promoted to the new master, and the cluster continued operating as normal.
There was probably a slight delay though, so it is better to think of cluster failover as a method of disaster recovery, and not a transparent process to applications.


## Advanced Lab


While having this load balancing and RW/RO split handled by the environment is transparent, and requires fairly little work on the
application side to accomplish, this isn't always the most robust way to handle the backing database environment.
Depending on the specific layout of the application, and any specific implementations of business logic, it is sometimes a better choice
to have the handling of Read/Write splits done on the application side.

Take a look to see whether you can implement this workflow directly into the Spring Data application. This isn't a trivial process, and doesn't have
much authorative documentation describing it, but it is possible. Take a look at the following resources to see if you can work out how
to do this:

- [https://vladmihalcea.com/read-write-read-only-transaction-routing-spring/](https://vladmihalcea.com/read-write-read-only-transaction-routing-spring/)
- [https://fable.sh/blog/splitting-read-and-write-operations-in-spring-boot/](https://fable.sh/blog/splitting-read-and-write-operations-in-spring-boot/)

It is worth noting that newer designed applications can take on the design paradims of microservices, allowing for client side management of
data without this approach above. E.g. having microservices split for different R/W roles. But, microservices also come with their own challenges.
