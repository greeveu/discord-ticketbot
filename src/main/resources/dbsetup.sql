CREATE TABLE IF NOT EXISTS tickets
(
	ticketID	VARCHAR                     NOT NULL,

	owner    	VARCHAR(20) DEFAULT ""      NOT NULL,

	supporter	VARCHAR(20) DEFAULT ""      NOT NULL,

	involved    VARCHAR     DEFAULT ""      NOT NULL,

	primary key (ticketID)
);