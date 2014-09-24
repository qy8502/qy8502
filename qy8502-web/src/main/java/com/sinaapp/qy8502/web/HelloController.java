package com.sinaapp.qy8502.web;

import java.util.Map;
import java.util.UUID;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.sae.eventstore.SaeKVFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sinaapp.qy8502.app.CreateToDoItemCommand;
import com.sinaapp.qy8502.app.MarkCompletedCommand;

@Controller
public class HelloController {
	@Autowired
	private CommandGateway commandGateway;

	private static Logger logger = LoggerFactory
			.getLogger(HelloController.class);

	@RequestMapping(value = "/hello", method = RequestMethod.GET)
	public @ResponseBody
	String hello() {
		logger.warn("Hello World!");
		return "Hello World!";
	}

	@Autowired
	public SaeKVFactory kvFactory;

	@RequestMapping(value = "/kvdb", method = RequestMethod.GET)
	public @ResponseBody
	String kvdb() {
		logger.warn("kvdb!");
		kvFactory.getInstance().set("kvdb", 123);
		String str = "kvdb " + kvFactory.getInstance().get("kvdb").toString();
		kvFactory.getInstance().delete("kvdb");
		return str;
	}

	@RequestMapping(value = "/kvdb/put/{key}", method = RequestMethod.GET)
	public @ResponseBody
	String kvdbPut(@PathVariable String key) {
		kvFactory.getInstance().set(key, 123);
		return key;
	}

	@RequestMapping(value = "/kvdb/pk/{prefix}/{compare}", method = RequestMethod.GET)
	public @ResponseBody
	String kvdbPK2(@PathVariable String prefix, @PathVariable String compare) {
		Map kvmap = kvFactory.getInstance().pkrget(prefix, compare);
		String str = "seach '" + prefix + "' afert '" + compare
				+ "' , count : " + kvmap.size();
		return str;
	}

	@RequestMapping(value = "/kvdb/pk/{prefix}", method = RequestMethod.GET)
	public @ResponseBody
	String kvdbPK(@PathVariable String prefix) {
		return kvdbPK2(prefix, "");
	}

	@RequestMapping(value = "/todo", method = RequestMethod.POST)
	public @ResponseBody
	String createToDoItem(@RequestBody String body) {
		String itemId = UUID.randomUUID().toString();
		commandGateway.send(new CreateToDoItemCommand(itemId, body));
		return itemId;
	}

	@RequestMapping(value = "/todo/{itemId}/completed", method = RequestMethod.PUT)
	public @ResponseBody
	void markCompleted(@PathVariable String itemId) {
		Assert.hasLength(itemId, "Wrong todo itemId!");
		commandGateway.send(new MarkCompletedCommand(itemId));
	}

}
