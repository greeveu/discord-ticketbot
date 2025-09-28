CREATE TABLE IF NOT EXISTS tickets
(
    ticketID               INTEGER PRIMARY KEY  NOT NULL,

    channelID              VARCHAR DEFAULT ""   NOT NULL,

    threadID               VARCHAR DEFAULT ""   NOT NULL,

    isWaiting              BOOL    DEFAULT 0    NOT NULL,

    waitingSince           VARCHAR DEFAULT NULL NULL,

    remindersSent          INTEGER DEFAULT 0    NOT NULL,

    supporterRemindersSent INTEGER DEFAULT 0    NOT NULL,

    closeMessage           VARCHAR DEFAULT NULL NULL,

    category               VARCHAR DEFAULT ""   NOT NULL,

    info                   VARCHAR DEFAULT "{}" NOT NULL,

    owner                  VARCHAR DEFAULT ""   NOT NULL,

    supporter              VARCHAR DEFAULT ""   NOT NULL,

    involved               VARCHAR DEFAULT ""   NOT NULL,

    baseMessage            VARCHAR DEFAULT ""   NOT NULL,

    isOpen                 BOOL    DEFAULT 1,

    closer                 VARCHAR DEFAULT ""
);
CREATE TABLE IF NOT EXISTS messages
(
    messageID   BIGINT PRIMARY KEY NOT NULL,

    content     VARCHAR DEFAULT "" NOT NULL,

    author      VARCHAR DEFAULT "" NOT NULL,

    timeCreated BIGINT  DEFAULT 0  NOT NULL,

    isDeleted   BOOL    DEFAULT 0  NOT NULL,

    isEdited    BOOL    DEFAULT 0  NOT NULL,

    ticketID    INTEGER,
    FOREIGN KEY (ticketID) REFERENCES tickets (ticketID)
);
CREATE TABLE IF NOT EXISTS edits
(
    messageID  BIGINT,

    content    VARCHAR DEFAULT "" NOT NULL,

    timeEdited BIGINT  DEFAULT 0  NOT NULL,

    FOREIGN KEY (messageID) REFERENCES messages (messageID)
);
CREATE TABLE IF NOT EXISTS logs
(
    log         VARCHAR DEFAULT "" NOT NULL,

    timeCreated VARCHAR DEFAULT 0  NOT NULL,

    ticketID    INTEGER,
    FOREIGN KEY (ticketID) REFERENCES tickets (ticketID)
);