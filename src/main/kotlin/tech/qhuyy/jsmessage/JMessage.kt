package tech.qhuyy.jsmessage

import org.bukkit.plugin.java.JavaPlugin
import tech.qhuyy.jsmessage.manager.ConfigManager

class JMessage : JavaPlugin() {
    lateinit var configManager: ConfigManager
        private set

    override fun onEnable() {
        configManager = ConfigManager(this)
    }

    override fun onDisable() {
    }
}
