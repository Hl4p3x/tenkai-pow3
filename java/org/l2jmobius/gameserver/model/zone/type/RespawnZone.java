/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.model.zone.type;

import java.util.EnumMap;
import java.util.Map;

import org.l2jmobius.gameserver.enums.Race;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.zone.ZoneRespawn;

/**
 * Respawn zone implementation.
 * @author Nyaran
 */
public class RespawnZone extends ZoneRespawn
{
	private final Map<Race, String> _raceRespawnPoint = new EnumMap<>(Race.class);
	
	public RespawnZone(int id)
	{
		super(id);
	}
	
	@Override
	protected void onEnter(Creature creature)
	{
	}
	
	@Override
	protected void onExit(Creature creature)
	{
	}

	@Override
	public void onDieInside(Creature character, Creature killer) {

	}

	public void addRaceRespawnPoint(String race, String point)
	{
		_raceRespawnPoint.put(Race.valueOf(race), point);
	}
	
	public Map<Race, String> getAllRespawnPoints()
	{
		return _raceRespawnPoint;
	}
	
	public String getRespawnPoint(PlayerInstance player)
	{
		return _raceRespawnPoint.get(player.getRace());
	}
}
