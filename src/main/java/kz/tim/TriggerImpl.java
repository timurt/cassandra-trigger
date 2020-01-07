package kz.tim;

import kz.tim.models.event.Event;
import kz.tim.models.PartitionData;
import kz.tim.extractors.DataExtractor;
import org.apache.cassandra.db.Mutation;
import org.apache.cassandra.db.partitions.Partition;
import org.apache.cassandra.triggers.ITrigger;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Main class.
 *
 * @author Timur Tibeyev.
 */
public class TriggerImpl implements ITrigger {

    private Logger logger = Logger.getLogger(TriggerImpl.class);
    private KafkaSender kafkaSender;

    public TriggerImpl() {
        logger.info("Cassandra to Kafka trigger successfully initialized");
        kafkaSender = new KafkaSender();
    }

    @Override
    public Collection<Mutation> augment(Partition update) {
        logger.info("Partition update received");
        try {
            process(update);
            logger.info("Processing partition update finished");
        } catch (Exception e) {
            logger.error("Processing partition update failed", e);
        }
        return new ArrayList<>();
    }

    private void process(Partition update) {
        PartitionData partitionData = new DataExtractor(update).extract();
        Event event = new Event(update.metadata().ksName, update.metadata().cfName,
                partitionData.getPartitionKeys());

        if (partitionData.isDeleted()) {
            kafkaSender.sendDeletedPartitionEvent(event);
        }
        if (!partitionData.getRows().isEmpty()) {
            kafkaSender.sendRowsEvents(event, partitionData.getRows());
        }
        if (!partitionData.getRangeTombstoneMarkers().isEmpty()) {
            kafkaSender.sendRangeTombstoneMarkersEvents(event,
                    partitionData.getRangeTombstoneMarkers());
        }
    }
}
