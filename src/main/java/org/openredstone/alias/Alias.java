package org.openredstone.alias;

import java.util.logging.Logger;
import java.util.Map;
import java.util.List;
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
import org.bukkit.event.server.ServerCommandEvent;

public class Alias extends JavaPlugin implements Listener
{
	public final Logger logger = Logger.getLogger("Minecraft");
	public final PluginDescriptionFile descFile = this.getDescription();
	public final Map aliases = this.getConfig()
		.getConfigurationSection("aliases")
		.getValues(false);

	@Override
	public void onEnable()
	{
		this.saveDefaultConfig();

		getServer().getPluginManager().registerEvents(this, this);

		logger.info(descFile.getName()+" enabled.");
		logger.info("Aliases: ");
		for (Map.Entry entry : (Iterable<Map.Entry>)aliases.entrySet())
		{
			logger.info(" - "+entry.getKey());
		}
	}

	@Override
	public void onDisable()
	{
		logger.info(descFile.getName()+" disabled.");
	}

	@EventHandler
	public void preCommandPlayer(PlayerCommandPreprocessEvent evt)
	{
		String message = evt.getMessage().substring(1);
		String[] tokens = message.split("( )+");

		if (!aliases.containsKey(tokens[0]))
			return;

		List<String> aliasParts = (List<String>)(aliases.get(tokens[0]));
		Player player = evt.getPlayer();
		evt.setCancelled(true);
		evt.setMessage("/");

		try
		{
			for (String aliasPart : aliasParts)
			{
				String cmd = parseAliasPart(aliasPart, tokens, player);
				logger.info(cmd);
				player.performCommand(cmd);
			}
		}
		catch (Exception e)
		{
			player.sendMessage(e.getMessage());
		}
	}

	private String parseAliasPart(String aliasPart, String[] tokens, Player player) throws Exception
	{
		Matcher argMatcher = Pattern.compile("\\$([0-9]+)").matcher(aliasPart);
		while (argMatcher.find())
		{
			int num = Integer.valueOf(argMatcher.group(1));

			if (num >= tokens.length)
				throw new Exception("You are missing some arguments.");

			aliasPart = aliasPart.replace(argMatcher.group(0), tokens[num]);
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
