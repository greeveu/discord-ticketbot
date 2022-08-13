CREATE TABLE IF NOT EXISTS tickets
(
	ticketID	VARCHAR                     NOT NULL,

	creator 	VARCHAR(20)                 NOT NULL,

	supporter	VARCHAR(20) DEFAULT ""      NOT NULL,

	involved    VARCHAR     DEFAULT ""      NOT NULL,

	primary key (ticketID)
);