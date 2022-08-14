package eu.greev.dcbot.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;

public class Utils {
    public static void sendPrivateMessage(@NotNull User user, String content) {
        user.openPrivateChannel()
                .flatMap(channel -> channel.sendMessage(content))
                .complete();
    }

    public static void sendPrivateEmbed(@NotNull User user, EmbedBuilder content) {
        user.openPrivateChannel()
                .flatMap(channel -> channel.sendMessageEmbeds(content.build()))
                .complete();
    }
}