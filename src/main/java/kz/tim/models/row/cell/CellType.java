package kz.tim.models.row.cell;

/**
 * Cell type. Refers to Cassandra data types.
 *
 * @author Timur Tibeyev.
 */
public enum CellType {
    UDT, LIST, MAP, SET, TUPLE, SIMPLE, COUNTER
}
