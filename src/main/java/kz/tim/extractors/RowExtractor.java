package kz.tim.extractors;

import kz.tim.models.row.RowAction;
import kz.tim.models.row.RowData;
import kz.tim.models.row.cell.CellData;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.ClusteringPrefix;
import org.apache.cassandra.db.partitions.Partition;
import org.apache.cassandra.db.rows.Row;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class responsible for extracting row information from update.
 *
 * @author Timur Tibeyev.
 */
public class RowExtractor {
    private Partition update;
    private CellExtractor cellExtractor;

    public RowExtractor(Partition update) {
        this.update = update;
        cellExtractor = new CellExtractor(update);
    }

    /**
     * Extracts row modifications from the partition update.
     *
     * @param clustering holds values for the clustering columns of a table
     * @return row update
     */
    public RowData extract(ClusteringPrefix clustering) {
        Map<String, Object> clusteringKeys = new TreeMap<>();
        for (int i = 0; i < clustering.size(); i++) {
            String columnName = update.metadata().clusteringColumns().get(i).name.toCQLString();
            Object columnValue = update.metadata().clusteringColumns().get(i)
                    .type.compose(clustering.get(i));

            clusteringKeys.put(columnName, columnValue);
        }

        Row row = update.getRow((Clustering) clustering);
        List<CellData> cells = cellExtractor.extractCells(row);

        RowAction action;
        if (rowIsDeleted(row)) {
            action = RowAction.DELETE;
        } else
        if (rowIsInserted(row)) {
            action = RowAction.INSERT;
        } else {
            action = RowAction.UPDATE;
        }

        return new RowData(action, clusteringKeys, cells);
    }

    private boolean rowIsInserted(Row row) {
        return row.deletion().time().markedForDeleteAt() > Long.MIN_VALUE;
    }

    private boolean rowIsDeleted(Row row) {
        return update.partitionLevelDeletion().markedForDeleteAt() > Long.MIN_VALUE;
    }
}
