package com.sinaapp.qy8502.app;

import org.axonframework.commandhandling.annotation.CommandHandler;
import org.axonframework.eventhandling.annotation.EventHandler;
import org.axonframework.eventsourcing.annotation.AbstractAnnotatedAggregateRoot;
import org.axonframework.eventsourcing.annotation.AggregateIdentifier;

public class ToDoItem extends AbstractAnnotatedAggregateRoot {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8172470036704264388L;
	@AggregateIdentifier
	private String id;

	public ToDoItem() {
	}

	@CommandHandler
	public ToDoItem(CreateToDoItemCommand command) {
		apply(new ToDoItemCreatedEvent(command.getTodoId(),
				command.getDescription()));
	}

	@CommandHandler
	public void markCompleted(MarkCompletedCommand command) {
		apply(new ToDoItemCompletedEvent(id));
	}

	@EventHandler
	public void on(ToDoItemCreatedEvent event) {
		this.id = event.getTodoId();
	}
}
