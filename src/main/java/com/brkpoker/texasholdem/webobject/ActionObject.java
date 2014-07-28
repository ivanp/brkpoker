/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.brkpoker.texasholdem.webobject;

import org.ozsoft.texasholdem.actions.Action;
import org.ozsoft.texasholdem.actions.BetAction;
import org.ozsoft.texasholdem.actions.CallAction;
import org.ozsoft.texasholdem.actions.RaiseAction;

/**
 *
 * @author ivan
 */
public class ActionObject
{
	public String name;
	public int amount;
	
	public ActionObject()
	{}
	
	public ActionObject(String name, int amount)
	{
		this.name = name;
		this.amount = amount;
	}
	
	public String getName()
	{
		return this.name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	public int getAmount()
	{
		return amount;
	}
	
	public void setAmount(int amount)
	{
		this.amount = amount;
	}
	
	public Action getActionInstance()
	{
		Action action = Action.FOLD;
		if (name.equalsIgnoreCase(Action.BET.getName()))
		{
			action = new BetAction(amount);
		}
		else if (name.equalsIgnoreCase(Action.RAISE.getName()))
		{
			action = new RaiseAction(amount);
		}
		else if (name.equalsIgnoreCase(Action.CALL.getName()))
		{
			action = Action.CALL;
		}
		else if (name.equalsIgnoreCase(Action.CHECK.getName()))
		{
			action = Action.CHECK;
		}
		else if (name.equalsIgnoreCase(Action.FOLD.getName()))
		{
			action = Action.FOLD;
		}
		else if (name.equalsIgnoreCase(Action.ALL_IN.getName()))
		{
			action = Action.ALL_IN;
		}
		
		return action;
	}
}
