;; Copyright 2013 Andrey Antukh <niwi@niwi.be>
;;
;; Licensed under the Apache License, Version 2.0 (the "License")
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(ns jdbc.pool
  "clj.jdbc has native support for more than one connection
  pool implementations.

  Currently it has support for:
  - c3p0 (see jdbc.pool.c3p0 namespace)

  Each wrapped connection pool interface only implements
  one function: `make-datasource-spec` that receves a default
  plain dbspec and returns other dbspect with datasource
  instance."
  (:require [jdbc :refer [uri->dbspec]])
  (:import (java.net URI))
  (:gen-class))

(defn normalize-dbspec
  "Normalizes a dbspec in one common format usefull for
  plain connections or connection pool implementations."
  [dbspec]
  (if (or (string? dbspec) (instance? URI dbspec))
    (uri->dbspec dbspec)
    dbspec))

(defn make-datasource-spec
  "Dummy function that returns dbspec as is. This
  function should be implemented in concrete connection
  pool namespaces"
  [dbspec] dbspec)
