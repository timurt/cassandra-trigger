package kz.tim;

import io.confluent.kafka.serializers.KafkaJsonSerializer;
import kz.tim.models.event.Event;
import kz.tim.models.event.EventType;
import kz.tim.models.rangetombstonemarker.RangeTombstoneMarkerData;
import kz.tim.models.row.RowData;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class responsible for creating connection with Kafka and producing messages.
 *
 * @author Timur Tibeyev.
 */
public class KafkaSender {
    private Producer<String, Event> producer;
    private static final String KEY_SEPARATOR = "#";
    private static final String PARTITION_AND_CLUSTERING_KEYS_SEPARATOR = "$";

    public KafkaSender() {
        producer = createProducer();
    }

    /**
     * Send message to Kafka, that contains information about modified rows and cells.
     *
     * @param event event.
     */
    public void sendRowsEvents(Event event, List<RowData> rows) {
        event.setEventType(EventType.ROW);
        for (RowData rowData: rows) {
            String recordKey = getKeyFromList(event.getPartitionKeys());
            if (!rowData.getClusteringKeys().isEmpty()) {
               recordKey += PARTITION_AND_CLUSTERING_KEYS_SEPARATOR
                       + getKeyFromList(rowData.getClusteringKeys());
            }
            event.setRow(rowData);
            send(recordKey, event);
        }
    }

    /**
     * Send message to Kafka, that contains information about deletion slices.
     *
     * @param event event.
     */
    public void sendRangeTombstoneMarkersEvents(
            Event event,
            List<RangeTombstoneMarkerData> rangeTombstoneMarkers
    ) {
        event.setEventType(EventType.RANGE_TOMBSTONE_MARKER);
        for (RangeTombstoneMarkerData rangeTombstoneMarkerData: rangeTombstoneMarkers) {
            String recordKey = getKeyFromList(event.getPartitionKeys());

            event.setRangeTombstoneMarker(rangeTombstoneMarkerData);
            send(recordKey, event);
        }
    }

    /**
     * Send message to Kafka, that contains information about deleted partition.
     *
     * @param event event.
     */
    public void sendDeletedPartitionEvent(Event event) {
        event.setDeleted(true);
        String recordKey = getKeyFromList(event.getPartitionKeys());
        send(recordKey, event);
    }

    private void send(String key, Event value) {
        ProducerRecord record = new ProducerRecord(TriggerConfig.TOPIC_NAME, key, value);
        producer.send(record);
    }

    private Producer<String, Event> createProducer() {
        Map<String, Object> configuration = new HashMap<>();
        configuration.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, TriggerConfig.BOOTSTRAP_SERVERS);

        Serializer keySerializer = getKeySerializer(configuration);
        KafkaJsonSerializer valueSerializer = getValueSerializer(configuration);

        return new KafkaProducer<String, Event>(configuration, keySerializer, valueSerializer);
    }

    private Serializer<String> getKeySerializer(Map<String, Object> configuration) {
        Serializer serializer = new StringSerializer();
        serializer.configure(configuration, true);
        return serializer;
    }

    private KafkaJsonSerializer<Event> getValueSerializer(Map<String, Object> configuration) {
        KafkaJsonSerializer serializer = new KafkaJsonSerializer<Event>();
        serializer.configure(configuration, false);
        return serializer;
    }

    private String getKeyFromList(Map<String, Object> map) {
        return map.values().stream().map(Object::toString)
                .collect(Collectors.joining(KEY_SEPARATOR));
    }
}
