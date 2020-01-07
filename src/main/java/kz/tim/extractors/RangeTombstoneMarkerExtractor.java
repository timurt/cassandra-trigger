package kz.tim.extractors;

import kz.tim.models.rangetombstonemarker.RangeTombstoneMarkerBound;
import kz.tim.models.rangetombstonemarker.RangeTombstoneMarkerData;
import org.apache.cassandra.db.ClusteringBound;
import org.apache.cassandra.db.ClusteringPrefix;
import org.apache.cassandra.db.partitions.Partition;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Class responsible for extracting range tombstone marker information from update.
 *
 * @author Timur Tibeyev.
 */
public class RangeTombstoneMarkerExtractor {
    private Partition update;

    public RangeTombstoneMarkerExtractor(Partition update) {
        this.update = update;
    }

    /**
     * Extracts range tombstone marker modifications from the partition update.
     *
     * @param clustering holds values for the clustering columns of a table
     * @return range tombstone marker update
     */
    public RangeTombstoneMarkerData extract(ClusteringPrefix clustering) {
        ClusteringBound bound = (ClusteringBound) clustering;
        List<RangeTombstoneMarkerBound> bounds = new LinkedList<>();
        for (int i = 0; i < bound.size(); i++) {
            String columnName = update.metadata().clusteringColumns().get(i).name.toCQLString();
            Object columnValue = update.metadata().comparator.subtype(i).compose(bound.get(i));

            Map<String, Object> clusteringKey = new HashMap<>();
            clusteringKey.put(columnName, columnValue);

            RangeTombstoneMarkerBound rangeTombstoneMarkerData =
                    new RangeTombstoneMarkerBound(clusteringKey, bound.isInclusive());

            bounds.add(rangeTombstoneMarkerData);
        }

        return new RangeTombstoneMarkerData(bounds, bound.kind().isStart());
    }
}
