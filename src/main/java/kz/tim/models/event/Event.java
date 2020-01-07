package kz.tim.models.event;

import kz.tim.models.rangetombstonemarker.RangeTombstoneMarkerData;
import kz.tim.models.row.RowData;

import java.util.SortedMap;

/**
 * Class stores all data, which will be produced to Kafka.
 *
 * @author Timur Tibeyev.
 */
public class Event {
    private String keyspaceName;
    private String columnFamilyName;
    private SortedMap<String, Object> partitionKeys;
    private RowData row;
    private RangeTombstoneMarkerData rangeTombstoneMarker;
    private boolean isDeleted;
    private EventType eventType;

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    /**
     * Parametrized constructor.
     *
     * @param ksName keyspace name.
     * @param cfName column family name.
     * @param partitionKeys partition keys.
     */
    public Event(String ksName, String cfName, SortedMap<String, Object> partitionKeys) {
        this.keyspaceName = ksName;
        this.columnFamilyName = cfName;
        this.partitionKeys = partitionKeys;
    }

    public RangeTombstoneMarkerData getRangeTombstoneMarker() {
        return rangeTombstoneMarker;
    }

    public void setRangeTombstoneMarker(RangeTombstoneMarkerData rangeTombstoneMarker) {
        this.rangeTombstoneMarker = rangeTombstoneMarker;
    }

    public String getKeyspaceName() {
        return keyspaceName;
    }

    public void setKeyspaceName(String keyspaceName) {
        this.keyspaceName = keyspaceName;
    }

    public String getColumnFamilyName() {
        return columnFamilyName;
    }

    public void setColumnFamilyName(String columnFamilyName) {
        this.columnFamilyName = columnFamilyName;
    }

    public SortedMap<String, Object> getPartitionKeys() {
        return partitionKeys;
    }

    public void setPartitionKeys(SortedMap<String, Object> partitionKeys) {
        this.partitionKeys = partitionKeys;
    }

    public RowData getRow() {
        return row;
    }

    public void setRow(RowData row) {
        this.row = row;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }
}
