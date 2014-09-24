package org.axonframework.sae.eventstore;

import com.sina.sae.kvdb.SaeKV;

public interface SaeKVFactory {
	public SaeKV getInstance();
}
