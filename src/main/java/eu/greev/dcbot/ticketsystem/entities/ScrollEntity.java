package eu.greev.dcbot.ticketsystem.entities;

import lombok.Data;

@Data
public class ScrollEntity {
    private final long handlerId;
    private final long userId;
    private int currentPage = 1;
    private final int maxPage;
    private final long timeCreated;
}