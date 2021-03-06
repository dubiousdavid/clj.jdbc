==========
User Guide
==========


Connecting to database
======================

Connection parameters
---------------------

Usually, the documentation of any jvm language that explains JDBC always
supposes that the reader comes from Java and knowns JDBC well. This
documentation will not make the same mistake.

JDBC is the default Java abstraction/interface for SQL databases.  It's like
the Python DB-API and similar abstractions in other languages.  Clojure, as a
guest language on the jvm, benefits from having a good, well tested abstraction
like this.

``dbspec`` is a simple Clojure way to define the database connection parameters
that are used to create a new database connection or create a new datasource
(connection pool) using plain clojure data structure.

This is a default aspect of one dbspec definition:

.. code-block:: clojure

    {:classname "org.postgresql.Driver"
     :subprotocol "postgresql"
     :subname "//localhost:5432/dbname"
     :user "username"
     :password "password"}

Description of the parameters:

- ``:classname`` This is a class location of JDBC driver. Each driver has one; in
  this example it is a path to a Postgresql JDBC driver.  This parameter can be
  omited and in that case it is automatically resolved from a predefined list
  using ``:subprotocol`` key.
- ``:user`` and ``:password`` can be ommited if they are empty.

dbspec has other formats which are ultimately parsed to the previously explained format.
As an example, you can pass a string containing a url with same data:

.. code-block:: text

    "postgresql://user:password@localhost:5432/dbname"


Creating a connection
---------------------

In clj.jdbc, every function that interacts with a database takes a connection
instance as parameter. In contrast to clojure.java.jdbc, clj.jdbc requires
connections to be explicitly opened before use (you can not use dbspec map
as connection).

For this purpose, clj.jdbc exposes two ways to create new connections:
then ``make-connection`` function and the ``with-connection`` macro.

The first way intends be a low level interface and delegates resource
management to the user (who has to close the connection once it is no longer
used, for example). The second way is a higher level interface:
``with-connection`` creates a context in which the connection is available and,
at the end of execution of the code within the context, the connection is
closed automatically.

.. note::

    clj.jdbc can not use any global/thread-local state, and always try ensure
    immutability.


.. note::

    If you use connection pools, connection is automatically returned to the pool.

This is a simple example of use ``make-connection``:

.. code-block:: clojure

    (let [conn (make-connection dbspec)]
      (do-something-with conn)
      (.close conn))

The Connection class implements the AutoClosable interface, so you can avoid
having to call the ``close`` method directly by using Clojure's ``with-open``
macro:

.. code-block:: clojure

    (with-open [conn (make-connection dbspec)]
      (do-something-with conn))

And this is an equivalent piece of code using the ``with-connection`` macro
that clj.jdbc provides:

.. code-block:: clojure

    (with-connection dbspec conn
      (do-something-with conn))


Execute database commands
=========================

clj.jdbc has many methods for executing database commands, like creating
tables, inserting data or simply executing stored procedures.


Execute raw sql statements
--------------------------

The simplest way to execute a raw SQL is using the ``execute!`` function. It
receives a connection as the first parameter followed by variable list
of sql sentences:

.. code-block:: clojure

    ;; Without transactions
    (with-connection dbspec conn
      (execute! conn "CREATE TABLE foo (id serial, name text);"))

    ;; In one transaction
    (with-connection dbspec conn
      (with-transaction conn
        (execute! conn "CREATE TABLE foo (id serial, name text);")))


Execute parametrized SQL statements
-----------------------------------

Raw SQL statements work well for creating tables and similar operations, but
when you need to insert some data, especially if the data comes from untrusted
sources, the ``execute!`` function is not adequate.

For this problem, clj.jdbc exposes the ``execute-prepared!`` function. It
accepts parametrized SQL and a list of groups of parameters.

To execute a simple insert SQL statement:

.. code-block:: clojure

    (let [sql "INSERT INTO foo VALUES (?, ?);"]
      (execute-prepared! conn sql ["Foo", 2]))

The `execute-prepared!` function can accept multiple param groups, that are
helpful for performing multiple inserts in a batch:

.. code-block:: clojure

    (let [sql "INSERT INTO foo VALUES (?, ?);"]
      (execute-prepared! conn sql ["Foo", 2] ["Bar", 3]))

The previous code should execute these SQL statements:

.. code-block:: sql

    INSERT INTO foo VALUES ('Foo', 2);
    INSERT INTO foo VALUES ('Bar', 3);


Make queries
============

As usual, clj.jdbc offers two ways to send queries to a database. But in this
section only will be explained the basic and the most usual way to make queries
using a ``query`` function.

``query`` function, given a active connection and vector with sql query as string
with optional parameters, executes it and returns a evaluated result as vector of
records:

.. code-block:: clojure

    (let [sql    ["SELECT id, name FROM people WHERE age > ?", 2]
          result (query sql)]
      (doseq [row results]
        (println row))))


.. note::

    This method seems usefull en most of cases but can not works well with
    queries that returns a lot of results. For this purpose, exists cursor
    type queries that are explained on :ref:`Advanced usage <cursor_queries>`
    section.


Transactions
============

Managing transactions well is almost the most important thing when building an
application, and delaying it to the end is not a good approach. Managing
transactions implicitly, trusting your "web framework" to do it for you, is
another very bad approach.

All transactions related functions are exposed on ``jdbc.transaction`` namespace
and if you need transactions, you should import that namespace:

.. code-block:: clojure

    (ns some.my.ns
      (:require [jdbc.transaction :as tx]))


The most idiomatic way to wrap some code in transaction, is using ``with-transaction``
macro:

.. code-block:: clojure

    (tx/with-transaction conn
       (do-thing-first conn)
       (do-thing-second conn))

Also, **clj.jdbc** exposes a more low level iterface, that permits a execute some
function in a transaction, using ``call-in-transaction`` function (``with-transaction``
macro uses this function internally).

Example:

.. code-block:: clojure

    (tx/call-in-transaction conn (fn [conn] (do-something-with conn)))


A callback function passwd to call-in-transaction should accept almost one parameter:
a connection.

.. note::

    clj.jdbc in contrast to java.jdbc, handles well nested transactions. So making all
    code wrapped in transaction block truly atomic independenty of transaction nesting.

    If you want extend o change a default transaction strategy, see
    :ref:`Transaction Strategy section <transaction-strategy>`.


Isolation Level
---------------

clj.jdbc by default does nothing with isolation level and keep it with default values. But
provides a simple way to set specific isolation level if is needed.

As example, each connection created with this dbspec automatically set
a isolation level to SERIALIZABLE:

.. code-block:: clojure

    (def dbsoec {:subprotocol "h2"
                 :subname "mem:"
                 :isolation-level :serializable})

This is a list of supported options:

- ``:read-commited`` - Set read committed isolation level
- ``:repeatable-read`` - Set repeatable reads isolation level
- ``:serializable`` - Set serializable isolation level
- ``:none`` - Use this option to indicate to clj.jdbc to do nothing and keep default behavior.

You can read more about it on wikipedia_.

.. _wikipedia: http://en.wikipedia.org/wiki/Isolation_(database_systems)
