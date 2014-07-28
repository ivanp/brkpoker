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
	private String tableName;
	private int seatNum;
	private int buyIn;
	
	public SitObject()
	{
	}
	
	public SitObject(String tableName, int seatNum, int buyIn)
	{
		this.tableName = tableName;
		this.seatNum = seatNum;
		this.buyIn = buyIn;
	}
	
	public String getTableName()
	{
		return tableName;
	}
	
	public void setTableName(String tableName)
	{
		this.tableName = tableName;
	}
	
	public int getSeatNum()
	{
		return seatNum;
	}
	
	public void setSeatNum(int seatNum)
	{
		this.seatNum = seatNum;
	}
	
	public int getBuyIn()
	{
		return buyIn;
	}
	
	public void setBuyIn(int buyIn)
	{
		this.buyIn = buyIn;
	}
}
