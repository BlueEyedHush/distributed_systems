// **********************************************************************
//
// Copyright (c) 2003-2011 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************

package sr.ice.client;

import Demo.CalcPrx;
import Demo.CalcPrxHelper;
import Ice.AsyncResult;
import Ice.Communicator;
import Ice.Identity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

public class Client5
{
	private static final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
	private static final Pattern CATEGORY_PATTERN = Pattern.compile("K[1-5]");
	private static final int MAX_NUM = 3;

	public static void main(String[] args)
	{
		int status = 0;
		Ice.Communicator communicator = null;
		int calcNum = 0;

		try {
            String category = "";
            while(!CATEGORY_PATTERN.matcher(category).matches()) {
                System.out.print("Category?: ");
			    category = in.readLine().trim();
            }

			// 1. Inicjalizacja ICE
			communicator = Ice.Util.initialize(args);

			CalcPrx calc1 = getProxyWithIdentity(communicator, category, calcNum++);

			// 4. Wywolanie zdalnych operacji

			String line = null;
			AsyncResult ar = null;
						
			do
			{
				try
				{
					System.out.print("==> ");
					System.out.flush();
					line = in.readLine();

					if (line == null)
					{
						break;
					}
					if(line.equalsIgnoreCase("no")) {
						calc1 = getProxyWithIdentity(communicator, category, calcNum++);
					}
					if (line.equals("add1"))
					{
						float r = calc1.add1(7, 8);
						System.out.println("RESULT (syn) = " + r);
					}
					if (line.equals("subtract"))
					{
						float r = calc1.subtract(7, 8);
						System.out.println("RESULT (syn) = " + r);
					}
					if (line.equals("dyn subtract")) //zad09
					{
						Ice.OutputStream outs = Ice.Util.createOutputStream(communicator);
						outs.startEncapsulation();
						//outs.write.... TODO
						//outs.write....  TODO
						outs.endEncapsulation();
						byte[] inParams = outs.finished();
						Ice.ByteSeqHolder outParams = new Ice.ByteSeqHolder();
						
						if (calc1.ice_invoke("...", Ice.OperationMode.Normal, inParams, outParams)) //TODO 
						{
							Ice.InputStream ins = Ice.Util.createInputStream(communicator, outParams.value);
							ins.startEncapsulation();
							//float r = ins.read.... TODO
							ins.endEncapsulation();
							
							System.out.println("RESULT (syn) = " + "r");  //TODO
						}
					}
					if(line.equals("add2 1")) //zad07
					{
						float r = calc1.add2(4, 5);
						System.out.println("RESULT (syn) = " + r);
					}
					if(line.equals("add2 2")) //zad07
					{
						float r = calc1.add2(40, 50);
						System.out.println("RESULT (syn) = " + r);
					}
				}
				catch (java.io.IOException ex)
				{
					System.err.println(ex);
				}
			}
			while (!line.equals("x"));


		} catch (Ice.LocalException e) {
			e.printStackTrace();
			status = 1;
		} catch (Exception e) {
			System.err.println(e.getMessage());
			status = 1;
		}
		if (communicator != null) {
			// Clean up
			//
			try {
				communicator.destroy();
			} catch (Exception e) {
				System.err.println(e.getMessage());
				status = 1;
			}
		}
		System.exit(status);
	}

	private static CalcPrx getProxyWithIdentity(Communicator communicator, String category, int num) {
		int clampedNum = num % (MAX_NUM+1);
		Ice.ObjectPrx base1 = communicator.stringToProxy(category + "/" + clampedNum + ":tcp -h localhost -p 10000");
		CalcPrx calc1 = CalcPrxHelper.checkedCast(base1);
		if (calc1 == null) throw new Error("Invalid proxy");
		else return calc1;
	}
}