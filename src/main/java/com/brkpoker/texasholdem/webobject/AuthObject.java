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
public class AuthObject
{
	private boolean success;
	private String name;
	private int cash;
	
	public AuthObject()
	{
		
	}
	
	public AuthObject(boolean success, String name, int cash)
	{
		this.success = success;
		this.name = name;
		this.cash = cash;
	}
	
	public boolean getSuccess()
	{
		return success;
	}
	
	public void setSuccess(boolean success)
	{
		this.success = success;
	}
	
	public String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	public int getCash()
	{
		return cash;
	}
	
	public void setCash(int cash)
	{
		this.cash = cash;
	}
}
