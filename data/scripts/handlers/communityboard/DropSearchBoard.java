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
package handlers.communityboard;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import org.l2jmobius.Config;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.data.xml.impl.NpcData;
import org.l2jmobius.gameserver.data.xml.impl.SpawnData;
import org.l2jmobius.gameserver.datatables.ItemTable;
import org.l2jmobius.gameserver.enums.DropType;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IParseBoardHandler;
import org.l2jmobius.gameserver.instancemanager.FarmZoneManager;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.holders.DropHolder;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.items.Item;
import org.l2jmobius.gameserver.model.spawns.NpcSpawnTemplate;
import org.l2jmobius.gameserver.model.stats.Stat;

/**
 * @author yksdtc
 */
public class DropSearchBoard implements IParseBoardHandler
{
	private static final String NAVIGATION_PATH = "data/html/CommunityBoard/Custom/navigation.html";
	private static final String[] COMMAND =
	{
		"_bbs_search_item",
		"_bbs_search_drop",
		"_bbs_npc_trace"
	};
	private static handlers.communityboard.DropSearchBoard _instance;

	private class CBDropHolder
	{
		final int itemId;
		final int npcId;
		final byte npcLevel;
		final long min;
		final long max;
		final double chance;
		final boolean isSpoil;
		final boolean isRaid;
		
		public CBDropHolder(NpcTemplate npcTemplate, DropHolder dropHolder)
		{
			isSpoil = dropHolder.getDropType() == DropType.SPOIL;
			itemId = dropHolder.getItemId();
			npcId = npcTemplate.getId();
			npcLevel = npcTemplate.getLevel();
			min = dropHolder.getMin();
			max = dropHolder.getMax();
			chance = dropHolder.getChance();
			isRaid = npcTemplate.getType().equals("RaidBoss") || npcTemplate.getType().equals("GrandBoss");
		}
		
		/**
		 * only for debug
		 */
		@Override
		public String toString()
		{
			return "DropHolder [itemId=" + itemId + ", npcId=" + npcId + ", npcLevel=" + npcLevel + ", min=" + min + ", max=" + max + ", chance=" + chance + ", isSpoil=" + isSpoil + "]";
		}
	}
	
	private final Map<Integer, List<CBDropHolder>> DROP_INDEX_CACHE = new HashMap<>();
	
	// nonsupport items
	private static final Set<Integer> BLOCK_ID = new HashSet<>();
	static
	{
		BLOCK_ID.add(Inventory.ADENA_ID);
	}
	
	public DropSearchBoard()
	{
		buildDropIndex();
	}
	
	private void buildDropIndex()
	{
		NpcData.getInstance().getTemplates(npc -> npc.getDropList(DropType.DROP) != null).forEach(npcTemplate ->
		{
			for (DropHolder dropHolder : npcTemplate.getDropList(DropType.DROP))
			{
				addToDropList(npcTemplate, dropHolder);
			}
		});
		NpcData.getInstance().getTemplates(npc -> npc.getDropList(DropType.SPOIL) != null).forEach(npcTemplate ->
		{
			for (DropHolder dropHolder : npcTemplate.getDropList(DropType.SPOIL))
			{
				addToDropList(npcTemplate, dropHolder);
			}
		});
		
		DROP_INDEX_CACHE.values().stream().forEach(l -> l.sort((d1, d2) -> Byte.valueOf(d1.npcLevel).compareTo(Byte.valueOf(d2.npcLevel))));
	}


	public static DropSearchBoard getInstance() {
		return _instance == null ? (_instance = new DropSearchBoard()) : _instance;
	}
	
	private void addToDropList(NpcTemplate npcTemplate, DropHolder dropHolder)
	{
		if (BLOCK_ID.contains(dropHolder.getItemId()))
		{
			return;
		}
		
		List<CBDropHolder> dropList = DROP_INDEX_CACHE.get(dropHolder.getItemId());
		if (dropList == null)
		{
			dropList = new ArrayList<>();
			DROP_INDEX_CACHE.put(dropHolder.getItemId(), dropList);
		}
		
		dropList.add(new CBDropHolder(npcTemplate, dropHolder));
	}

	public void  updateDropIndex() {
		buildDropIndex();
	}

