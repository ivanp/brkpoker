/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.brkpoker.texasholdem.webobject;

/**
 *
 * @author ivan
 */
public class RequestActionObject
{
	private int minBet;
	private int currentBet;
	private String[] allowedActions;
	
	public RequestActionObject()
	{
	}
	
	public RequestActionObject(int minBet, int currentBet, String[] allowedActions)
	{
		this.minBet = minBet;
		this.currentBet = currentBet;
		this.allowedActions = allowedActions;
	}
	
	public int getMinBet()
	{
		return minBet;
	}
	
	public void setMinBet(int minBet)
	{
		this.minBet = minBet;
	}
	
	public int getCurrentBet()
	{
		return currentBet;
	}
	
	public void setCurrentBet(int currentBet)
	{
		this.currentBet = currentBet;
	}
	
	public String[] getAllowedActions()
	{
		return allowedActions;
	}
	
	public void setAllowedActions(String[] allowedActions)
	{
		this.allowedActions = allowedActions;
	}
}
