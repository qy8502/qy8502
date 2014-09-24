package com.sinaapp.qy8502.app;

import java.io.Serializable;

public class ToDoItemCreatedEvent implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 142140616619044681L;
	private final String todoId;
	private final String description;

	public ToDoItemCreatedEvent(String todoId, String description) {
		this.todoId = todoId;
		this.description = description;
	}

	public String getTodoId() {
		return todoId;
	}

	public String getDescription() {
		return description;
	}
}
