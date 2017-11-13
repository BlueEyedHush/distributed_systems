package sr.ice.server;

import Ice.*;
import Ice.Object;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;


public class ServantLocator3 implements ServantLocator
{
	private static final Logger LOGGER = LoggerFactory.getLogger(ServantLocator3.class.getSimpleName());

	private final BlockingQueue<Object> servants;

	public ServantLocator3(Supplier<? extends Object> servantSupplier, int poolSize)
	{
		servants = new LinkedBlockingQueue<>(poolSize);
		for(int i = 0; i < poolSize; i++) servants.add(servantSupplier.get());
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
		Object servant = null;
		try {
			servant = servants.take();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		LOGGER.info("servant for {} became avaliable", idToString(curr.id));
		return servant;
	}

	/**
	 * If locate has returned servant, this method will be called when request handling is finished
     */
	public void finished(Current curr, Object servant, java.lang.Object cookie) throws UserException
	{
		LOGGER.info("finished {} - returning servant back to pool", idToString(curr.id));
		servants.add(servant); // this should never throw exception, since there should never be more servants than queue capacity
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
