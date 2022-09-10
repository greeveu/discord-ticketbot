CREATE TABLE IF NOT EXISTS tickets
(
	ticketID	VARCHAR                                 NOT NULL,

    channelID   VARCHAR     DEFAULT ""                  NOT NULL,

    topic       VARCHAR     DEFAULT "No topic given"    NOT NULL,

	owner    	VARCHAR     DEFAULT ""                  NOT NULL,

	supporter	VARCHAR     DEFAULT ""                  NOT NULL,

	involved    VARCHAR     DEFAULT ""                  NOT NULL,

	primary key (ticketID)
);