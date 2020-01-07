package kz.tim.models.rangetombstonemarker;

import java.util.List;

/**
 * Stores bounds for range tombstone markers (see README.MD).
 *
 * @author Timur Tibeyev.
 */
public class RangeTombstoneMarkerData {
    private List<RangeTombstoneMarkerBound> bounds;
    private boolean isStart;

    public RangeTombstoneMarkerData(List<RangeTombstoneMarkerBound> bounds, boolean isStart) {
        this.bounds = bounds;
        this.isStart = isStart;
    }

    public List<RangeTombstoneMarkerBound> getBounds() {
        return bounds;
    }

    public void setBounds(List<RangeTombstoneMarkerBound> bounds) {
        this.bounds = bounds;
    }

    public boolean isStart() {
        return isStart;
    }

    public void setStart(boolean start) {
        isStart = start;
    }
}
