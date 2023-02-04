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

	closer      VARCHAR     DEFAULT ""
);