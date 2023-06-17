CREATE TABLE IF NOT EXISTS tickets
(
	ticketID    INTEGER     PRIMARY KEY                 NOT NULL,

    channelID   VARCHAR     DEFAULT ""                  NOT NULL,

    threadID    VARCHAR     DEFAULT ""                  NOT NULL,

    topic       VARCHAR     DEFAULT "No topic given"    NOT NULL,

    info        VARCHAR     DEFAULT ""                  NOT NULL,

    owner       VARCHAR     DEFAULT ""                  NOT NULL,

	supporter	VARCHAR     DEFAULT ""                  NOT NULL,

	involved    VARCHAR     DEFAULT ""                  NOT NULL,

    baseMessage VARCHAR     DEFAULT ""                  NOT NULL,

	closer      VARCHAR     DEFAULT ""
);
CREATE TABLE IF NOT EXISTS messages
(
    messageID   VARCHAR     PRIMARY KEY                 NOT NULL,

    content     VARCHAR     DEFAULT ""                  NOT NULL,

    author      VARCHAR     DEFAULT ""                  NOT NULL,

    timeCreated BIGINT      DEFAULT 0                   NOT NULL,

    isDeleted   BOOL        DEFAULT 0                   NOT NULL,

    isEdited    BOOL        DEFAULT 0                   NOT NULL,

    ticketID    VARCHAR,    FOREIGN KEY (ticketID) REFERENCES tickets(ticketID)
);