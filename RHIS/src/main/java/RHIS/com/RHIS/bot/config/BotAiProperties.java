package RHIS.com.RHIS.bot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration du module bot de génération de rapports par langage naturel.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "rhis.bot")
public class BotAiProperties {

    private int maxMessageLength = 2000;
    private int llmTimeoutSeconds = 30;
}
