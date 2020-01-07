# Cassandra to Kafka via Cassandra Triggers

Application listens to changes in the *Cassandra*, 
constructs *json* object representing modifications and 
sends it to the *Kafka*.

- This Cassandra trigger able to detect modifications on different data types: UDT, LIST, MAP, SET, TUPLE, COUNTER and primitives.
- This Cassandra trigger also can retrieve deleted items from partition update

### Technologies

- Java 8
- Cassandra 3.11.5
- Kafka 2.2.2
- Zookeeper 3.4.6

### How it works

1. First of all, you need to build jar file using the following command

```
$ ./gradlew clean jar
```

2. Start docker containers:
```
$ docker-compose up --build
```

3. Connect to cassandra using Cassandra CLI:
```
$ cqlsh
```

4. Create sample keyspace:
```
CREATE KEYSPACE cycling
  WITH REPLICATION = { 
   'class' : 'SimpleStrategy', 
   'replication_factor' : 1 
  };
```

5. Create sample table
```
CREATE TABLE cycling.cyclist_teams (
  id uuid PRIMARY KEY,
  firstname text,
  lastname text,
  teams map<int, text>
);
```

6. Bind trigger with this table:
```
CREATE TRIGGER kafka_trigger ON cycling.cyclist_teams USING 'kz.tim.TriggerImpl';
```

7. Perform simple insert:
```
INSERT INTO cycling.cyclist_teams (
  id, firstname, lastname, teams
) VALUES (
  5b6962dd-3f90-4c93-8f61-eabfa4a803e2, 
  'Marianne',
  'VOS', 
  {
    2015 : 'Rabobank-Liv Woman Cycling Team', 
    2014 : 'Rabobank-Liv Woman Cycling Team'
  }
);
```

8. Connect to Kafka consumer and check update messages:
```
$ bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --from-beginning --topic test-topic
```

9. Profit :)

### Kafka record structure

Record key is the concatenation of the partition keys of the *Cassandra* partition and serialized by `StringSerializer`.

Record value also uses `StringSerializer`, content of the value is in *JSON* format and 
has the following structure:

Partition deleted message:
```
{
    "keyspaceName": "keyspace_name", 
    "columnFamilyName": "table_name",
    "partitionKeys": {
        "partition_key_name": "partition_key_value"
    },
    "isDeleted": true
}
```

Row inserted/updated/deleted message:
```
{
    "keyspaceName": "keyspace_name", 
    "columnFamilyName": "table_name",
    "partitionKeys": {
        "partition_key_name": "partition_key_value"
    },
    "eventType": "ROW",
    "row": {
        "action": "update", //[INSERT, UPDATE, DELETE]
        "clusteringKeys": {
            "clustering_key_name": "clustering_key_value"
        },
        "affectedCells": [
            {
                "name": "column_name",
                "value": "column_value",
                "type": "cell_type" //[UDT, LIST, MAP, SET, TUPLE, SIMPLE]
            },
            {
                "name": "another_column_name",
                "type": "SIMPLE",
                "isDeleted": true,
            },
            {
                "name": "other_name",
                "value": "column_value",
                "type": "SET",
                "deletedItems": []
            }
        ]
    }
}
```

Slice deletions with clustering keys:
Example table:
```
CREATE TABLE cycling.rank_by_year_and_name ( 
   race_year int, 
   race_name text, 
   cyclist_name text, 
   rank int, 
   PRIMARY KEY ((race_year, race_name), rank) );
```

CQL query;
```
delete from rank_by_year_and_name where rank > 1 and race_year = 10 and race_name = 'abc';
```

Since deletion if performed using clustering key `rank`, Kafka message will be following:
```
{
  "keyspaceName": "cycling",
  "columnFamilyName": "rank_by_year_and_name",
  "partitionKeys": {},
  "row": null,
  "rangeTombstoneMarker": {
    "bounds": [
      {
        "clusteringKey": {
          "rank": 1
        },
        "inclusive": false
      }
    ],
    "start": true
  },
  "eventType": "RANGE_TOMBSTONE_MARKER",
  "deleted": false
}
```

Description of the cell fields:

- **name** - string - name of the updated cell
- **deleted** - boolean - if cell deleted or not
- **value** - any - value of the cell
- **type** - enum - type of the cell:
    - *UDT* - for user-defined type, ex `avatar`, `media`
    - *LIST* - for lists, ex `['abc', 'def', 'abc']`
    - *MAP* - for maps, ex `{1: 'Peter', 2: 'John'}`
    - *SET* - for sets, ex `{1, 2, 3, 4}`
    - *TUPLE* - for tuples, ex `(3, 'bar', 2.1)`
    - *COUNTER* - for counters, ex `100`, `-50`
    - *SIMPLE* - for other primitive types, ex `'Timur'`, `123.42`, `true`, `500`
- **deletedItems** - set - contains deleted items, can be used only with `SET` and `MAP` cell types:
    - if cell type is *MAP*, then **deletedItems** will contain keys of the deleted objects. 
    For the following query: `UPDATE cycling.cyclist_teams SET teams = teams - {'2013','2014'} WHERE id=e7cd5752-bc0d-4157-a80f-7523add8dbcd;`, 
    **deletedItems** will contain `{'2013','2014'}`
    - if cell type is *SET*, then **deletedItems** will contain deleted objects. 
    For the following query: `UPDATE cycling.cyclist_career_teams SET teams = teams - {'WOMBATS'} WHERE id = 5b6962dd-3f90-4c93-8f61-eabfa4a803e2;`, 
    **deletedItems** will contain `{'WOMBATS'}`
    
### Range Tombstone Marker

From documentation:
> A range tombstone is a tombstone that covers a slice/range of rows.

Deletions where clustering keys are used with comparison operators (`>, >=, =, =<, <`), causes trigger to produce `RangeTombstoneMarker` events.

### Inspired by
- https://github.com/gradeup/cassandra-trigger
- https://github.com/smartcat-labs/cassandra-kafka-connector

### Useful links
- http://cassandra.apache.org/doc/4.0/cql/triggers.html
- https://pandaforme.gitbooks.io/introduction-to-cassandra/content/understand_the_cassandra_data_model.html
