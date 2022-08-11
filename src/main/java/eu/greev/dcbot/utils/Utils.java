package eu.greev.dcbot.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class Utils {
    public static void sendPrivateMessage(@NotNull User user, String content) {
        user.openPrivateChannel()
                .flatMap(channel -> channel.sendMessage(content))
                .queue();
    }

    public static void sendDelayedPrivateMessage(@NotNull User user, String content, long delay, TimeUnit unit) {
        user.openPrivateChannel()
                .flatMap(channel -> channel.sendMessage(content))
                .queueAfter(delay, unit);
    }

    public static void sendPrivateEmbed(@NotNull User user, EmbedBuilder content) {
        user.openPrivateChannel()
                .flatMap(channel -> channel.sendMessageEmbeds(content.build()))
                .queue();
    }

    public static void sendDelayedPrivateEmbed(@NotNull User user, EmbedBuilder content, long delay, TimeUnit unit) {
        user.openPrivateChannel()
                .flatMap(channel -> channel.sendMessageEmbeds(content.build()))
                .queueAfter(delay, unit);
    }

    public static void deleteMessageDelayed(@NotNull Message message, long delay, TimeUnit unit) {
        message.delete().queueAfter(delay, unit);
    }
}
