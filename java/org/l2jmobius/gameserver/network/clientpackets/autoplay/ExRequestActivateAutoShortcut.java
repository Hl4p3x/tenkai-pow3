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
package org.l2jmobius.gameserver.network.clientpackets.autoplay;

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.PacketReader;
import org.l2jmobius.gameserver.model.Shortcut;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.clientpackets.IClientIncomingPacket;
import org.l2jmobius.gameserver.network.serverpackets.autoplay.ExActivateAutoShortcut;

/**
 * @author JoeAlisson, Mobius
 */
public class ExRequestActivateAutoShortcut implements IClientIncomingPacket
{
	private boolean _activate;
	private int _room;
	
	@Override
	public boolean read(GameClient client, PacketReader packet)
	{
		_room = packet.readH();
		_activate = packet.readC() == 1;
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
		
		int slot = _room % 12;
		int page = _room / 12;
		Shortcut shortcut = player.getShortCut(slot, page);
		if (shortcut == null)
		{
			for (int room = 264; room <= 275; room++) {
				slot = room % 12;
				page = room / 12;
				shortcut = player.getShortCut(slot, page);
				if (shortcut == null) {
					continue;
				}
				final ItemInstance item = player.getInventory().getItemByObjectId(shortcut.getId());
				Skill skill = null;
				if (item == null)
				{
					continue;
				}
				client.sendPacket(new ExActivateAutoShortcut(room, _activate));

				// stop
				if (!_activate)
				{
					if (item != null)
					{
						// auto supply
						if (_room > 263)
						{
							player.removeAutoSupplyItem(item.getId());
						}
						else // auto potion
						{
							player.removeAutoPotionItem(item.getId());
						}
					}
					// auto skill
					if (skill != null)
					{
						player.removeAutoSkill(skill.getId());
					}
					continue;
				}

				// start
				if (_room > 263)
				{
					// auto supply
					if (Config.ENABLE_AUTO_ITEM && (item != null))
					{
						player.addAutoSupplyItem(item.getId());
					}
				}
				else
				{
					// auto potion
					if ((page == 23) && (slot == 1))
					{
						if (Config.ENABLE_AUTO_POTION && (item != null) && item.isPotion())
						{
							player.addAutoPotionItem(item.getId());
							return;
						}
					}
					// auto skill
					if (Config.ENABLE_AUTO_BUFF && (skill != null))
					{
						player.addAutoSkill(skill.getId());
					}
				}
			}
			return;
		}
		client.sendPacket(new ExActivateAutoShortcut(_room, _activate));

		final ItemInstance item = player.getInventory().getItemByObjectId(shortcut.getId());
		Skill skill = null;
		if (item == null)
		{
			skill = player.getKnownSkill(shortcut.getId());
		}
		
		// stop
		if (!_activate)
		{
			if (item != null)
			{
				// auto supply
				if (_room > 263)
				{
					player.removeAutoSupplyItem(item.getId());
				}
				else // auto potion
				{
					player.removeAutoPotionItem(item.getId());
				}
			}
			// auto skill
			if (skill != null)
			{
				player.removeAutoSkill(skill.getId());
			}
			return;
		}
		
		// start
		if (_room > 263)
		{
			// auto supply
			if (Config.ENABLE_AUTO_ITEM && (item != null))
			{
				player.addAutoSupplyItem(item.getId());
			}
		}
		else
		{
			// auto potion
			if ((page == 23) && (slot == 1))
			{
				if (Config.ENABLE_AUTO_POTION && (item != null) && item.isPotion())
				{
					player.addAutoPotionItem(item.getId());
					return;
				}
			}
			// auto skill
			if (Config.ENABLE_AUTO_BUFF && (skill != null))
			{
				player.addAutoSkill(skill.getId());
			}
		}
	}
}
