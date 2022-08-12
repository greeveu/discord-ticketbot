CREATE TABLE IF NOT EXISTS tickets
(
	ticketID	INT              NOT NULL,

	creator 	VARCHAR(20)     NOT NULL,

	supporter	VARCHAR(20)     NOT NULL,

	involved    VARCHAR,

	primary key (ticketID)
);