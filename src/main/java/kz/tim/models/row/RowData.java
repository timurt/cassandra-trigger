package kz.tim.models.row;

import kz.tim.models.row.cell.CellData;

import java.util.List;
import java.util.Map;

/**
 * Stores row modifications.
 *
 * @author Timur Tibeyev.
 */
public class RowData {
    private RowAction action;
    private Map<String, Object> clusteringKeys;
    private List<CellData> affectedCells;

    /**
     * Parametrized constructor.
     *
     * @param action row action.
     * @param clusteringKeys clustering keys of the row.
     * @param cells list of the affected cells.
     */
    public RowData(RowAction action, Map<String, Object> clusteringKeys, List<CellData> cells) {
        this.action = action;
        this.clusteringKeys = clusteringKeys;
        this.affectedCells = cells;
    }

    public RowAction getAction() {
        return action;
    }

    public void setAction(RowAction action) {
        this.action = action;
    }

    public Map<String, Object> getClusteringKeys() {
        return clusteringKeys;
    }

    public void setClusteringKeys(Map<String, Object> clusteringKeys) {
        this.clusteringKeys = clusteringKeys;
    }

    public List<CellData> getAffectedCells() {
        return affectedCells;
    }

    public void setAffectedCells(List<CellData> affectedCells) {
        this.affectedCells = affectedCells;
    }
}
