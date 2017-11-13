// **********************************************************************
//
// Copyright (c) 2003-2011 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************

package sr.ice.server;


import Demo.Calc;
import Ice.Object;
import sr.ice.impl.CalcI;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Server1
{
	public void t1(String[] args)
	{
		int status = 0;
		Ice.Communicator communicator = null;

		try
		{
			// 1. Inicjalizacja ICE
			communicator = Ice.Util.initialize(args);

			// 2. Konfiguracja adaptera
			// METODA 1 (polecana): Konfiguracja adaptera Adapter1 jest w pliku konfiguracyjnym podanym jako parametr uruchomienia serwera
			//Ice.ObjectAdapter adapter = communicator.createObjectAdapter("Adapter1");  
			
			// METODA 2 (niepolecana): Konfiguracja adaptera Adapter1 jest w kodzie źródłowym
			Ice.ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints("Adapter", "tcp -h localhost -p 10000");

			Function<String, Object> filesystemConfigMock = n -> {
				if(Integer.valueOf(n) % 2 == 0) {
					return new CalcI(-100.0f);
				} else {
					return new CalcI(100.0f);
				}
			};
            ServantLocator1 lazyInitLocator = new ServantLocator1(filesystemConfigMock, adapter);
            adapter.addServantLocator(lazyInitLocator, "K1");

			ServantLocator2 forEachRequestLocator = new ServantLocator2(CalcI::new);
			adapter.addServantLocator(forEachRequestLocator, "K2");

			ServantLocator3 forEachRequestPool = new ServantLocator3(CalcI::new, 2);
			adapter.addServantLocator(forEachRequestPool, "K3");

			CalcI defaultServantForAll = new CalcI();
			adapter.addDefaultServant(defaultServantForAll, "K4");

			Map<String, Ice.Object> datastoreMock = new HashMap<>();
			ServantLocator5 evictingLocator = new ServantLocator5(CalcI::new,
					(val, key) -> datastoreMock.put(key, val),
					datastoreMock::get,
					3);
			adapter.addServantLocator(evictingLocator, "K5");
			// 3. Stworzenie serwanta/serwantów
			//CalcI calcServant1 = new CalcI();

			// zad07
		    //WorkQueue workQueue = new WorkQueue(); 
		    //CalcI calcServant2 = new CalcI(workQueue);      
		    //workQueue.start();	        

		    
			// 4. Dodanie wpisów do ASM
			//adapter.add(calcServant1, new Identity("calc11", "calc"));
	        //adapter.add(calcServant2, new Identity("calc77", "calc"));

	        // 5. Aktywacja adaptera i przejście w pętlę przetwarzania żądań
			adapter.activate();
			System.out.println("Entering event processing loop...");
			communicator.waitForShutdown();
		}
		catch (Exception e)
		{
			System.err.println(e);
			status = 1;
		}
		if (communicator != null)
		{
			// Clean up
			try
			{
				communicator.destroy();
			}
			catch (Exception e)
			{
				System.err.println(e);
				status = 1;
			}
		}
		System.exit(status);
	}


	public static void main(String[] args)
	{
		Server1 app = new Server1();
		app.t1(args);
	}
}
