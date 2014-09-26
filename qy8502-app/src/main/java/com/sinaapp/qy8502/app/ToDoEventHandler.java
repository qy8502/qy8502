package com.sinaapp.qy8502.app;

import org.axonframework.eventhandling.annotation.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToDoEventHandler {

	private static Logger logger = LoggerFactory
			.getLogger(ToDoEventHandler.class);

	@EventHandler
	public void handle(ToDoItemCreatedEvent event) {
		logger.warn("We've got something to do: " + event.getDescription()
				+ " (" + event.getTodoId() + ")");
	}

	@EventHandler
	public void handle(ToDoItemCompletedEvent event) {
		logger.warn("We've completed a task: " + event.getTodoId());
	}
}
