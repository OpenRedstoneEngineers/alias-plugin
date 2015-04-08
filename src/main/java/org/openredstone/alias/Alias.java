package org.openredstone.alias;

import java.util.logging.Logger;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.Iterable;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerCommandEvent;

public class Alias extends JavaPlugin implements Listener
{
	public final Logger logger = Logger.getLogger("Minecraft");
	public final PluginDescriptionFile descFile = this.getDescription();
	public Map aliases;

	@Override
	public void onEnable()
	{
		this.saveDefaultConfig();

		getServer().getPluginManager().registerEvents(this, this);
		logger.info(descFile.getName()+" enabled.");

		loadConfig();
	}

	@Override
	public void onDisable()
	{
		logger.info(descFile.getName()+" disabled.");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel,  String[] args)
	{
		if (args.length < 1)
		{
			sender.sendMessage("Usage: alias <reload>");
			return false;
		}

		if (cmd.getName().equalsIgnoreCase("alias"))
		{
			if (args[0].equals("reload"))
			{
				if (hasPermission(sender, "alias.reload"))
				{
					reloadConfig();
					loadConfig();
					sender.sendMessage("Reloaded config.");

					return true;
				}
				else
				{
					sender.sendMessage("You don't have permission to do that.");
				}
			}
		}

		return false;
	}

	@EventHandler
	public void onPreCommandPlayer(PlayerCommandPreprocessEvent evt)
	{
		String message = evt.getMessage().substring(1);
		String[] tokens = message.split("( )+");

		if (!aliases.containsKey(tokens[0]))
			return;

		List<String> aliasParts = (List<String>)(aliases.get(tokens[0]));
		Player player = evt.getPlayer();
		evt.setCancelled(true);
		evt.setMessage("/");

		execAliasParts(aliasParts, tokens, player);
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent evt)
	{
		if (!aliases.containsKey("__join"))
			return;

		List<String> aliasParts = (List<String>)(aliases.get("__join"));
		execAliasParts(aliasParts, null, evt.getPlayer());
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent evt)
	{
		if (!aliases.containsKey("__quit"))
			return;

		List<String> aliasParts = (List<String>)(aliases.get("__quit"));
		execAliasParts(aliasParts, null, evt.getPlayer());
	}

	private void loadConfig()
	{
		aliases = getConfig()
			.getConfigurationSection("aliases")
			.getValues(false);

		logger.info("Aliases: ");
		for (Map.Entry entry : (Iterable<Map.Entry>)aliases.entrySet())
		{
			logger.info(" - "+entry.getKey());
		}
	}

	private boolean hasPermission(CommandSender sender, String perm)
	{
		if (sender instanceof Player)
		{
			if (((Player)sender).hasPermission(perm))
				return true;
			else
				return false;
		}
		else
		{
			return true;
		}
	}

	private void execAliasParts(List<String> aliasParts, String[] tokens, Player player)
	{
		List<String> commands = new ArrayList<String>();
		try
		{
			for (String aliasPart : aliasParts)
			{
				if (aliasPart.startsWith("__no-op"))
					continue;

				String cmd = parseAliasPart(aliasPart, tokens, player);
				logger.info("Alias: '"+aliasPart+"' became '"+cmd+"'");
				commands.add(cmd);
			}
		}
		catch (Exception e)
		{
			player.sendMessage(e.getMessage());
			return;
		}

		for (String cmd : commands)
		{
			player.performCommand(cmd);
		}
	}

	private String parseAliasPart(String aliasPart, String[] tokens, Player player) throws Exception
	{
		Matcher argMatcher = Pattern.compile("\\$([0-9]+)(?:(\\+))?").matcher(aliasPart);
		while (argMatcher.find())
		{
			int num = Integer.valueOf(argMatcher.group(1));

			if (num >= tokens.length)
				throw new Exception("You are missing some arguments.");

			if (argMatcher.group(2) != null && argMatcher.group(2).equals("+"))
			{
				String res = "";

				for (int i = num; i < tokens.length; ++i)
				{
					res += " "+tokens[i];
				}

				res = res.substring(1);
				aliasPart = aliasPart.replace(argMatcher.group(0), res);
			}
			else
			{
				aliasPart = aliasPart.replace(argMatcher.group(0), tokens[num]);
			}
		}

		Matcher varMatcher = Pattern.compile("\\{([a-zA-Z\\.]+)\\}").matcher(aliasPart);
		while (varMatcher.find())
		{
			String varName = varMatcher.group(1);
			String varVal = "";

			if (varName.equals("player.uuid"))
				varVal = player.getUniqueId().toString().replaceAll("-", "");
			else if (varName.equals("player.name"))
				varVal = player.getName();
			else if (varName.equals("player.nick"))
				varVal = player.getDisplayName();
			
			aliasPart = aliasPart.replace(varMatcher.group(0), varVal);
		}
		return aliasPart;
	}
}
