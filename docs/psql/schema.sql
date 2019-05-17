drop table if exists sessions;
drop table if exists memberships;
drop table if exists catalogs;
drop table if exists users;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS users (
	username varchar(50) PRIMARY KEY,
	password_hash varchar NOT NULL,
	session_id varchar(50) UNIQUE,
	public_key text
);

CREATE TABLE IF NOT EXISTS sessions (
	session_id varchar(50) PRIMARY KEY REFERENCES users(session_id),
	ip varchar(16) NOT NULL,
	user_agent varchar(50) NOT NULL,
	user_data text,
	last_activity numeric(10) NOT NULL CHECK (last_activity > 0)
);

CREATE TABLE IF NOT EXISTS catalogs (
	catalog_id numeric(10) CHECK (catalog_id > 0) PRIMARY KEY,
	catalog_title varchar(50) NOT NULL,
	slices text NOT NULL
);

CREATE TABLE IF NOT EXISTS memberships (
    catalog_id numeric(10) REFERENCES catalogs(catalog_id),
    username varchar(50) REFERENCES users(username),
    parent_folder_drive_id varchar(64),
    catalog_drive_id varchar(64),
    PRIMARY KEY (catalog_id, username)
);
