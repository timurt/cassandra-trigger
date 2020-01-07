package kz.tim.models.rangetombstonemarker;

import java.util.Map;

/**
 * Represents bound.
 *
 * @author Timur Tibeyev.
 */
public class RangeTombstoneMarkerBound {
    private boolean isInclusive;
    private Map<String, Object> clusteringKey;

    public RangeTombstoneMarkerBound(Map<String, Object> clusteringKey, boolean isInclusive) {
        this.clusteringKey = clusteringKey;
        this.isInclusive = isInclusive;
    }

    public boolean isInclusive() {
        return isInclusive;
    }

    public void setInclusive(boolean inclusive) {
        isInclusive = inclusive;
    }

    public Map<String, Object> getClusteringKey() {
        return clusteringKey;
    }

    public void setClusteringKey(Map<String, Object> clusteringKey) {
        this.clusteringKey = clusteringKey;
    }
}
