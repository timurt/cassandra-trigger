package kz.tim.models.row.cell;

/**
 * Stores cell modifications.
 *
 * @author Timur Tibeyev.
 */
public class CellData {
    private String name;
    private Object value;
    private CellType type;
    private boolean isDeleted = false;
    private Object deletedItems;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public CellType getType() {
        return type;
    }

    public void setType(CellType type) {
        this.type = type;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public Object getDeletedItems() {
        return deletedItems;
    }

    public void setDeletedItems(Object deletedItems) {
        this.deletedItems = deletedItems;
    }
}
