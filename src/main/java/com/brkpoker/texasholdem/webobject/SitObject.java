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
public class SitObject
{
	private String table;
	private int num;
	private int buy;
	
	public SitObject()
	{
	}
	
	public SitObject(String table, int num, int buy)
	{
		this.table = table;
		this.num = num;
		this.buy = buy;
	}
	
	public String getTable()
	{
		return table;
	}
	
	public void setTable(String table)
	{
		this.table = table;
	}
	
	public int getNum()
	{
		return num;
	}
	
	public void setNum(int num)
	{
		this.num = num;
	}
	
	public int getBuy()
	{
		return buy;
	}
	
	public void setBuy(int buy)
	{
		this.buy = buy;
	}
}
