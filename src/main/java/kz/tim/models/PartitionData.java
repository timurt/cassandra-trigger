package kz.tim.models;

import kz.tim.models.rangetombstonemarker.RangeTombstoneMarkerData;
import kz.tim.models.row.RowData;

import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Class stores all data regarding partition update.
 *
 * @author Timur Tibeyev.
 */
public class PartitionData {
    private boolean isDeleted = false;
    private List<RowData> rows = new LinkedList<>();
    private List<RangeTombstoneMarkerData> rangeTombstoneMarkers = new LinkedList<>();
    private SortedMap<String, Object> partitionKeys = new TreeMap<>();

    public SortedMap<String, Object> getPartitionKeys() {
        return partitionKeys;
    }

    public void setPartitionKeys(SortedMap<String, Object> partitionKeys) {
        this.partitionKeys = partitionKeys;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public List<RowData> getRows() {
        return rows;
    }

    public void setRows(List<RowData> rows) {
        this.rows = rows;
    }

    public List<RangeTombstoneMarkerData> getRangeTombstoneMarkers() {
        return rangeTombstoneMarkers;
    }

    public void setRangeTombstoneMarkers(List<RangeTombstoneMarkerData> rangeTombstoneMarkers) {
        this.rangeTombstoneMarkers = rangeTombstoneMarkers;
    }
}