	@Override
	public boolean parseCommunityBoardCommand(String command, PlayerInstance player)
	{
		final String navigation = HtmCache.getInstance().getHtm(player, NAVIGATION_PATH);
		final String[] params = command.split(" ");
		String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/Custom/dropsearch/main.html");
		buildDropIndex();
		switch (params[0])
		{
			case "_bbs_search_item":
			{
				final String itemName = buildItemName(params);
				final String result = buildItemSearchResult(itemName);
				html = html.replace("%searchResult%", result);
				break;
			}
			case "_bbs_search_drop":
			{
				final DecimalFormat chanceFormat = new DecimalFormat("0.00##");
				final int itemId = Integer.parseInt(params[1]);
				int page = Integer.parseInt(params[2]);
				final List<CBDropHolder> list = DROP_INDEX_CACHE.get(itemId);
				int pages = list.size() / 14;
				if (pages == 0)
				{
					pages++;
				}
				
				final int start = (page - 1) * 14;
				final int end = Math.min(list.size() - 1, start + 14);
				final StringBuilder builder = new StringBuilder();
				final double dropAmountEffectBonus = player.getStat().getValue(Stat.BONUS_DROP_AMOUNT, 1);
				final double dropRateEffectBonus = player.getStat().getValue(Stat.BONUS_DROP_RATE, 1);
				final double spoilRateEffectBonus = player.getStat().getValue(Stat.BONUS_SPOIL_RATE, 1);
				for (int index = start; index <= end; index++)
				{
					final CBDropHolder cbDropHolder = list.get(index);
					
					// real time server rate calculations
					double rateChance = 1;
					double rateAmount = 1;
					if (cbDropHolder.isSpoil)
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
						final Item item = ItemTable.getInstance().getTemplate(cbDropHolder.itemId);
						if (Config.RATE_DROP_CHANCE_BY_ID.get(cbDropHolder.itemId) != null)
						{
							rateChance *= Config.RATE_DROP_CHANCE_BY_ID.get(cbDropHolder.itemId);
						}
						else if (item.hasExImmediateEffect())
						{
							rateChance *= Config.RATE_HERB_DROP_CHANCE_MULTIPLIER;
						}
						else if (cbDropHolder.isRaid)
						{
							rateAmount *= Config.RATE_RAID_DROP_CHANCE_MULTIPLIER;
						}
						else
						{
							rateChance *= Config.RATE_DEATH_DROP_CHANCE_MULTIPLIER;
						}
						
						if (Config.RATE_DROP_AMOUNT_BY_ID.get(cbDropHolder.itemId) != null)
						{
							rateAmount *= Config.RATE_DROP_AMOUNT_BY_ID.get(cbDropHolder.itemId);
						}
						else if (item.hasExImmediateEffect())
						{
							rateAmount *= Config.RATE_HERB_DROP_AMOUNT_MULTIPLIER;
						}
						else if (cbDropHolder.isRaid)
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
							if (Config.PREMIUM_RATE_DROP_CHANCE_BY_ID.get(cbDropHolder.itemId) != null)
							{
								rateChance *= Config.PREMIUM_RATE_DROP_CHANCE_BY_ID.get(cbDropHolder.itemId);
							}
							else if (item.hasExImmediateEffect())
							{
								// TODO: Premium herb chance? :)
							}
							else if (cbDropHolder.isRaid)
							{
								// TODO: Premium raid chance? :)
							}
							else
							{
								rateChance *= Config.PREMIUM_RATE_DROP_CHANCE;
							}
							
							if (Config.PREMIUM_RATE_DROP_AMOUNT_BY_ID.get(cbDropHolder.itemId) != null)
							{
								rateAmount *= Config.PREMIUM_RATE_DROP_AMOUNT_BY_ID.get(cbDropHolder.itemId);
							}
							else if (item.hasExImmediateEffect())
							{
								// TODO: Premium herb amount? :)
							}
							else if (cbDropHolder.isRaid)
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
					
					builder.append("<tr>");
					builder.append("<td width=30>").append(cbDropHolder.npcLevel).append("</td>");
					builder.append("<td width=170>").append("<a action=\"bypass _bbs_npc_trace " + cbDropHolder.npcId + "\">").append("&@").append(cbDropHolder.npcId).append(";").append("</a>").append("</td>");
					builder.append("<td width=80 align=CENTER>").append(cbDropHolder.min * rateAmount).append("-").append(cbDropHolder.max * rateAmount).append("</td>");
					builder.append("<td width=50 align=CENTER>").append(chanceFormat.format(cbDropHolder.chance * rateChance)).append("%").append("</td>");
					builder.append("<td width=50 align=CENTER>").append(cbDropHolder.isSpoil ? "Spoil" : "Drop").append("</td>");
					builder.append("</tr>");
				}
				
				html = html.replace("%searchResult%", builder.toString());
				builder.setLength(0);
				
				builder.append("<tr>");
				for (page = 1; page <= pages; page++)
				{
					builder.append("<td>").append("<a action=\"bypass -h _bbs_search_drop " + itemId + " " + page + " $order $level\">").append(page).append("</a>").append("</td>");
				}
				builder.append("</tr>");
				html = html.replace("%pages%", builder.toString());
				break;
			}
			case "_bbs_npc_trace":
			{
				final int npcId = Integer.parseInt(params[1]);
				final List<NpcSpawnTemplate> spawnList = SpawnData.getInstance().getNpcSpawns(npc -> npc.getId() == npcId);
				if (spawnList.isEmpty())
				{
					player.sendMessage("Cannot find any spawn. Maybe dropped by a boss or instance monster.");
				}
				else
				{
					final NpcSpawnTemplate spawn = spawnList.get(Rnd.get(spawnList.size()));
					player.getRadar().addMarker(spawn.getSpawnLocation().getX(), spawn.getSpawnLocation().getY(), spawn.getSpawnLocation().getZ());
				}
				break;
			}
		}
		
		if (html != null)
		{
			html = html.replace("%navigation%", navigation);
			CommunityBoardHandler.separateAndSend(html, player);
		}
		
		return false;
	}
	
