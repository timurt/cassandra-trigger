package kz.tim.extractors;

import kz.tim.models.PartitionData;

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.CompositeType;
import org.apache.cassandra.db.partitions.Partition;
import org.apache.cassandra.db.rows.Unfiltered;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Class responsible for extracting all information regarding partition update.
 *
 * @author Timur Tibeyev.
 */
public class DataExtractor {
    private RowExtractor rowExtractor;
    private RangeTombstoneMarkerExtractor rangeTombstoneMarkerExtractor;
    private Partition update;

    /**
     * Parametrized constructor.
     *
     * @param update partition update.
     */
    public DataExtractor(Partition update) {
        this.update = update;
        rowExtractor = new RowExtractor(update);
        rangeTombstoneMarkerExtractor = new RangeTombstoneMarkerExtractor(update);
    }

    /**
     * Extracts partition modifications from the {@link Partition} object.
     *
     * @return partition updates.
     */
    public PartitionData extract() {
        PartitionData result = new PartitionData();

        UnfilteredRowIterator iterator = update.unfilteredIterator();
        while (iterator.hasNext()) {
            Unfiltered current = iterator.next();
            if (current.isRow()) {
                result.getRows().add(rowExtractor.extract(current.clustering()));
            } else if (current.isRangeTombstoneMarker()) {
                result.getRangeTombstoneMarkers()
                        .add(rangeTombstoneMarkerExtractor.extract(current.clustering()));
            }
        }
        return result;
    }

    private SortedMap<String, Object> extractPartitionKeys() {
        ByteBuffer key = update.partitionKey().getKey();
        CFMetaData metadata = update.metadata();
        SortedMap<String, Object> partitionKeyData = new TreeMap<>();

        List<ColumnDefinition> partitionKeyList = metadata.partitionKeyColumns();
        if (partitionKeyList.size() == 1) {

            AbstractType columnType = partitionKeyList.get(0).type;
            String columnName = partitionKeyList.get(0).name.toString();
            Object columnValue = columnType.compose(key);

            if (columnValue != null) {
                partitionKeyData.put(columnName, columnValue);
            }
        } else {
            for (int i = 0; i < partitionKeyList.size(); i++) {
                String columnName = partitionKeyList.get(i).name.toString();
                AbstractType columnType = partitionKeyList.get(i).type;

                ByteBuffer tempKey = CompositeType.extractComponent(key, i);
                Object columnValue = columnType.compose(tempKey);

                if (columnValue != null) {
                    partitionKeyData.put(columnName, columnValue);
                }
            }
        }

        return partitionKeyData;
    }
}
