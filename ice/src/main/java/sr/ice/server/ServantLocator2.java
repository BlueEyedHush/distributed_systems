package sr.ice.server;

import Ice.*;
import Ice.Object;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;
import java.util.logging.*;


public class ServantLocator2 implements ServantLocator
{
	private static final Logger LOGGER = LoggerFactory.getLogger(ServantLocator2.class.getSimpleName());

	private final Supplier<? extends Object> servantSupplier;

	public ServantLocator2(Supplier<? extends Object> servantSupplier)
	{
		this.servantSupplier = servantSupplier;
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
		LOGGER.info("locate {}/{}", curr.id.category, curr.id.name);
		return servantSupplier.get();
	}

	/**
	 * If locate has returned servant, this method will be called when request handling is finished
     */
	public void finished(Current curr, Object servant, java.lang.Object cookie) throws UserException
	{
		LOGGER.info("finished {}/{}", curr.id.category, curr.id.name);
	}

	/**
	 * called when service locator is no longer needed
     */
	public void deactivate(String category)
	{
		LOGGER.info("finished {}", category);
	}
}
