package org.axonframework.sae.eventstore;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.axonframework.domain.DomainEventMessage;
import org.axonframework.domain.DomainEventStream;
import org.axonframework.domain.SimpleDomainEventStream;
import org.axonframework.eventstore.EventStreamNotFoundException;
import org.axonframework.eventstore.SnapshotEventStore;
import org.axonframework.serializer.Serializer;
import org.axonframework.serializer.xml.XStreamSerializer;
import org.axonframework.upcasting.SimpleUpcasterChain;
import org.axonframework.upcasting.UpcasterAware;
import org.axonframework.upcasting.UpcasterChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KVDBEventStore implements SnapshotEventStore, UpcasterAware {

	private static final Logger logger = LoggerFactory
			.getLogger(KVDBEventStore.class);

	private final SaeKVFactory factory;

	private final Serializer eventSerializer;
	private UpcasterChain upcasterChain = SimpleUpcasterChain.EMPTY;

	private static final String DEFAULT_DOMAINEVENTS_PREFIX = "domainevents";
	private static final String DEFAULT_SNAPSHOTEVENTS_PREFIX = "snapshotevents";
	private static final String DEFAULT_SEPARATOR = "_";

	private final String domainEventsPrefix;
	private final String snapshotEventsPrefix;
	private final String separator;

	public KVDBEventStore(Serializer eventSerializer, SaeKVFactory factory,
			String domainEventsPrefix, String snapshotEventsPrefix,
			String separator) {
		this.eventSerializer = eventSerializer;
		this.factory = factory;
		this.domainEventsPrefix = domainEventsPrefix;
		this.snapshotEventsPrefix = snapshotEventsPrefix;
		this.separator = separator;
	}

	public KVDBEventStore(Serializer eventSerializer, SaeKVFactory factory) {
		this(eventSerializer, factory, DEFAULT_DOMAINEVENTS_PREFIX,
				DEFAULT_SNAPSHOTEVENTS_PREFIX, DEFAULT_SEPARATOR);
	}

	public KVDBEventStore(SaeKVFactory factory) {
		this(new XStreamSerializer(), factory);
	}

	public KVDBEventStore(String domainEventsPrefix,
			String snapshotEventsPrefix, String separator) {
		this(new XStreamSerializer(), new SimpleSeaKVFactory(),
				domainEventsPrefix, snapshotEventsPrefix, separator);
	}

	public KVDBEventStore(Serializer eventSerializer) {
		this(eventSerializer, new SimpleSeaKVFactory(),
				DEFAULT_DOMAINEVENTS_PREFIX, DEFAULT_SNAPSHOTEVENTS_PREFIX,
				DEFAULT_SEPARATOR);
	}

	public KVDBEventStore() {
		this(new XStreamSerializer(), new SimpleSeaKVFactory());
	}

	public KVDBEventStore(SaeKVFactory factory, String domainEventsPrefix,
			String snapshotEventsPrefix, String separator) {
		this(new XStreamSerializer(), factory, domainEventsPrefix,
				snapshotEventsPrefix, separator);
	}

	@Override
	public void appendEvents(String type, DomainEventStream events) {
		while (events.hasNext()) {
			DomainEventMessage event = events.next();

			EventEntry entry = new EventEntry(type, event, eventSerializer);
			String key = makeDomainKey(type, event.getAggregateIdentifier()
					.toString(), event.getSequenceNumber());
			try {
				factory.getInstance().set(key, entry.asByteArray());
				logger.warn("stored  event to kvdb, key : " + key + " , sn: "
						+ entry.getSequenceNumber());
			} catch (IOException e) {
				logger.error("store event to kvdb failed, key : " + key, e);
			}

		}
	}

	@Override
	public DomainEventStream readEvents(String type, Object identifier) {
		long snapshotSequenceNumber = -1;
		EventEntry lastSnapshotEvent = loadLastSnapshotEvent(type, identifier);
		if (lastSnapshotEvent != null) {
			snapshotSequenceNumber = lastSnapshotEvent.getSequenceNumber();
		}

		List<DomainEventMessage> events = readEventSegmentInternal(type,
				identifier, snapshotSequenceNumber + 1);
		if (lastSnapshotEvent != null) {
			events.addAll(0, lastSnapshotEvent.getDomainEvent(identifier,
					eventSerializer, upcasterChain, false));
		}

		if (events.isEmpty()) {
			throw new EventStreamNotFoundException(type, identifier);
		}

		return new SimpleDomainEventStream(events);
	}

	@Override
	public void appendSnapshotEvent(String type,
			DomainEventMessage snapshotEvent) {

		EventEntry entry = new EventEntry(type, snapshotEvent, eventSerializer);
		String key = makeSnapshotKey(type, snapshotEvent
				.getAggregateIdentifier().toString(),
				snapshotEvent.getSequenceNumber());
		try {
			factory.getInstance().set(key, entry.asByteArray());
			logger.warn("stored snapshot event to kvdb, key : " + key
					+ " , sn: " + snapshotEvent.getSequenceNumber());
		} catch (IOException e) {
			logger.error("store snapshot event to kvdb failed, key : " + key, e);
		}
	}

	private List<DomainEventMessage> readEventSegmentInternal(String type,
			Object identifier, long firstSequenceNumber) {

		String prefix = makeDomainKeyPrefix(type, identifier.toString());
		String compare = prefix;
		Map<String, Object> kvMap;

		if (firstSequenceNumber > 0) {
			compare = makeDomainKey(type, identifier.toString(),
					firstSequenceNumber - 1);
		}
		kvMap = factory.getInstance().pkrget(prefix, compare);

		logger.warn("getting event from kvdb, prefix : " + prefix
				+ ", compare : " + compare + ", count : " + kvMap.size());

		TreeMap<Long, EventEntry> eventEntryMap = new TreeMap<Long, EventEntry>();

		Iterator<Object> kvIterator = kvMap.values().iterator();

		// sort the entry from kvdb
		while (kvIterator.hasNext()) {
			try {
				EventEntry eventEntry = new EventEntry(
						(byte[]) kvIterator.next());

				eventEntryMap.put(eventEntry.getSequenceNumber(), eventEntry);

			} catch (IOException e) {
				logger.error("get event from kvdb failed, prefix : " + prefix
						+ ", compare : " + compare, e);
			}

		}

		List<DomainEventMessage> events = new ArrayList<DomainEventMessage>(
				eventEntryMap.size());

		Iterator<EventEntry> entityIterator = eventEntryMap.values().iterator();
		while (entityIterator.hasNext()) {
			EventEntry eventEntry = entityIterator.next();
			events.addAll(eventEntry.getDomainEvent(identifier,
					eventSerializer, upcasterChain, false));
			logger.warn("got event from kvdb, prefix : " + prefix
					+ ", compare : " + compare + " , sn: "
					+ eventEntry.getSequenceNumber());
		}
		return events;
	}

	private EventEntry loadLastSnapshotEvent(String type, Object identifier) {
		String prefix = makeSnapshotKeyPrefix(type, identifier.toString());
		Map<String, Object> kvMap = factory.getInstance().pkrget(prefix, 1,
				prefix);
		logger.warn("getting snapshot event from kvdb, prefix : " + prefix
				+ ", count : " + kvMap.size());
		Iterator<Object> entityIterator = kvMap.values().iterator();
		if (entityIterator.hasNext()) {
			try {
				EventEntry eventEntry = new EventEntry(
						(byte[]) entityIterator.next());
				logger.warn("got snapshot event from kvdb, prefix : " + prefix
						+ " , sn: " + eventEntry.getSequenceNumber());
				return eventEntry;
			} catch (IOException e) {
				logger.error("get snapshot event from kvdb failed, prefix : "
						+ prefix, e);
			}
		}
		return null;
	}

	@Override
	public void setUpcasterChain(UpcasterChain upcasterChain) {
		this.upcasterChain = upcasterChain;
	}

	private String makeDomainKey(String type, String aggregateIdentifier,
			long firstSequenceNumber) {
		return makeDomainKeyPrefix(type, aggregateIdentifier)
				+ SequenceNumberToString(firstSequenceNumber);
	}

	private String makeDomainKeyPrefix(String type, String aggregateIdentifier) {
		return domainEventsPrefix + separator + type + separator
				+ aggregateIdentifier + separator;
	}

	private String makeSnapshotKey(String type, String aggregateIdentifier,
			long firstSequenceNumber) {
		return makeSnapshotKeyPrefix(type, aggregateIdentifier)
				+ SequenceNumberToString(Long.MAX_VALUE - firstSequenceNumber);
	}

	private String makeSnapshotKeyPrefix(String type, String aggregateIdentifier) {
		return snapshotEventsPrefix + separator + type + separator
				+ aggregateIdentifier + separator;
	}

	private String SequenceNumberToString(long sequenceNumber) {
		byte b[] = new byte[8];
		ByteBuffer bb = ByteBuffer.wrap(b);
		// by default BB is big endian like we need
		bb.putLong(sequenceNumber);

		StringBuilder buf = new StringBuilder(16);

		for (int i = 0; i < b.length; i++) {
			int x = b[i] & 0xFF;
			String s = Integer.toHexString(x);
			if (s.length() == 1)
				buf.append("0");
			buf.append(s);
		}

		return buf.toString();
	}
}