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
package org.l2jmobius.gameserver.network.clientpackets.sessionzones;

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.PacketReader;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.olympiad.OlympiadManager;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.IClientIncomingPacket;

/**
 * @author Mobius
 */
public class ExTimedHuntingZoneEnter implements IClientIncomingPacket
{
	private int _zoneId;
	
	@Override
	public boolean read(GameClient client, PacketReader packet)
	{
		_zoneId = packet.readD();
		return true;
	}
	
	@Override
	public void run(GameClient client)
	{
		final PlayerInstance player = client.getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (player.isMounted())
		{
			player.sendMessage("Cannot use time-limited hunting zones while mounted.");
			return;
		}
		if (player.isInDuel())
		{
			player.sendMessage("Cannot use time-limited hunting zones during a duel.");
			return;
		}
		if (player.isInOlympiadMode() || OlympiadManager.getInstance().isRegistered(player))
		{
			player.sendPacket(SystemMessageId.CANNOT_USE_TIME_LIMITED_HUNTING_ZONES_WHILE_WAITING_FOR_THE_OLYMPIAD);
			return;
		}
		if (player.isOnEvent() || (player.getBlockCheckerArena() > -1))
		{
			player.sendMessage("Cannot use time-limited hunting zones while registered on an event.");
			return;
		}
		if (player.isInInstance())
		{
			player.sendMessage("Cannot use time-limited hunting zones while in an instance.");
			return;
		}
		
		if (((_zoneId == 1) && (player.getLevel() < 100)) //
			|| ((_zoneId == 6) && (player.getLevel() < 105)) //
		)
		{
			player.sendMessage("Your level does not correspond the zone equivalent.");
		}
		
		final long currentTime = System.currentTimeMillis();
		long endTime = player.getVariables().getLong(PlayerVariables.HUNTING_ZONE_RESET_TIME + _zoneId, 0);
		long addTime = player.getVariables().getLong("HUNTING_ZONE_ADD_TIME_" + _zoneId, 0);
		if ((endTime + Config.TIME_LIMITED_ZONE_RESET_DELAY) < currentTime)
		{
			endTime = currentTime + addTime + Config.TIME_LIMITED_ZONE_INITIAL_TIME;
		}
		
		if (endTime > currentTime)
		{
			if (player.getAdena() > Config.TIME_LIMITED_ZONE_TELEPORT_FEE)
			{
				player.reduceAdena("TimedHuntingZone", Config.TIME_LIMITED_ZONE_TELEPORT_FEE, player, true);
			}
			else
			{
				player.sendPacket(SystemMessageId.NOT_ENOUGH_ADENA);
				return;
			}
			
			switch (_zoneId)
			{
				case 1: // Storm Isle
				{
					player.teleToLocation(194291, 176604, -1888);
					break;
				}
				case 6: // Primeval Isle
				{
					player.teleToLocation(9400, -21720, -3634);
					break;
				}
			}
			
			player.getVariables().set(PlayerVariables.HUNTING_ZONE_RESET_TIME + _zoneId, endTime);
			player.startTimedHuntingZone(_zoneId, endTime - currentTime);
		}
		else
		{
			player.sendPacket(SystemMessageId.YOU_DON_T_HAVE_ENOUGH_TIME_AVAILABLE_TO_ENTER_THE_HUNTING_ZONE);
		}
	}
}
