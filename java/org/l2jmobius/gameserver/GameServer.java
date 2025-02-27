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
package org.l2jmobius.gameserver;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.concurrent.ThreadPool;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.enums.ServerMode;
import org.l2jmobius.commons.util.DeadLockDetector;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.data.sql.impl.AnnouncementsTable;
import org.l2jmobius.gameserver.data.sql.impl.CharNameTable;
import org.l2jmobius.gameserver.data.sql.impl.CharSummonTable;
import org.l2jmobius.gameserver.data.sql.impl.ClanTable;
import org.l2jmobius.gameserver.data.sql.impl.CrestTable;
import org.l2jmobius.gameserver.data.sql.impl.OfflineTraderTable;
import org.l2jmobius.gameserver.data.xml.impl.ActionData;
import org.l2jmobius.gameserver.data.xml.impl.AdminData;
import org.l2jmobius.gameserver.data.xml.impl.AlchemyData;
import org.l2jmobius.gameserver.data.xml.impl.AppearanceItemData;
import org.l2jmobius.gameserver.data.xml.impl.ArmorSetData;
import org.l2jmobius.gameserver.data.xml.impl.AttendanceRewardData;
import org.l2jmobius.gameserver.data.xml.impl.BeautyShopData;
import org.l2jmobius.gameserver.data.xml.impl.BuyListData;
import org.l2jmobius.gameserver.data.xml.impl.CategoryData;
import org.l2jmobius.gameserver.data.xml.impl.ClanHallData;
import org.l2jmobius.gameserver.data.xml.impl.ClanMasteryData;
import org.l2jmobius.gameserver.data.xml.impl.ClanShopData;
import org.l2jmobius.gameserver.data.xml.impl.ClassListData;
import org.l2jmobius.gameserver.data.xml.impl.CombinationItemsData;
import org.l2jmobius.gameserver.data.xml.impl.CubicData;
import org.l2jmobius.gameserver.data.xml.impl.DailyMissionData;
import org.l2jmobius.gameserver.data.xml.impl.DoorData;
import org.l2jmobius.gameserver.data.xml.impl.EnchantItemData;
import org.l2jmobius.gameserver.data.xml.impl.EnchantItemGroupsData;
import org.l2jmobius.gameserver.data.xml.impl.EnchantItemHPBonusData;
import org.l2jmobius.gameserver.data.xml.impl.EnchantItemOptionsData;
import org.l2jmobius.gameserver.data.xml.impl.EnchantSkillGroupsData;
import org.l2jmobius.gameserver.data.xml.impl.EnsoulData;
import org.l2jmobius.gameserver.data.xml.impl.EquipmentUpgradeData;
import org.l2jmobius.gameserver.data.xml.impl.EventEngineData;
import org.l2jmobius.gameserver.data.xml.impl.ExperienceData;
import org.l2jmobius.gameserver.data.xml.impl.ExtendDropData;
import org.l2jmobius.gameserver.data.xml.impl.FakePlayerData;
import org.l2jmobius.gameserver.data.xml.impl.FenceData;
import org.l2jmobius.gameserver.data.xml.impl.FishingData;
import org.l2jmobius.gameserver.data.xml.impl.HennaData;
import org.l2jmobius.gameserver.data.xml.impl.HitConditionBonusData;
import org.l2jmobius.gameserver.data.xml.impl.InitialEquipmentData;
import org.l2jmobius.gameserver.data.xml.impl.InitialShortcutData;
import org.l2jmobius.gameserver.data.xml.impl.ItemCrystallizationData;
import org.l2jmobius.gameserver.data.xml.impl.KarmaData;
import org.l2jmobius.gameserver.data.xml.impl.LuckyGameData;
import org.l2jmobius.gameserver.data.xml.impl.MonsterBookData;
import org.l2jmobius.gameserver.data.xml.impl.MultisellData;
import org.l2jmobius.gameserver.data.xml.impl.NpcData;
import org.l2jmobius.gameserver.data.xml.impl.NpcNameLocalisationData;
import org.l2jmobius.gameserver.data.xml.impl.OptionData;
import org.l2jmobius.gameserver.data.xml.impl.PetDataTable;
import org.l2jmobius.gameserver.data.xml.impl.PetSkillData;
import org.l2jmobius.gameserver.data.xml.impl.PlayerTemplateData;
import org.l2jmobius.gameserver.data.xml.impl.PlayerXpPercentLostData;
import org.l2jmobius.gameserver.data.xml.impl.PrimeShopData;
import org.l2jmobius.gameserver.data.xml.impl.RecipeData;
import org.l2jmobius.gameserver.data.xml.impl.ResidenceFunctionsData;
import org.l2jmobius.gameserver.data.xml.impl.SayuneData;
import org.l2jmobius.gameserver.data.xml.impl.SecondaryAuthData;
import org.l2jmobius.gameserver.data.xml.impl.SendMessageLocalisationData;
import org.l2jmobius.gameserver.data.xml.impl.ShuttleData;
import org.l2jmobius.gameserver.data.xml.impl.SiegeScheduleData;
import org.l2jmobius.gameserver.data.xml.impl.SkillData;
import org.l2jmobius.gameserver.data.xml.impl.SkillTreeData;
import org.l2jmobius.gameserver.data.xml.impl.SpawnData;
import org.l2jmobius.gameserver.data.xml.impl.StaticObjectData;
import org.l2jmobius.gameserver.data.xml.impl.TeleportListData;
import org.l2jmobius.gameserver.data.xml.impl.TeleporterData;
import org.l2jmobius.gameserver.data.xml.impl.TransformData;
import org.l2jmobius.gameserver.data.xml.impl.VariationData;
import org.l2jmobius.gameserver.datatables.BotReportTable;
import org.l2jmobius.gameserver.datatables.EventDroplist;
import org.l2jmobius.gameserver.datatables.EventPrizesTable;
import org.l2jmobius.gameserver.datatables.ItemTable;
import org.l2jmobius.gameserver.events.instanced.EventsManager;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.gui.ServerGui;
import org.l2jmobius.gameserver.handler.ConditionHandler;
import org.l2jmobius.gameserver.handler.DailyMissionHandler;
import org.l2jmobius.gameserver.handler.EffectHandler;
import org.l2jmobius.gameserver.handler.SkillConditionHandler;
import org.l2jmobius.gameserver.idfactory.IdFactory;
import org.l2jmobius.gameserver.instancemanager.*;
import org.l2jmobius.gameserver.instancemanager.games.MonsterRace;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.entity.Hero;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.olympiad.Olympiad;
import org.l2jmobius.gameserver.model.votereward.VoteSystem;
import org.l2jmobius.gameserver.network.ClientNetworkManager;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.loginserver.LoginServerNetworkManager;
import org.l2jmobius.gameserver.network.telnet.TelnetServer;
import org.l2jmobius.gameserver.scripting.ScriptEngineManager;
import org.l2jmobius.gameserver.taskmanager.TaskManager;
import org.l2jmobius.gameserver.ui.Gui;
import org.l2jmobius.gameserver.util.Broadcast;

