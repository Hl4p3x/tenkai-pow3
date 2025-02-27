package org.l2jmobius.gameserver.events.instanced;

import org.l2jmobius.Config;
import org.l2jmobius.commons.concurrent.ThreadPool;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.olympiad.OlympiadManager;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.Broadcast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class EventsMatchMaker {

    public static EventsMatchMaker instance = null;

    private MatchMakingTask pvpTask;
    private MatchMakingTask specialTask;
    public ConcurrentHashMap<Integer, EventInstance> Instances = new ConcurrentHashMap<>();
    private int nextInstanceId = 1;

    public static EventsMatchMaker getInstance() {
        if (instance == null) {
            instance = new EventsMatchMaker();
        }
        return instance;
    }

    public void start() {
        if (Config.INSTANCED_EVENT_ENABLED) {
            pvpTask = new MatchMakingTask(true);
            specialTask = new MatchMakingTask(false);
            ThreadPool.scheduleAtFixedRate(pvpTask, 60000L, 10000L);
            ThreadPool.scheduleAtFixedRate(specialTask, 60000L, 10000L);
            System.out.println("Instanced Events scheduled.");
        } else {
            System.out.println("Instanced Events are disabled.");
        }
    }

    public MatchMakingTask getPvpTask() {
        return pvpTask;
    }

    public MatchMakingTask getSpecialTask() {
        return specialTask;
    }

    class MatchMakingTask implements Runnable {
        private boolean pvp = true;
        private EventConfig currentConfig = null;
        private Map<Integer, PlayerInstance> registeredPlayers = new HashMap<>();
        private int prepareAttempts = 0;
        private int fillProgress = 0;

        MatchMakingTask(boolean pvp) {
            this.pvp = pvp;
            currentConfig = new EventConfig(pvp);
        }

        @Override
        public void run() {
            List<Integer> toRemove = new ArrayList<>();
            try {
                for (EventInstance event : Instances.values()) {
                    if (event == null) {
                        continue;
                    }

                    if (event.isState(EventInstance.EventState.INACTIVE)) {
                        toRemove.add(event.getId());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Instances.clear();
            }

            for (int eventId : toRemove) {
                Instances.remove(eventId);
            }

            // Prepare an instance
            while (prepare()) {
            }
        }

        public EventConfig getCurrentConfig() {
            return currentConfig;
        }

        public Map<Integer, PlayerInstance> getRegisteredPlayers() {
            return registeredPlayers;
        }

        public int getFillProgress() {
            return fillProgress;
        }

        private boolean prepare() {
            if (registeredPlayers.isEmpty()) {
                return false;
            }

            // First sort the registered players
            int[][] sorted = new int[registeredPlayers.size()][2];
            int i = 0;
            for (PlayerInstance player : registeredPlayers.values()) {
                if (player == null || OlympiadManager.getInstance().isRegisteredInComp(player) || player.isInOlympiadMode() ||
                        player.isOlympiadStart() || player.isFlyingMounted() || player.inObserverMode()) {
                    continue;
                }

                int objId = player.getObjectId();
                int strPoints = player.getSTR();
                // Find the index of where the current player should be put
                int j = 0;
                while (j < i && strPoints < sorted[j][1]) {
                    j++;
                }
                // Move the rest
                for (int k = i; k > j; k--) {
                    int temp1 = sorted[k][0];
                    int temp2 = sorted[k][1];
                    sorted[k][0] = sorted[k - 1][0];
                    sorted[k][1] = sorted[k - 1][1];
                    sorted[k - 1][0] = temp1;
                    sorted[k - 1][1] = temp2;
                }
                // And put the current player in the blank space
                sorted[j][0] = objId;
                sorted[j][1] = strPoints;

                i++;
            }

            // Next divide all the registered players in groups, depending on their strenght
            int[] group = null;
            i = 0;
            int bestFillProgress = 0;
            while (i < sorted.length) {
                group = new int[currentConfig.getLocation().getMaxPlayers()];
                int points = sorted[i][1];
                int j = 0;
                while (i + j < sorted.length && (j < currentConfig.getMinPlayers() ||
                        j < currentConfig.getLocation().getMaxPlayers() && (points - sorted[i + j][1] < 15000 || currentConfig.hasNoLevelLimits()))) {
                    group[j] = sorted[i + j][0];
                    j++;
                }

                int minPlayers = currentConfig.getMinPlayers();
                if (prepareAttempts < 100) {
                    minPlayers += (currentConfig.getLocation().getMaxPlayers() - currentConfig.getMinPlayers()) * (100 - prepareAttempts) / 100;
                }

                if (j >= minPlayers) {
                    bestFillProgress = 100;
                    break;
                }

                if (100 * j / minPlayers > bestFillProgress) {
                    bestFillProgress = 100 * j / minPlayers;
                }

                group = null;
                i += j;
            }

            fillProgress = bestFillProgress;

            // And finally create the event instances according to the generated groups
            if (group != null) {
                EventInstance ei = createInstance(nextInstanceId++, group, currentConfig);
                if (ei != null) {
                    Instances.put(ei.getId(), ei);
                    currentConfig = new EventConfig(pvp);
                    prepareAttempts = 0;
                    fillProgress = 0;
                    String type = pvp ? "PvP" : "special";
                    Broadcast.toAllOnlinePlayers(
                                    "The " + type + " event has been set up to start. The next event will be a " + currentConfig.getEventString() +
                                            ". Type .event to join.");

                    for (EventTeam team : ei.getTeams()) {
                        for (int memberId : team.getParticipatedPlayers().keySet()) {
                            registeredPlayers.remove(memberId);
                        }
                    }
                }

                return true;
            }

            prepareAttempts++;

            if (prepareAttempts > 200) {
                currentConfig = new EventConfig(pvp);
                prepareAttempts = 0;
                fillProgress = 0;
                String type = pvp ? "PvP" : "special";
                Broadcast.toAllOnlinePlayers("The " + type + " event couldn't start after almost an hour waiting, shuffling configuration.");
                Broadcast.toAllOnlinePlayers("The new " + type + " event will be a " + currentConfig.getEventString() + ". Type .event to join.");
            }

            return false;
        }
    }

    public void onLogin(PlayerInstance playerInstance) {
        if (playerInstance != null && isPlayerParticipant(playerInstance.getObjectId())) {
            removeParticipant(playerInstance.getObjectId());
            if (playerInstance.getEvent() != null) {
                playerInstance.stopAllEffects();
                playerInstance.getEvent().onLogin(playerInstance);
            }
        }
    }

    public void onLogout(PlayerInstance playerInstance) {
        if (playerInstance != null && isPlayerParticipant(playerInstance.getObjectId())) {
            if (playerInstance.getEvent() != null) {
                playerInstance.stopAllEffects();
                playerInstance.getEvent().onLogout(playerInstance);
            }

            removeParticipant(playerInstance.getObjectId());
        }
    }

    public void join(PlayerInstance playerInstance, boolean pvp) {
        if (isPlayerParticipant(playerInstance.getObjectId())) {
            return;
        }

        NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);

        if (playerInstance.isCursedWeaponEquipped()) {
            npcHtmlMessage.setHtml(
                    "<html><head><title>Instanced Events</title></head><body>Cursed weapon owners are not allowed to participate.</body></html>");
        } else if (OlympiadManager.getInstance().isRegisteredInComp(playerInstance)) {
            npcHtmlMessage.setHtml(
                    "<html><head><title>Instanced Events</title></head><body>You can not participate when registered for Olympiad.</body></html>");
        } else if (playerInstance.getReputation() < 0) {
            npcHtmlMessage.setHtml(
                    "<html><head><title>Instanced Events</title></head><body>Chaotic players are not allowed to participate.</body></html>");
        } else if (playerInstance.isJailed()) {
            npcHtmlMessage.setHtml(
                    "<html><head><title>Instanced Events</title></head><body>You cannot participate, you must wait your jail time.</body></html>");
        } else if (playerInstance.isCastingNow()) {
            npcHtmlMessage.setHtml("<html><head><title>Instanced Events</title></head><body>You can't register while casting a skill.</body></html>");
        } else if (checkDualBox(playerInstance)) {
            npcHtmlMessage.setHtml(
                    "<html><head><title>Instanced Events</title></head><body>You have another character already registered for this event!</body></html>");
        } else if (playerInstance.getInstanceId() != 0) {
            npcHtmlMessage.setHtml(
                    "<html><head><title>Instanced Events</title></head><body>You can't join one event while in other instance!</body></html>");
        } else if (playerInstance.isInDuel() || playerInstance.isDead()) {
            npcHtmlMessage.setHtml("<html><head><title>Instanced Events</title></head><body>You can't join one event at this moment!</body></html>");
        } else {
            if (addParticipant(playerInstance, pvp)) {
                CommunityBoardHandler.getInstance().handleParseCommand("_bbscustom;currentEvent", playerInstance);
            }

            return;
        }

        playerInstance.sendPacket(npcHtmlMessage);
    }

    public synchronized boolean addParticipant(PlayerInstance playerInstance, boolean pvp) {
        // Check for nullpoitner
        if (playerInstance == null) {
            return false;
        }

        if (pvp) {
            pvpTask.getRegisteredPlayers().put(playerInstance.getObjectId(), playerInstance);
        } else {
            specialTask.getRegisteredPlayers().put(playerInstance.getObjectId(), playerInstance);
        }

        return true;
    }

    public void leave(PlayerInstance playerInstance) {
        if (!isPlayerParticipant(playerInstance.getObjectId())) {
            return;
        }

        //LasTravel: If the event is started the player shouldn't be allowed to leave
        if (playerInstance.getEvent() != null && playerInstance.getEvent().isState(EventInstance.EventState.STARTED)) {
            return;
        }

        if (removeParticipant(playerInstance.getObjectId())) {
            CommunityBoardHandler.getInstance().handleParseCommand("_bbscustom;currentEvent", playerInstance);
        }
    }

    public boolean removeParticipant(int playerObjectId) {
        if (pvpTask.getRegisteredPlayers().remove(playerObjectId) != null || specialTask.getRegisteredPlayers().remove(playerObjectId) != null) {
            return true;
        }

        EventInstance event = getParticipantEvent(playerObjectId);
        if (event != null) {
            return event.removeParticipant(playerObjectId);
        }

        return false;
    }

    public String getEventInfoPage(PlayerInstance player) {

        String a = null;

        if (player.getEvent() != null && player.getEvent().isState(EventInstance.EventState.STARTED)) {
            a = HtmCache.getInstance().getHtm(null, "CommunityBoard/runningEvent.htm");

            a = a.replace("%runningEventInfo%", player.getEvent().getInfo(player));

            return a;
        } else {
            a = HtmCache.getInstance().getHtm(null, "CommunityBoard/joinEvents.htm");
        }

        //PvP Event
        a = a.replace("%pvpEventName%", getInstance().getPvpTask().getCurrentConfig().getEventName());
        a = a.replace("%pvpEventLocation%", getInstance().getPvpTask().getCurrentConfig().getEventLocationName());
        a = a.replace("%pvpStartProgress%", getInstance().getPvpTask().getFillProgress() + "%");
        a = a.replace("%pvpEventId%", String.valueOf(getInstance().getPvpTask().getCurrentConfig().getEventImageId()));
        a = a.replace("%pvpInfoLink%", String.valueOf(getInstance().getPvpTask().getCurrentConfig().getType()));

        if (pvpTask.getRegisteredPlayers().isEmpty()) {
            a = a.replace("%pvpEventPlayers%", "");
            a = a.replace("Registred Players At PvP Event", "");
        } else {
            a = a.replace("%pvpEventPlayers%", getPvPEventRegistredPlayers(player));
        }

        //Special Event
        a = a.replace("%specialStartProgress%", getInstance().getSpecialTask().getFillProgress() + "%");
        a = a.replace("%specialEventLocation%", getInstance().getSpecialTask().getCurrentConfig().getEventLocationName());
        a = a.replace("%specialEventName%", getInstance().getSpecialTask().getCurrentConfig().getEventName());
        a = a.replace("%specialEventId%", String.valueOf(getInstance().getSpecialTask().getCurrentConfig().getEventImageId()));
        a = a.replace("%specialInfoLink%", String.valueOf(getInstance().getSpecialTask().getCurrentConfig().getType()));

        if (specialTask.getRegisteredPlayers().isEmpty()) {
            a = a.replace("%specialEventPlayers%", "");
            a = a.replace("Registred Players At Special Event", "");
        } else {
            a = a.replace("%specialEventPlayers%", getSpecialEventRegistredPlayers(player));
        }

        //Both events
        if (isPlayerParticipant(player.getObjectId())) {
            a = a.replace("%pvpEventJoinButton%", "");
            a = a.replace("%specialEventJoinButton%", "");
            a = a.replace("%leaveButton%",
                    "<button value=\"Leave Match making\" action=\"bypass -h TenkaiEventLeave\" width=630 height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
        } else {
            a = a.replace("%leaveButton%", "");
            if (player.getLevel() < 99) {
                a = a.replace("%pvpEventJoinButton%", "<font color=FF0000>You can't join a PvP event until you get stronger!</font>");
            } else {
                a = a.replace("%pvpEventJoinButton%",
                        "<button value=\"Join Match making (PvP)\" action=\"bypass -h TenkaiEventJoin true\" width=255 height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
            }
            a = a.replace("%specialEventJoinButton%",
                    "<button value=\"Join Match making (Special)\" action=\"bypass -h TenkaiEventJoin false\" width=255 height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
        }

        //Observe part?
        if (Instances.isEmpty()) {
            a = a.replace("Current Observable Events", "");
            a = a.replace("%observeEvents%", "");
        } else {
            int b = Instances.size();

            int d = 1;

            int e = 1;

            String c = "";

            for (EventInstance event : Instances.values()) {
                if (event.isState(EventInstance.EventState.STARTED)) {
                    if (d == 1) {
                        c += "<tr>";
                    }

                    c += "<td><button value=\"" + event.getType().toString() + " #" + e + "\" action=\"bypass -h TenkaiEventObserve " +
                            event.getId() + "\" width=90 height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>";

                    d++;

                    b--;

                    e++;

                    if (d == 6 || b == 0) {
                        d = 1;

                        c += "</tr>";
                    }
                }
            }

            a = a.replace("%observeEvents%", c);
            a += "<br><br><br><br>";
        }

        return a;
    }

    private String getPvPEventRegistredPlayers(PlayerInstance player) {
        String a = "";

        if (pvpTask.getRegisteredPlayers().isEmpty()) {
            return a;
        }

        for (PlayerInstance participant : pvpTask.getRegisteredPlayers().values()) {
            if (participant == null) {
                continue;
            }

            a += getPlayerString(participant, player) + ", ";
        }

        a = a.substring(0, a.length() - 2);

        a += ".";

        return a;
    }

    private String getSpecialEventRegistredPlayers(PlayerInstance player) {
        String a = "";

        if (specialTask.getRegisteredPlayers().isEmpty()) {
            return a;
        }

        for (PlayerInstance participant : specialTask.getRegisteredPlayers().values()) {
            if (participant == null) {
                continue;
            }

            a += getPlayerString(participant, player) + ", ";
        }

        a = a.substring(0, a.length() - 2);

        a += ".";

        return a;
    }

    public String getPlayerString(PlayerInstance player, PlayerInstance reader) {
        String color = "FFFFFF";
        if (player == reader) {
            color = "FFFF00";
        } else if (player.getFriendList().contains(reader.getObjectId())) {
            color = "00FFFF";
        } else if (reader.getParty() != null && reader.getParty() == player.getParty()) {
            color = "00FF00";
        } else if (reader.getClan() != null) {
            if (reader.getClanId() > 0 && reader.getClanId() == player.getClanId()) {
                color = "8888FF";
            } else if (reader.getAllyId() > 0 && reader.getAllyId() == player.getAllyId()) {
                color = "88FF88";
            } else if (reader.getClan().isAtWarWith(player.getClanId())) {
                color = "CC0000";
            }
        }
        return "<font color=\"" + color + "\">" + player.getName() + "</font>";
    }

    public int getParticipantEventId(int playerObjectId) {
        for (EventInstance event : Instances.values()) {
            if (event.isPlayerParticipant(playerObjectId)) {
                return event.getId();
            }
        }
        return -1;
    }

    public EventInstance getParticipantEvent(int playerObjectId) {
        for (EventInstance event : Instances.values()) {
            if (event.isPlayerParticipant(playerObjectId)) {
                return event;
            }
        }
        return null;
    }

    public byte getParticipantTeamId(int playerObjectId) {
        EventInstance event = getParticipantEvent(playerObjectId);
        if (event == null) {
            return -1;
        }
        return event.getParticipantTeamId(playerObjectId);
    }

    public EventTeam getParticipantTeam(int playerObjectId) {
        EventInstance event = getParticipantEvent(playerObjectId);
        if (event == null) {
            return null;
        }
        return getParticipantEvent(playerObjectId).getParticipantTeam(playerObjectId);
    }

    public EventTeam getParticipantEnemyTeam(int playerObjectId) {
        EventInstance event = getParticipantEvent(playerObjectId);
        if (event == null) {
            return null;
        }
        return getParticipantEvent(playerObjectId).getParticipantEnemyTeam(playerObjectId);
    }

    public boolean isPlayerParticipant(int playerObjectId) {
        if (pvpTask.getRegisteredPlayers().containsKey(playerObjectId) || specialTask.getRegisteredPlayers().containsKey(playerObjectId)) {
            return true;
        }

        for (EventInstance event : Instances.values()) {
            if (event.isPlayerParticipant(playerObjectId)) {
                return true;
            }
        }
        return false;
    }

    public EventInstance createInstance(int id, int[] group, EventConfig config) {
        // A map of lists to access the players sorted by class
        Map<Integer, List<PlayerInstance>> playersByClass = new HashMap<>();
        // Classify the players according to their class
        for (int playerId : group) {
            if (playerId == 0) {
                continue;
            }

            PlayerInstance player = World.getInstance().getPlayer(playerId);
            int classId = player.getClassId().getId();
            if (classId == -1) {
                classId = 147;
            }

            List<PlayerInstance> players = playersByClass.get(classId);
            if (players == null) {
                players = new ArrayList<>();
                playersByClass.put(classId, players);
            }

            players.add(player);
        }

        // If we found none, don't do anything
        if (playersByClass.isEmpty()) {
            return null;
        }

        // Create the event and fill it with the players, in class order
        EventInstance event = config.createInstance(id);
        for (int classId = 139; classId <= 190; classId++) {
            List<PlayerInstance> players = playersByClass.get(classId);
            if (players == null) {
                continue;
            }

            for (PlayerInstance player : players) {
                event.addParticipant(player);
            }
        }

        return event;
    }

    private boolean checkDualBox(PlayerInstance player) {
        if (player == null) {
            return false;
        }

        for (PlayerInstance registered : pvpTask.getRegisteredPlayers().values()) {
            if (registered == null) {
                continue;
            }

            if (player.getIPAddress().equalsIgnoreCase(registered.getIPAddress())) {
                return true;
            }
        }

        for (PlayerInstance registered : specialTask.getRegisteredPlayers().values()) {
            if (registered == null) {
                continue;
            }

            if (player.getIPAddress().equalsIgnoreCase(registered.getIPAddress()) ) {
                return true;
            }
        }

        return false;

        //TODO LasTravel: Hwid check don't work if we don't have LG
		/*String hwId = player.getClient().getHWId();
        for (Player registered : registeredPlayers.values())
		{
			if (registered.getClient() != null
					&& registered.getClient().getHWId() != null
					&& registered.getClient().getHWId().equals(hwId))
				return true;
		}
		return false;*/
    }

    public void handleEventBypass(PlayerInstance activeChar, String command) {
        if (activeChar == null) {
            return;
        }

        if (command.startsWith("TenkaiEventJoin")) {
            join(activeChar, Boolean.parseBoolean(command.split(" ", 0)[1]));
        } else if (command.equals("TenkaiEventLeave")) {
            leave(activeChar);
        } else if (command.startsWith("TenkaiEventObserve")) {
            int eventId = Integer.valueOf(command.substring(19));

            if (Instances.get(eventId) != null) {
                Instances.get(eventId).observe(activeChar);
            }
        }

		/*else if (command.startsWith("TenkaiEventParticipation"))
		{
			int eventId = Integer.valueOf(command.substring(25));
			if (Events.getInstance().Events.getInstance().get(eventId) != null)
				Events.getInstance().Events.getInstance().get(eventId).join(activeChar);
		}
		else if (command.startsWith("TenkaiEventStatus"))
		{
			int eventId = Integer.valueOf(command.substring(18));
			if (Events.getInstance().Events.getInstance().get(eventId) != null)
				Events.getInstance().Events.getInstance().get(eventId).eventInfo(activeChar);
		}*/
    }
}
