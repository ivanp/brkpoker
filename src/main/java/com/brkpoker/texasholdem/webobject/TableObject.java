/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.brkpoker.texasholdem.webobject;

import java.util.List;
import org.ozsoft.texasholdem.Table;

/**
 *
 * @author ivan
 */
public class TableObject
{
	private String name;
	private int playersCount;
	private String type;
	private int bigBlind;
	private String message;
	private List<PlayerObject> players;
	
	public TableObject()
	{
	}
	
	public TableObject(Table table)
	{
		name = table.getName();
		playersCount = table.getPlayersCount();
		bigBlind = table.getBigBlind();
		message = table.getMessage();
		
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
