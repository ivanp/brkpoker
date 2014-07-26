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
public class PlayRequestObject
{
	private String table;
	private int buyin;
	
	public PlayRequestObject()
	{
		
	}
	
	public PlayRequestObject(String table, int buyin)
	{
		this.table = table;
		this.buyin = buyin;
	}
	
	public String getTable()
	{
		return table;
	}
	
	public void setTable(String table)
	{
		this.table = table;
	}
	
	public int getBuyin()
	{
		return buyin;
	}
	
	public void setBuyin(int buyin)
	{
		this.buyin = buyin;
	}
}
