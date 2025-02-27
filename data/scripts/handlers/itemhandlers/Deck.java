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
package scripts.handlers.itemhandlers;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.datatables.ItemTable;
import org.l2jmobius.gameserver.enums.DropType;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.holders.DropHolder;
import org.l2jmobius.gameserver.model.items.Item;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.Util;

import java.text.DecimalFormat;
import java.util.List;

public class Deck implements IItemHandler
{

	private static final int DROP_LIST_ITEMS_PER_PAGE = 10;

	@Override
	public boolean useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!(playable instanceof PlayerInstance))
		{
			return false;
		}

		PlayerInstance player = (PlayerInstance) playable;
		Object target = player.getTarget();

		if (target == null || !(target instanceof Npc))
		{
			player.sendMessage("You should target a monster to see its drop list!");
			return false;
		}

		Npc mob = (Npc) player.getTarget();
		if (mob != null)
		{
			sendNpcDropList(player, mob, DropType.DROP, 1);
		}
		return true;
	}

	private void sendNpcDropList(PlayerInstance player, Npc npc, DropType dropType, int page)
	{
		final List<DropHolder> dropList = npc.getTemplate().getDropList(dropType);
		if (dropList == null)
		{
			return;
		}

		int pages = dropList.size() / DROP_LIST_ITEMS_PER_PAGE;
		if ((DROP_LIST_ITEMS_PER_PAGE * pages) < dropList.size())
		{
			pages++;
		}

		final StringBuilder pagesSb = new StringBuilder();
		if (pages > 1)
		{
			pagesSb.append("<table><tr>");
			for (int i = 0; i < pages; i++)
			{
				pagesSb.append("<td align=center><button value=\"" + (i + 1) + "\" width=20 height=20 action=\"bypass NpcViewMod dropList " + dropType + " " + npc.getObjectId() + " " + i + "\" back=\"L2UI_CT1.Button_DF_Calculator_Down\" fore=\"L2UI_CT1.Button_DF_Calculator\"></td>");
			}
			pagesSb.append("</tr></table>");
		}

		if (page >= pages)
		{
			page = pages - 1;
		}

		final int start = page > 0 ? page * DROP_LIST_ITEMS_PER_PAGE : 0;
		int end = (page * DROP_LIST_ITEMS_PER_PAGE) + DROP_LIST_ITEMS_PER_PAGE;
		if (end > dropList.size())
		{
			end = dropList.size();
		}

		final DecimalFormat amountFormat = new DecimalFormat("#,###");
		final DecimalFormat chanceFormat = new DecimalFormat("0.00##");
		int leftHeight = 0;
		int rightHeight = 0;
		final double dropAmountEffectBonus = player.getStat().getValue(Stat.BONUS_DROP_AMOUNT, 1);
		final double dropRateEffectBonus = player.getStat().getValue(Stat.BONUS_DROP_RATE, 1);
		final double spoilRateEffectBonus = player.getStat().getValue(Stat.BONUS_SPOIL_RATE, 1);
		final StringBuilder leftSb = new StringBuilder();
		final StringBuilder rightSb = new StringBuilder();
		String limitReachedMsg = "";
		for (int i = start; i < end; i++)
		{
			final StringBuilder sb = new StringBuilder();
			final int height = 64;
			final DropHolder dropItem = dropList.get(i);
			final Item item = ItemTable.getInstance().getTemplate(dropItem.getItemId());

			// real time server rate calculations
			double rateChance = 1;
			double rateAmount = 1;
			if (dropType == DropType.SPOIL)
			{
				rateChance = Config.RATE_SPOIL_DROP_CHANCE_MULTIPLIER;
				rateAmount = Config.RATE_SPOIL_DROP_AMOUNT_MULTIPLIER;

				// also check premium rates if available
				if (Config.PREMIUM_SYSTEM_ENABLED && player.hasPremiumStatus())
				{
					rateChance *= Config.PREMIUM_RATE_SPOIL_CHANCE;
					rateAmount *= Config.PREMIUM_RATE_SPOIL_AMOUNT;
				}

				// bonus spoil rate effect
				rateChance *= spoilRateEffectBonus;
			}
			else
			{
				if (Config.RATE_DROP_CHANCE_BY_ID.get(dropItem.getItemId()) != null)
				{
					rateChance *= Config.RATE_DROP_CHANCE_BY_ID.get(dropItem.getItemId());
				}
				else if (item.hasExImmediateEffect())
				{
					rateChance *= Config.RATE_HERB_DROP_CHANCE_MULTIPLIER;
				}
				else if (npc.isRaid())
				{
					rateChance *= Config.RATE_RAID_DROP_CHANCE_MULTIPLIER;
				}
				else
				{
					rateChance *= Config.RATE_DEATH_DROP_CHANCE_MULTIPLIER;
				}

				if (Config.RATE_DROP_AMOUNT_BY_ID.get(dropItem.getItemId()) != null)
				{
					rateAmount *= Config.RATE_DROP_AMOUNT_BY_ID.get(dropItem.getItemId());
				}
				else if (item.hasExImmediateEffect())
				{
					rateAmount *= Config.RATE_HERB_DROP_AMOUNT_MULTIPLIER;
				}
				else if (npc.isRaid())
				{
					rateAmount *= Config.RATE_RAID_DROP_AMOUNT_MULTIPLIER;
				}
				else
				{
					rateAmount *= Config.RATE_DEATH_DROP_AMOUNT_MULTIPLIER;
				}

				// also check premium rates if available
				if (Config.PREMIUM_SYSTEM_ENABLED && player.hasPremiumStatus())
				{
					if (Config.PREMIUM_RATE_DROP_CHANCE_BY_ID.get(dropItem.getItemId()) != null)
					{
						rateChance *= Config.PREMIUM_RATE_DROP_CHANCE_BY_ID.get(dropItem.getItemId());
					}
					else if (item.hasExImmediateEffect())
					{
						// TODO: Premium herb chance? :)
					}
					else if (npc.isRaid())
					{
						// TODO: Premium raid chance? :)
					}
					else
					{
						rateChance *= Config.PREMIUM_RATE_DROP_CHANCE;
					}

					if (Config.PREMIUM_RATE_DROP_AMOUNT_BY_ID.get(dropItem.getItemId()) != null)
					{
						rateAmount *= Config.PREMIUM_RATE_DROP_AMOUNT_BY_ID.get(dropItem.getItemId());
					}
					else if (item.hasExImmediateEffect())
					{
						// TODO: Premium herb amount? :)
					}
					else if (npc.isRaid())
					{
						// TODO: Premium raid amount? :)
					}
					else
					{
						rateAmount *= Config.PREMIUM_RATE_DROP_AMOUNT;
					}
				}

				// bonus drop amount effect
				rateAmount *= dropAmountEffectBonus;
				// bonus drop rate effect
				rateChance *= dropRateEffectBonus;
			}

			sb.append("<table width=332 cellpadding=2 cellspacing=0 background=\"L2UI_CT1.Windows.Windows_DF_TooltipBG\">");
			sb.append("<tr><td width=32 valign=top>");
			sb.append("<button width=\"32\" height=\"32\" back=\"" + (item.getIcon() == null ? "icon.etc_question_mark_i00" : item.getIcon()) + "\" fore=\"" + (item.getIcon() == null ? "icon.etc_question_mark_i00" : item.getIcon()) + "\" itemtooltip=\"" + dropItem.getItemId() + "\">");
			sb.append("</td><td fixwidth=300 align=center><font name=\"hs9\" color=\"CD9000\">");
			sb.append(item.getName());
			sb.append("</font></td></tr><tr><td width=32></td><td width=300><table width=295 cellpadding=0 cellspacing=0>");
			sb.append("<tr><td width=48 align=right valign=top><font color=\"LEVEL\">Amount:</font></td>");
			sb.append("<td width=247 align=center>");

			final long min = (long) (dropItem.getMin() * rateAmount);
			final long max = (long) (dropItem.getMax() * rateAmount);
			if (min == max)
			{
				sb.append(amountFormat.format(min));
			}
			else
			{
				sb.append(amountFormat.format(min));
				sb.append(" - ");
				sb.append(amountFormat.format(max));
			}

			sb.append("</td></tr><tr><td width=48 align=right valign=top><font color=\"LEVEL\">Chance:</font></td>");
			sb.append("<td width=247 align=center>");
			sb.append(chanceFormat.format(Math.min(dropItem.getChance() * rateChance, 100)));
			sb.append("%</td></tr></table></td></tr><tr><td width=32></td><td width=300>&nbsp;</td></tr></table>");
			if ((sb.length() + rightSb.length() + leftSb.length()) < 16000) // limit of 32766?
			{
				if (leftHeight >= (rightHeight + height))
				{
					rightSb.append(sb);
					rightHeight += height;
				}
				else
				{
					leftSb.append(sb);
					leftHeight += height;
				}
			}
			else
			{
				limitReachedMsg = "<br><center>Too many drops! Could not display them all!</center>";
			}
		}

		final StringBuilder bodySb = new StringBuilder();
		bodySb.append("<table><tr>");
		bodySb.append("<td>");
		bodySb.append(leftSb.toString());
		bodySb.append("</td><td>");
		bodySb.append(rightSb.toString());
		bodySb.append("</td>");
		bodySb.append("</tr></table>");

		String html = HtmCache.getInstance().getHtm(player, "data/html/mods/NpcView/DropList.htm");
		if (html == null)
		{
			LOGGER.warning(handlers.bypasshandlers.NpcViewMod.class.getSimpleName() + ": The html file data/html/mods/NpcView/DropList.htm could not be found.");
			return;
		}
		html = html.replace("%name%", npc.getName());
		html = html.replace("%dropListButtons%", getDropListButtons(npc));
		html = html.replace("%pages%", pagesSb.toString());
		html = html.replace("%items%", bodySb.toString() + limitReachedMsg);
		Util.sendCBHtml(player, html);
	}


	private static String getDropListButtons(Npc npc)
	{
		final StringBuilder sb = new StringBuilder();
		final List<DropHolder> dropListDeath = npc.getTemplate().getDropList(DropType.DROP);
		final List<DropHolder> dropListSpoil = npc.getTemplate().getDropList(DropType.SPOIL);
		if ((dropListDeath != null) || (dropListSpoil != null))
		{
			sb.append("<table width=275 cellpadding=0 cellspacing=0><tr>");
			if (dropListDeath != null)
			{
				sb.append("<td align=center><button value=\"Show Drop\" width=100 height=25 action=\"bypass NpcViewMod dropList DROP " + npc.getObjectId() + "\" back=\"L2UI_CT1.Button_DF_Calculator_Down\" fore=\"L2UI_CT1.Button_DF_Calculator\"></td>");
			}

			if (dropListSpoil != null)
			{
				sb.append("<td align=center><button value=\"Show Spoil\" width=100 height=25 action=\"bypass NpcViewMod dropList SPOIL " + npc.getObjectId() + "\" back=\"L2UI_CT1.Button_DF_Calculator_Down\" fore=\"L2UI_CT1.Button_DF_Calculator\"></td>");
			}

			sb.append("</tr></table>");
		}
		return sb.toString();
	}
}
