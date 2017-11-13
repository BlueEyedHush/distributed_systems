package sr.ice.server;

import Ice.*;
import Demo.*;
import Ice.Object;
import org.slf4j.*;

import java.util.function.Function;
import java.util.function.Supplier;


public class ServantLocator1 implements Ice.ServantLocator
{
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ServantLocator1.class.getSimpleName());
	private final Function<String, ? extends Object> servantSupplier;
	private final ObjectAdapter adapter;

	public ServantLocator1(Function<String, ? extends Object> servantSupplier, Ice.ObjectAdapter adapter)
	{
		this.servantSupplier = servantSupplier;
		this.adapter = adapter;
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
		LOGGER.info("locate() {}", idToString(curr.id));

		synchronized (this) {
			Object servant = adapter.find(curr.id);
			if(servant == null) {
				LOGGER.info("create new for {}", idToString(curr.id));
				servant = servantSupplier.apply(curr.id.name);
				adapter.add(servant, curr.id);
			}
			return servant;
		}
	}

	/**
	 * If locate has returned servant, this method will be called when request handling is finished
     */
	public void finished(Ice.Current curr, Ice.Object servant, java.lang.Object cookie) throws UserException 
	{
		LOGGER.info("finished {}", idToString(curr.id));
	}

	/**
	 * called when service locator is no longer needed
     */
	public void deactivate(String category)
	{
		LOGGER.info("deactivated locator for {}", category);
	}

	private static String idToString(Identity id) {
		return id.category + "/" + id.name;
	}
}