public class GameServer
{
	private static final Logger LOGGER = Logger.getLogger(GameServer.class.getName());
	
	private final DeadLockDetector _deadDetectThread;
	private static GameServer INSTANCE;
	public static final Calendar dateTimeServerStarted = Calendar.getInstance();
	
	public long getUsedMemoryMB()
	{
		return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576;
	}
	
	public DeadLockDetector getDeadLockDetectorThread()
	{
		return _deadDetectThread;
	}
	
	public GameServer() throws Exception
	{
		final long serverLoadStart = System.currentTimeMillis();


		// GUI
		if (!GraphicsEnvironment.isHeadless())
		{
			System.out.println("GameServer: Running in GUI mode.");
			new Gui();
		}


		// Create log folder
		final File logFolder = new File(".", "log");
		logFolder.mkdir();
		
		// Create input stream for log file -- or store file data into memory
		try (InputStream is = new FileInputStream(new File("./log.cfg")))
		{
			LogManager.getLogManager().readConfiguration(is);
		}

		// Initialize config
		Config.load(ServerMode.GAME);

		ServerGui gui = new ServerGui();
		gui.init();

		printSection("Database");
		DatabaseFactory.init();
		
		printSection("ThreadPool");
		ThreadPool.init();
		
		printSection("IdFactory");
		IdFactory.init();
		if (!IdFactory.hasInitialized())
		{
			LOGGER.severe(getClass().getSimpleName() + ": Could not read object IDs from database. Please check your configuration.");
			throw new Exception("Could not initialize the ID factory!");
		}
		
		// load script engines
		printSection("Scripting Engine");
		EventDispatcher.getInstance();
		ScriptEngineManager.getInstance();
		
		printSection("Telnet");
		TelnetServer.getInstance();
		
		printSection("World");
		// start game time control early
		GameTimeController.init();
		World.getInstance();
		MapRegionManager.getInstance();
		ZoneManager.getInstance();
		DoorData.getInstance();
		FenceData.getInstance();
		AnnouncementsTable.getInstance();
		GlobalVariablesManager.getInstance();

		printSection("Data");
		ActionData.getInstance();
		CategoryData.getInstance();
		SecondaryAuthData.getInstance();
		CombinationItemsData.getInstance();
		SayuneData.getInstance();
		DailyMissionHandler.getInstance().executeScript();
		DailyMissionData.getInstance();



		printSection("Skills");
		SkillConditionHandler.getInstance().executeScript();
		EffectHandler.getInstance().executeScript();
		EnchantSkillGroupsData.getInstance();
		SkillTreeData.getInstance();
		SkillData.getInstance();
		PetSkillData.getInstance();
		
		printSection("Items");
		ConditionHandler.getInstance().executeScript();
		ItemTable.getInstance();
		EnchantItemGroupsData.getInstance();
		EnchantItemData.getInstance();
		EnchantItemOptionsData.getInstance();
		ItemCrystallizationData.getInstance();
		OptionData.getInstance();
		VariationData.getInstance();
		EnsoulData.getInstance();
		EnchantItemHPBonusData.getInstance();
		BuyListData.getInstance();
		MultisellData.getInstance();
		EquipmentUpgradeData.getInstance();
		RecipeData.getInstance();
		ArmorSetData.getInstance();
		FishingData.getInstance();
		HennaData.getInstance();
		PrimeShopData.getInstance();
		PcCafePointsManager.getInstance();
		AppearanceItemData.getInstance();
		AlchemyData.getInstance();
		CommissionManager.getInstance();
		LuckyGameData.getInstance();
		AttendanceRewardData.getInstance();
		
		printSection("Characters");
		ClassListData.getInstance();
		InitialEquipmentData.getInstance();
		InitialShortcutData.getInstance();
		ExperienceData.getInstance();
		PlayerXpPercentLostData.getInstance();
		KarmaData.getInstance();
		HitConditionBonusData.getInstance();
		PlayerTemplateData.getInstance();
		CharNameTable.getInstance();
		AdminData.getInstance();
		PetDataTable.getInstance();
		CubicData.getInstance();
		CharSummonTable.getInstance().init();
		BeautyShopData.getInstance();
		MentorManager.getInstance();
		
		if (Config.FACTION_SYSTEM_ENABLED)
		{
			FactionManager.getInstance();
		}
		
		if (Config.PREMIUM_SYSTEM_ENABLED)
		{
			LOGGER.info("PremiumManager: Premium system is enabled.");
			PremiumManager.getInstance();
		}
		
		printSection("Clans");
		ClanTable.getInstance();
		ResidenceFunctionsData.getInstance();
		ClanHallData.getInstance();
		ClanHallAuctionManager.getInstance();
		ClanEntryManager.getInstance();
		ClanMasteryData.getInstance();
		ClanShopData.getInstance();
		
		printSection("Geodata");
		GeoEngine.getInstance();
		
		printSection("NPCs");
		NpcData.getInstance();
		FakePlayerData.getInstance();
		FakePlayerChatManager.getInstance();
		ExtendDropData.getInstance();
		SpawnData.getInstance();
		MonsterBookData.getInstance();
		WalkingManager.getInstance();
		StaticObjectData.getInstance();
		ItemAuctionManager.getInstance();
		CastleManager.getInstance().loadInstances();
		GrandBossManager.getInstance();
		EventDroplist.getInstance();
		
		printSection("Instance");
		InstanceManager.getInstance();
		
		printSection("Olympiad");
		Olympiad.getInstance();
		Hero.getInstance();
		
		// Call to load caches
		printSection("Cache");
		HtmCache.getInstance();
		CrestTable.getInstance();
		TeleportListData.getInstance();
		TeleporterData.getInstance();
		MatchingRoomManager.getInstance();
		PetitionManager.getInstance();
		CursedWeaponsManager.getInstance();
		TransformData.getInstance();
		BotReportTable.getInstance();
		RankManager.getInstance();
		if (Config.SELLBUFF_ENABLED)
		{
			SellBuffsManager.getInstance();
		}
		if (Config.MULTILANG_ENABLE)
		{
			SystemMessageId.loadLocalisations();
			NpcStringId.loadLocalisations();
			SendMessageLocalisationData.getInstance();
			NpcNameLocalisationData.getInstance();
		}
		
		printSection("Scripts");
		QuestManager.getInstance();
		BoatManager.getInstance();
		AirShipManager.getInstance();
		ShuttleData.getInstance();
		GraciaSeedsManager.getInstance();
		
		try
		{
			LOGGER.info(getClass().getSimpleName() + ": Loading server scripts:");
			ScriptEngineManager.getInstance().executeScript(ScriptEngineManager.MASTER_HANDLER_FILE);
			ScriptEngineManager.getInstance().executeScriptList();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed to execute script list!", e);
		}
		
		SpawnData.getInstance().init();
		DBSpawnManager.getInstance();
		
		printSection("Event Engine");
		EventEngineData.getInstance();
		VoteSystem.initialize();

		printSection("Siege");
		SiegeManager.getInstance().getSieges();
		CastleManager.getInstance().activateInstances();
		FortManager.getInstance().loadInstances();
		FortManager.getInstance().activateInstances();
		FortSiegeManager.getInstance();
		SiegeScheduleData.getInstance();
		
		CastleManorManager.getInstance();
		SiegeGuardManager.getInstance();
		QuestManager.getInstance().report();
		
		if (Config.SAVE_DROPPED_ITEM)
		{
			ItemsOnGroundManager.getInstance();
		}
		
		if ((Config.AUTODESTROY_ITEM_AFTER > 0) || (Config.HERB_AUTO_DESTROY_TIME > 0))
		{
			ItemsAutoDestroy.getInstance();
		}
		
		MonsterRace.getInstance();
		
		TaskManager.getInstance();
		
		AntiFeedManager.getInstance().registerEvent(AntiFeedManager.GAME_ID);
		
		if (Config.ALLOW_MAIL)
		{
			MailManager.getInstance();
		}
		if (Config.CUSTOM_MAIL_MANAGER_ENABLED)
		{
			CustomMailManager.getInstance();
		}


		printSection("Tenkai Features");
		EventPrizesTable.getInstance().load();
		EventsManager.getInstance().start();
		FarmZoneManager.getInstance();
		UpgradableFarmZoneManager.getInstance();

		PunishmentManager.getInstance();
		
		Runtime.getRuntime().addShutdownHook(Shutdown.getInstance());
		
		LOGGER.info("IdFactory: Free ObjectID's remaining: " + IdFactory.size());
		
		if ((Config.OFFLINE_TRADE_ENABLE || Config.OFFLINE_CRAFT_ENABLE) && Config.RESTORE_OFFLINERS)
		{
			OfflineTraderTable.getInstance().restoreOfflineTraders();
		}
		
		if (Config.SERVER_RESTART_SCHEDULE_ENABLED)
		{
			ServerRestartManager.getInstance();
		}
		
		if (Config.DEADLOCK_DETECTOR)
		{
			_deadDetectThread = new DeadLockDetector(Duration.ofSeconds(Config.DEADLOCK_CHECK_INTERVAL), () ->
			{
				if (Config.RESTART_ON_DEADLOCK)
				{
					Broadcast.toAllOnlinePlayers("Server has stability issues - restarting now.");
					Shutdown.getInstance().startShutdown(null, 60, true);
				}
			});
			_deadDetectThread.setDaemon(true);
			_deadDetectThread.start();
		}
		else
		{
			_deadDetectThread = null;
		}
		System.gc();
		final long totalMem = Runtime.getRuntime().maxMemory() / 1048576;
		LOGGER.info(getClass().getSimpleName() + ": Started, using " + getUsedMemoryMB() + " of " + totalMem + " MB total memory.");
		LOGGER.info(getClass().getSimpleName() + ": Maximum number of connected players is " + Config.MAXIMUM_ONLINE_USERS + ".");
		LOGGER.info(getClass().getSimpleName() + ": Server loaded in " + ((System.currentTimeMillis() - serverLoadStart) / 1000) + " seconds.");
		
		ClientNetworkManager.getInstance().start();
		
		if (Boolean.getBoolean("newLoginServer"))
		{
			LoginServerNetworkManager.getInstance().connect();
		}
		else
		{
			LoginServerThread.getInstance().start();
		}
		
		Toolkit.getDefaultToolkit().beep();
	}
	
	public long getStartedTime()
	{
		return ManagementFactory.getRuntimeMXBean().getStartTime();
	}
	
	public String getUptime()
	{
		final long uptime = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
		final long hours = uptime / 3600;
		final long mins = (uptime - (hours * 3600)) / 60;
		final long secs = ((uptime - (hours * 3600)) - (mins * 60));
		if (hours > 0)
		{
			return hours + "hrs " + mins + "mins " + secs + "secs";
		}
		return mins + "mins " + secs + "secs";
	}
	
	public static void main(String[] args) throws Exception
	{
		INSTANCE = new GameServer();
	}
	
	private void printSection(String s)
	{
		s = "=[ " + s + " ]";
		while (s.length() < 61)
		{
			s = "-" + s;
		}
		LOGGER.info(s);
	}
	
	public static GameServer getInstance()
	{
		return INSTANCE;
	}
}