	/**
	 * @param itemName
	 * @return
	 */
	private String buildItemSearchResult(String itemName)
	{
		int limit = 0;
		final Set<Integer> existInDropData = DROP_INDEX_CACHE.keySet();
		final List<Item> items = new ArrayList<>();
		for (Item item : ItemTable.getInstance().getAllItems())
		{
			if (item == null)
			{
				continue;
			}
			
			if (!existInDropData.contains(item.getId()))
			{
				continue;
			}
			
			if (item.getName().toLowerCase().contains(itemName.toLowerCase()))
			{
				items.add(item);
				limit++;
			}
			
			if (limit == 14)
			{
				break;
			}
		}
		
		if (items.isEmpty())
		{
			return "<tr><td width=100 align=CENTER>No Match</td></tr>";
		}
		
		int line = 0;
		
		final StringBuilder builder = new StringBuilder(items.size() * 28);
		int i = 0;
		for (Item item : items)
		{
			i++;
			if (i == 1)
			{
				line++;
				builder.append("<tr>");
			}
			
			String icon = item.getIcon();
			if (icon == null)
			{
				icon = "icon.etc_question_mark_i00";
			}
			
			builder.append("<td>");
			builder.append("<button value=\".\" action=\"bypass _bbs_search_drop " + item.getId() + " 1 $order $level\" width=32 height=32 back=\"" + icon + "\" fore=\"" + icon + "\">");
			builder.append("</td>");
			builder.append("<td width=200>");
			builder.append("&#").append(item.getId()).append(";");
			builder.append("</td>");
			
			if (i == 2)
			{
				builder.append("</tr>");
				i = 0;
			}
		}
		
		if ((i % 2) == 1)
		{
			builder.append("</tr>");
		}
		
		if (line < 7)
		{
			for (i = 0; i < (7 - line); i++)
			{
				builder.append("<tr><td height=36></td></tr>");
			}
		}
		
		return builder.toString();
	}
	
	/**
	 * @param params
	 * @return
	 */
	private String buildItemName(String[] params)
	{
		final StringJoiner joiner = new StringJoiner(" ");
		for (int i = 1; i < params.length; i++)
		{
			joiner.add(params[i]);
		}
		return joiner.toString();
	}
	
	@Override
	public String[] getCommunityBoardCommands()
	{
		return COMMAND;
	}
}
