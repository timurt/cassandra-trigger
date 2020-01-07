package kz.tim.models.event;

/**
 * Type of the produced event.
 * If it is {@link #ROW}, then event is about row update.
 * If it is {@link #RANGE_TOMBSTONE_MARKER}, then event is about
 * row range tombstone marker (see README.MD).
 *
 * @author Timur Tibeyev.
 */
public enum EventType {
    ROW, RANGE_TOMBSTONE_MARKER
}
