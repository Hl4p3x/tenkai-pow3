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
package org.l2jmobius.gameserver.model.holders;

import org.l2jmobius.gameserver.enums.ItemSkillAgathion;
import org.l2jmobius.gameserver.enums.ItemSkillType;

/**
 * @author UnAfraid
 */
public class ItemSkillHolder extends SkillHolder
{
	private final ItemSkillType _type;
	private final ItemSkillAgathion _agathionSlot;
	private final int _chance;
	private final int _value;
	
	public ItemSkillHolder(int skillId, int skillLvl, ItemSkillType type, int chance, int value)
	{
		super(skillId, skillLvl);
		_type = type;
		_chance = chance;
		_value = value;
		_agathionSlot = ItemSkillAgathion.PRIMARY_AGATHION;
	}

	public ItemSkillHolder(int skillId, int skillLvl, ItemSkillType type, int chance, int value, ItemSkillAgathion agathion)
	{
		super(skillId, skillLvl);
		_type = type;
		_chance = chance;
		_value = value;
		_agathionSlot = agathion;
	}
	
	public ItemSkillType getType()
	{
		return _type;
	}

	public ItemSkillAgathion getAgathionSlot()
	{
		return _agathionSlot;
	}
	
	public int getChance()
	{
		return _chance;
	}
	
	public int getValue()
	{
		return _value;
	}
}
