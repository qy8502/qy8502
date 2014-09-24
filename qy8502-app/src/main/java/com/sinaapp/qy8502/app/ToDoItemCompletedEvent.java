package com.sinaapp.qy8502.app;

import java.io.Serializable;

public class ToDoItemCompletedEvent implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4913990589468975427L;
	private final String todoId;

	public ToDoItemCompletedEvent(String todoId) {
		this.todoId = todoId;
	}

	public String getTodoId() {
		return todoId;
	}
}
