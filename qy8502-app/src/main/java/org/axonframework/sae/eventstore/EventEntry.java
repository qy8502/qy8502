package org.axonframework.sae.eventstore;

import static org.axonframework.serializer.MessageSerializer.serializeMetaData;
import static org.axonframework.serializer.MessageSerializer.serializePayload;
import static org.axonframework.upcasting.UpcastUtils.upcastAndDeserialize;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.axonframework.domain.DomainEventMessage;
import org.axonframework.serializer.SerializedDomainEventData;
import org.axonframework.serializer.SerializedMetaData;
import org.axonframework.serializer.SerializedObject;
import org.axonframework.serializer.Serializer;
import org.axonframework.serializer.SimpleSerializedObject;
import org.axonframework.upcasting.UpcasterChain;
import org.joda.time.DateTime;

public class EventEntry implements SerializedDomainEventData<byte[]> {

	private static final String AGGREGATE_IDENTIFIER = "aggregateIdentifier";
	private static final String SEQUENCE_NUMBER = "sequenceNumber";
	private static final String SERIALIZED_EVENT = "serializedEvent";
	private static final String TIME_STAMP = "timeStamp";
	private static final String EVENT_TYPE = "eventType";
	private static final String EVENT_REVISION = "eventRevision";
	private static final String META_DATA = "metaData";
	private static final String AGGREGATE_TYPE = "aggregateType";

	private final String eventIdentifier;
	private final String aggregateIdentifier;
	private final long sequenceNumber;
	private final String timeStamp;
	private final String aggregateType;
	private final byte[] serializedEvent;
	private final String eventRevision;
	private final String eventType;
	private final byte[] serializedMetaData;

	/**
	 * Constructor used to create a new event entry to store in Mongo
	 * 
	 * @param aggregateType
	 *            String containing the aggregate type of the event
	 * @param event
	 *            The actual DomainEvent to store
	 * @param serializer
	 *            Serializer to use for the event to store
	 */
	EventEntry(String aggregateType, DomainEventMessage event,
			Serializer serializer) {

		this.eventIdentifier = event.getIdentifier();
		this.aggregateType = aggregateType;
		this.aggregateIdentifier = event.getAggregateIdentifier().toString();
		this.sequenceNumber = event.getSequenceNumber();

		SerializedObject<byte[]> serializedPayload = serializePayload(event,
				serializer, byte[].class);
		SerializedObject<byte[]> serializedMetaData = serializeMetaData(event,
				serializer, byte[].class);

		this.serializedEvent = serializedPayload.getData();
		this.eventType = serializedPayload.getType().getName();
		this.eventRevision = serializedPayload.getType().getRevision();
		this.serializedMetaData = serializedMetaData.getData();
		this.timeStamp = event.getTimestamp().toString();
	}

	/**
	 * Reconstruct an EventEntry based on the given <code>entity</code>, which
	 * contains the k
	 * 
	 * @param entity
	 *            the entity containing the fields to build the entry with
	 * @throws IOException
	 */
	EventEntry(byte[] entity) throws IOException {
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
				entity);
		ObjectInputStream in = new ObjectInputStream(byteArrayInputStream);
		//in.readByte();
		this.eventIdentifier = in.readUTF();
		this.aggregateIdentifier = in.readUTF();
		this.aggregateType = in.readUTF();
		this.sequenceNumber = in.readLong();
		this.timeStamp = in.readUTF();

		this.eventType = in.readUTF();
		this.eventRevision = in.readUTF();
		this.serializedEvent = new byte[in.readInt()];
		in.readFully(this.serializedEvent);

		int metaDataSize = in.readInt();
		this.serializedMetaData = new byte[metaDataSize];
		in.readFully(this.serializedMetaData);

	}

	/**
	 * Returns the actual DomainEvent from the EventEntry using the provided
	 * Serializer.
	 * 
	 * @param actualAggregateIdentifier
	 *            The actual aggregate identifier instance used to perform the
	 *            lookup
	 * @param serializer
	 *            Serializer used to de-serialize the stored DomainEvent
	 * @param upcasterChain
	 *            Set of upcasters to use when an event needs upcasting before
	 *            de-serialization
	 * @param skipUnknownTypes
	 *            Whether to skip unknown event types
	 * @return The actual DomainEventMessage instances stored in this entry
	 */
	@SuppressWarnings("unchecked")
	public List<DomainEventMessage> getDomainEvent(
			Object actualAggregateIdentifier, Serializer serializer,
			UpcasterChain upcasterChain, boolean skipUnknownTypes) {
		return upcastAndDeserialize(this, actualAggregateIdentifier,
				serializer, upcasterChain, skipUnknownTypes);
	}

	@Override
	public String getEventIdentifier() {
		return eventIdentifier;
	}

	@Override
	public Object getAggregateIdentifier() {
		return aggregateIdentifier;
	}

	/**
	 * getter for the sequence number of the event.
	 * 
	 * @return long representing the sequence number of the event
	 */
	@Override
	public long getSequenceNumber() {
		return sequenceNumber;
	}

	@Override
	public DateTime getTimestamp() {
		return new DateTime(timeStamp);
	}

	@Override
	public SerializedObject<byte[]> getMetaData() {
		return new SerializedMetaData<byte[]>(serializedMetaData, byte[].class);
	}

	@Override
	public SerializedObject<byte[]> getPayload() {
		return new SimpleSerializedObject<byte[]>(serializedEvent,
				byte[].class, eventType, eventRevision);
	}

	public byte[] asByteArray() throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(byteArrayOutputStream);

		// type byte for future use
		//out.write(0);
		out.writeUTF(this.eventIdentifier);
		out.writeUTF(this.aggregateIdentifier);
		out.writeUTF(this.aggregateType);
		out.writeLong(this.sequenceNumber);
		out.writeUTF(this.timeStamp);

		out.writeUTF(this.eventType);
		out.writeUTF(this.eventRevision == null ? "" : this.eventRevision);
		out.writeInt(this.serializedEvent.length);
		out.write(this.serializedEvent);
		out.writeInt(this.serializedMetaData.length);
		out.write(this.serializedMetaData);
		out.flush();
		return byteArrayOutputStream.toByteArray();
	}


}
