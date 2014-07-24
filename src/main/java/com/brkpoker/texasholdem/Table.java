/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.brkpoker.texasholdem;

import java.util.ArrayList;
import java.util.List;
import org.ozsoft.texasholdem.TableType;

/**
 *
 * @author ivan
 */
public class Table extends org.ozsoft.texasholdem.Table
{
	private final List<User> spectators;
	
	public Table(TableType type, int bigBlind) 
	{
		super(type, bigBlind);
		spectators = new ArrayList();
	}
	
	public void addSpectator(User user)
	{
		spectators.add(user);
	}
	
	public void removeSpectator(User user)
	{
		spectators.remove(user);
	}
}
