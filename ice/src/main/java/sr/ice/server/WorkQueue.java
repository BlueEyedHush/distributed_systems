package sr.ice.server;

//**********************************************************************
//
//Copyright (c) 2003-2013 ZeroC, Inc. All rights reserved.
//
//This copy of Ice is licensed to you under the terms described in the
//ICE_LICENSE file included in this distribution.
//
//**********************************************************************

import Demo.*;

public class WorkQueue extends Thread
{
	
 class CallbackEntry
 {
     AMD_Calc_add2 cb;
     int delay;
     float a;
     float b;
 }

 public synchronized void run()
 {
     while(!_done)
     {
         if(_callbacks.size() == 0)
         {
             try
             {
                 wait();
             }
             catch(java.lang.InterruptedException ex)
             {
             }
         }

         if(_callbacks.size() != 0)
         {
             // Get next work item.
             CallbackEntry entry = (CallbackEntry)_callbacks.getFirst();

             // Wait for the amount of time indicated in delay to emulate a process 
             // that takes a significant period of time to complete.
             try
             {
                 wait(entry.delay);
             }
             catch(java.lang.InterruptedException ex)
             {
             }

             if(!_done)
             {
                 _callbacks.removeFirst();
                 float r = entry.a + entry.b;
                 System.out.println("ADD (by workqueue): a = " + entry.a + ", b = " + entry.b + ", result = " + r);
                 entry.cb.ice_response(r);
             }
         }
     }

     // Throw exception for any outstanding requests.
     for(CallbackEntry p : _callbacks)
     {
         p.cb.ice_exception(new RequestCanceledException());
     }
 }

 public synchronized void addTask(AMD_Calc_add2 cb, int delay, float a, float b)
 {
     if(!_done)
     {
         // Add the work item.
         CallbackEntry entry = new CallbackEntry();
         entry.cb = cb;
         entry.delay = delay;
         entry.a = a;
         entry.b = b;

         if(_callbacks.size() == 0)
         {
             notify();
         }
         _callbacks.add(entry);
     }
     else
     {
         // Destroyed, throw exception.
         cb.ice_exception(new RequestCanceledException());
     }
 }

 public synchronized void _destroy()                  
 // Thread.destroy is deprecated.
 {
     _done = true;
     notify();
 }

 private java.util.LinkedList<CallbackEntry> _callbacks = new java.util.LinkedList<CallbackEntry>();
 private boolean _done = false;
}
