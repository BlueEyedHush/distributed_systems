// **********************************************************************
//
// Copyright (c) 2003-2011 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************

package sr.ice.impl;

import Demo.AMD_Calc_add2;
import Demo.RequestCanceledException;
import Demo._CalcDisp;
import Ice.Current;
import sr.ice.server.WorkQueue;

public class CalcI extends _CalcDisp
{

	private static final long serialVersionUID = -2448962912780867770L;
    private final float resultOffset;
    private WorkQueue _workQueue;

    public CalcI() {
        this(0.0f);
    }

    public CalcI(float resultOffset)
    {
        this.resultOffset = resultOffset;
        _workQueue = null;
    }

	@Override
	public float add1(float a, float b, Current __current) 
	{
        /*try
        {
            Thread.sleep(2000);
        }
        catch(java.lang.InterruptedException ex)
        {
        }*/

		System.out.println("ADD: a = " + a + ", b = " + b + ", result = " + (a+b));
		
		return a + b + resultOffset;
	}

	@Override
	public float subtract(float a, float b, Current __current) 
	{
		System.out.println("ADD: a = " + a + ", b = " + b + ", result = " + (a-b));
	
		return a - b + resultOffset;
	}

	@Override
	public void add2_async(AMD_Calc_add2 __cb, float a, float b, Current __current) throws RequestCanceledException 
	{
        if(a < 10 && b < 10) //zadanie jest proste
        {
            System.out.println("ADD (immediate): a = " + a + ", b = " + b + ", result = " + (a+b));
            __cb.ice_response(a+b+resultOffset);
        }
        else //zadanie jest skomplikowane
        {
            _workQueue.addTask(__cb, 5000, a, b);
        }
	}



}
