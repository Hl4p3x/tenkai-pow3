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
package org.l2jmobius.gameserver.network.serverpackets.sessionzones;

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.PacketWriter;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.OutgoingPackets;
import org.l2jmobius.gameserver.network.serverpackets.IClientOutgoingPacket;

/**
 * @author Mobius
 */
public class TimedHuntingZoneList implements IClientOutgoingPacket
{
	private final PlayerInstance _player;
	private final boolean _isInTimedHuntingZone;
	
	public TimedHuntingZoneList(PlayerInstance player)
	{
		_player = player;
		_isInTimedHuntingZone = player.isInTimedHuntingZone();
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EX_TIME_RESTRICT_FIELD_LIST.writeId(packet);

		final long currentTime = System.currentTimeMillis();
		long endTime;
		packet.writeD(2); // zone count
		
		// Storm Isle
		packet.writeD(1); // required item count
		packet.writeD(57); // item id
		packet.writeQ(Config.TIME_LIMITED_ZONE_TELEPORT_FEE); // item count
		packet.writeD(1); // reset cycle
		packet.writeD(1); // zone id
		packet.writeD(100); // min level
		packet.writeD(120); // max level
		packet.writeD(0); // remain time base?
		endTime = _player.getVariables().getLong(PlayerVariables.HUNTING_ZONE_RESET_TIME + 1, 0);
		long addTime = _player.getVariables().getLong("HUNTING_ZONE_ADD_TIME_" + 1, 0);

		if ((endTime + Config.TIME_LIMITED_ZONE_RESET_DELAY) < currentTime)
		{
			endTime = currentTime + Config.TIME_LIMITED_ZONE_INITIAL_TIME;
			_player.getVariables().set("HUNTING_ZONE_ADD_TIME_" + 1, 0);
		}
		packet.writeD((int) (Math.max(endTime + addTime - currentTime, 0)) / 1000); // remain time
		packet.writeD((int) (Config.TIME_LIMITED_MAX_ADDED_TIME / 1000));
		packet.writeD(addTime == 0 ? 3600 : 3600 - (int)(addTime / 1000)); // remain refill time
		packet.writeD(3600); // refill time max
		packet.writeC(_isInTimedHuntingZone ? 0 : 1); // field activated
		packet.writeC(0x00); // ?
		packet.writeC(0x00); // ?



		// Primeval Isle
		packet.writeD(1); // required item count
		packet.writeD(57); // item id
		packet.writeQ(Config.TIME_LIMITED_ZONE_TELEPORT_FEE); // item count
		packet.writeD(1); // reset cycle
		packet.writeD(6); // zone id
		packet.writeD(105); // min level
		packet.writeD(120); // max level
		packet.writeD(0); // remain time base?
		endTime = _player.getVariables().getLong(PlayerVariables.HUNTING_ZONE_RESET_TIME + 6, 0);
		addTime = _player.getVariables().getLong("HUNTING_ZONE_ADD_TIME_" + 6, 0);
		if ((endTime + Config.TIME_LIMITED_ZONE_RESET_DELAY) < currentTime)
		{
			endTime = currentTime + Config.TIME_LIMITED_ZONE_INITIAL_TIME;
			_player.getVariables().set("HUNTING_ZONE_ADD_TIME_" + 6, 0);
		}
		packet.writeD((int) (Math.max(endTime + addTime - currentTime, 0)) / 1000); // remain time
		packet.writeD((int) (Config.TIME_LIMITED_MAX_ADDED_TIME / 1000));
		packet.writeD(addTime == 0 ? 3600 : 3600 - (int)(addTime / 1000)); // remain refill time
		packet.writeD(3600); // refill time max
		packet.writeC(_isInTimedHuntingZone ? 0 : 1); // field activated
		packet.writeC(0x00); // ?
		packet.writeC(0x00); // ?



		return true;
	}
}