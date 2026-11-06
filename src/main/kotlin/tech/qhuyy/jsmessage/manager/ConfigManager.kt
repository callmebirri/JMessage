package tech.qhuyy.jsmessage.manager

import org.bukkit.configuration.file.FileConfiguration
import tech.qhuyy.jsmessage.JMessage

@Suppress("unused")
class ConfigManager(
    private val plugin: JMessage
) {
    private var config: FileConfiguration = plugin.config

    init {
        plugin.saveDefaultConfig()
        reload()
    }

    fun reload() {
        plugin.reloadConfig()
        config = plugin.config
    }
}
