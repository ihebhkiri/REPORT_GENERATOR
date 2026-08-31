package RHIS.com.RHIS.bot.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BotAiPropertiesTest {

    @Test
    void bindsConfigurationValues() {
        var source = new MapConfigurationPropertySource(Map.of(
                "rhis.bot.max-message-length", "1500",
                "rhis.bot.llm-timeout-seconds", "12"));
        BotAiProperties properties = new Binder(source)
                .bind("rhis.bot", Bindable.of(BotAiProperties.class))
                .get();
        assertEquals(1500, properties.getMaxMessageLength());
        assertEquals(12, properties.getLlmTimeoutSeconds());
    }

    @Test
    void providesDefaults() {
        BotAiProperties properties = new BotAiProperties();
        assertEquals(2000, properties.getMaxMessageLength());
        assertEquals(30, properties.getLlmTimeoutSeconds());
    }
}
