/*
 * GoldBank
 * Copyright (c) 2014, Maxim Roncacé <http://bitbucket.org/mproncace>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.amigocraft.GoldBank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.api.event.state.ServerStartedEvent;
import org.spongepowered.api.event.state.ServerStoppingEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.util.event.Subscribe;

@Plugin(id = "goldbank", name = "GoldBank", version = "4.0.0-SNAPSHOT")
public class GoldBank {

	private static PluginContainer plugin;
	private static final Logger log = LoggerFactory.getLogger(GoldBank.class);

	@Subscribe
	public void onServerStarted(ServerStartedEvent event){
		plugin = event.getGame().getPluginManager().getPlugin("goldbank").get();
		log.info(getPlugin().getName() + " v" + getPlugin().getVersion() + " has been enabled!");
	}

	@Subscribe
	public void onServerStopping(ServerStoppingEvent event){
		getLogger().info(getPlugin().getName() + " v" + getPlugin().getVersion() + " has been disabled!");
	}

	public static PluginContainer getPlugin(){
		return plugin;
	}

	public static Logger getLogger(){
		return log;
	}

}
