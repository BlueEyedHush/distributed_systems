package sr.ice.server;

import Ice.*;
import Ice.Object;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;


public class ServantLocator5 implements ServantLocator
{
	private static final Logger LOGGER = LoggerFactory.getLogger(ServantLocator5.class.getSimpleName());

	private final Supplier<? extends Object> servantSupplier;
	private final BiConsumer<Object, String> servantSerializer;
	private final Function<String, Object> servantDeserializer;

	// doesn't really need to be thread safe, but since that is what I have...
	private final ConcurrentLinkedHashMap<String, Object> inMemoryIdToServant;
	private final Set<String> idOfEvicted = new HashSet<>();

	public ServantLocator5(Supplier<? extends Object> servantSupplier,
						   BiConsumer<Object, String> servantSerializer,
						   Function<String, Object> servantDeserializer,
						   int inMemoryPoolSize)
	{
		if(inMemoryPoolSize < 2) throw new IllegalArgumentException("inMemoryPoolSize must be at least 2");

		this.servantSupplier = servantSupplier;
		this.servantSerializer = servantSerializer;
		this.servantDeserializer = servantDeserializer;

		ConcurrentLinkedHashMap.Builder<String, Object> builder = new ConcurrentLinkedHashMap.Builder<>();
		inMemoryIdToServant = builder
				.concurrencyLevel(1)
				.maximumWeightedCapacity(inMemoryPoolSize)
				.weigher(v -> 1)
				.listener((k,v) -> {
					LOGGER.info("evicting {}", k);
					servantSerializer.accept(v, k);
					idOfEvicted.add(k);
				})
				.build();

		LOGGER.info("created");
	}

	/**
	 * Contract: this method can result in
	 * - servant being returned
	 * - null being returned - cases client's runtime to raise ObjectNotExistExceptionm
	 * - exceptions being thrown - runtime propagates the exception to the client
	 *
	 * cookie - allows to pass arbitrary data between this function and finished()
     */
	public Object locate(Current curr, LocalObjectHolder cookie) throws UserException
	{
		LOGGER.info("locate {}", idToString(curr.id));
		String id = curr.id.name;
		Object servant;
		synchronized (this) {
			if (inMemoryIdToServant.containsKey(id)) {
				LOGGER.info("{} in memory, returning", id);
				servant = inMemoryIdToServant.get(id);
			} else if (idOfEvicted.contains(id)) {
				LOGGER.info("{} not present, needs to be deserialized", id);
				servant = servantDeserializer.apply(id);
				inMemoryIdToServant.put(id, servant); // eviction should happen automagically
			} else {
				LOGGER.info("creating servant for {}", id);
				servant = servantSupplier.get();
				inMemoryIdToServant.put(id, servant);
			}
		}

		return servant;
	}

	/**
	 * If locate has returned servant, this method will be called when request handling is finished
     */
	public void finished(Current curr, Object servant, java.lang.Object cookie) throws UserException
	{
		LOGGER.info("finished {}", idToString(curr.id));
	}

	/**
	 * called when service locator is no longer needed
     */
	public void deactivate(String category)
	{
		LOGGER.info("finished {}", category);
	}

	private static String idToString(Identity id) {
		return id.category + "/" + id.name;
	}
}
