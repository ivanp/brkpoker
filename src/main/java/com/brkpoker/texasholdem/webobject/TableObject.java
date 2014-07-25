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
public class TableObject
{
	private String name;
	private int playersCount;
	
	public TableObject()
	{
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	public String getName()
	{
		return name;
	}
	
	public void setPlayersCount(int count)
	{
		playersCount = count;
	}
	
	public int getPlayersCount()
	{
		return playersCount;
	}
}
