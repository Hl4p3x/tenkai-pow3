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
package org.l2jmobius.gameserver.ui;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.GameServer;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.util.Locator;

/**
 * @author Mobius
 */
public class SystemPanel extends JPanel
{
	private static final long START_TIME = System.currentTimeMillis();
	
	public static volatile int MAX_CONNECTED_COUNT = 0;
	public static volatile int OFFLINE_TRADE_COUNT = 0;
	
	public SystemPanel()
	{
		setBackground(Color.WHITE);
		setBounds(500, 20, 284, 140);
		setBorder(new LineBorder(new Color(0, 0, 0), 1, false));
		setOpaque(true);
		setLayout(null);
		
		final JLabel lblProtocol = new JLabel("Protocol");
		lblProtocol.setFont(new Font("Monospaced", Font.PLAIN, 16));
		lblProtocol.setBounds(10, 5, 264, 17);
		add(lblProtocol);
		
		final JLabel lblConnected = new JLabel("Connected");
		lblConnected.setFont(new Font("Monospaced", Font.PLAIN, 16));
		lblConnected.setBounds(10, 23, 264, 17);
		add(lblConnected);
		
		final JLabel lblMaxConnected = new JLabel("Max connected");
		lblMaxConnected.setFont(new Font("Monospaced", Font.PLAIN, 16));
		lblMaxConnected.setBounds(10, 41, 264, 17);
		add(lblMaxConnected);
		
		final JLabel lblOfflineShops = new JLabel("Offline trade");
		lblOfflineShops.setFont(new Font("Monospaced", Font.PLAIN, 16));
		lblOfflineShops.setBounds(10, 59, 264, 17);
		add(lblOfflineShops);
		
		final JLabel lblElapsedTime = new JLabel("Elapsed time");
		lblElapsedTime.setFont(new Font("Monospaced", Font.PLAIN, 16));
		lblElapsedTime.setBounds(10, 77, 264, 17);
		add(lblElapsedTime);
		
		final JLabel lblJavaVersion = new JLabel("Build JDK");
		lblJavaVersion.setFont(new Font("Monospaced", Font.PLAIN, 16));
		lblJavaVersion.setBounds(10, 95, 264, 17);
		add(lblJavaVersion);
		
		final JLabel lblBuildDate = new JLabel("Build date");
		lblBuildDate.setFont(new Font("Monospaced", Font.PLAIN, 16));
		lblBuildDate.setBounds(10, 113, 264, 17);
		add(lblBuildDate);
		
		// Set initial values.
		lblProtocol.setText("Protocol: 0");
		lblConnected.setText("Connected: 0");
		lblMaxConnected.setText("Max connected: 0");
		lblOfflineShops.setText("Offline trade: 0");
		lblElapsedTime.setText("Elapsed: 0 sec");
		lblJavaVersion.setText("Java version: " + System.getProperty("java.version"));
		lblBuildDate.setText("Build date: Unavailable");
		try
		{
			final File jarName = Locator.getClassSource(GameServer.class);
			final JarFile jarFile = new JarFile(jarName);
			final Attributes attrs = jarFile.getManifest().getMainAttributes();
			lblBuildDate.setText("Build date: " + attrs.getValue("Build-Date").split(" ")[0]);
			jarFile.close();
		}
		catch (Exception e)
		{
			// Handled above.
		}
		
		// Initial update task.
		new Timer().schedule(new TimerTask()
		{
			@Override
			public void run()
			{
				lblProtocol.setText((Config.PROTOCOL_LIST.size() > 1 ? "Protocols: " : "Protocol: ") + (Config.SERVER_LIST_TYPE >= 400 ? "Classic " : "") + Config.PROTOCOL_LIST.toString());
			}
		}, 4500);
		
		// Repeating elapsed time task.
		new Timer().scheduleAtFixedRate(new TimerTask()
		{
			@Override
			public void run()
			{
				final int playerCount = World.getInstance().getPlayers().size();
				if (MAX_CONNECTED_COUNT < playerCount)
				{
					MAX_CONNECTED_COUNT = playerCount;
				}
				lblConnected.setText("Connected: " + playerCount);
				lblMaxConnected.setText("Max connected: " + MAX_CONNECTED_COUNT);
				lblOfflineShops.setText("Offline trade: " + OFFLINE_TRADE_COUNT);
				lblElapsedTime.setText("Elapsed: " + getDurationBreakdown(System.currentTimeMillis() - START_TIME));
			}
		}, 1000, 1000);
	}
	
	static String getDurationBreakdown(long millis)
	{
		final long days = TimeUnit.MILLISECONDS.toDays(millis);
		millis -= TimeUnit.DAYS.toMillis(days);
		final long hours = TimeUnit.MILLISECONDS.toHours(millis);
		millis -= TimeUnit.HOURS.toMillis(hours);
		final long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
		millis -= TimeUnit.MINUTES.toMillis(minutes);
		final long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
		return (days + "d " + hours + "h " + minutes + "m " + seconds + "s");
	}
}
