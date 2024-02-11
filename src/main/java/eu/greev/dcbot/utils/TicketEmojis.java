package eu.greev.dcbot.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.emoji.Emoji;

@Getter
@RequiredArgsConstructor
public enum TicketEmojis {
    BACKWARDS(Emoji.fromUnicode("◀️")),
    FORWARDS(Emoji.fromUnicode("▶️"));

    private final Emoji emoji;
}