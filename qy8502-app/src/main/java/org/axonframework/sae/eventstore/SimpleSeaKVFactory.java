package org.axonframework.sae.eventstore;

import com.sina.sae.kvdb.SaeKV;

public class SimpleSeaKVFactory implements SaeKVFactory {
	static SaeKV kv;

	public SimpleSeaKVFactory() {

	}

	public SaeKV getInstance() {
		if (kv == null) {
			kv = new SaeKV();
			kv.init();
		}
		return kv;
	}
}
