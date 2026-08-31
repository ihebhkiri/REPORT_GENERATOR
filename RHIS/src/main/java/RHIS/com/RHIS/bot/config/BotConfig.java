package RHIS.com.RHIS.bot.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Enregistre les propriétés du module bot dans le contexte Spring.
 */
@Configuration
@EnableConfigurationProperties(BotAiProperties.class)
public class BotConfig {
}
