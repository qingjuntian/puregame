delimiter |
CREATE DATABASE IF NOT EXISTS puregame
|
use puregame
|

drop table if exists dict;

drop table if exists dict_category;

drop table if exists player;

CREATE TABLE player(
    player_id int unsigned AUTO_INCREMENT, 
    player_name varchar(80) NOT NULL,
    password  varchar(256),
    display_name varchar(100),
    create_time  timestamp NOT NULL default CURRENT_TIMESTAMP,
    status       int unsigned,
    deleted tinyint default false,
    delete_time datetime,
    primary key(player_id)
) engine = InnoDB;

CREATE TABLE dict_category(
    id int unsigned AUTO_INCREMENT,
    name varchar(100) NOT NULL,
    description varchar(200) NULL,
    parent_id int NULL,
    primary key(id)
) engine = InnoDB;

CREATE TABLE game(
    game_id int unsigned AUTO_INCREMENT,
    game_type int unsigned NOT NULL,
    game_name varchar(256),
    init_score int,
    seats int,    
    roomserver_ip varchar(256),
    roomserver_port varchar(10),
    primary key(game_id)        
) engine = InnoDB;


CREATE TABLE player_level(
    player_level_id int unsigned AUTO_INCREMENT,
    player_id int unsigned,
    game_id int unsigned,
    score int,
    primary key(player_level_id)
) engine = InnoDB;

CREATE TABLE dict(
    id int unsigned AUTO_INCREMENT,
    code int NOT NULL,
    name varchar(100) NOT NULL,
    description varchar(200) NULL,
    alias varchar(100) NULL,
    category_id int unsigned NOT NULL,
    parent_id int NULL,
    primary key(id),
    FOREIGN KEY(category_id) REFERENCES dict_category(id)
) engine = InnoDB;

|


INSERT INTO player(
        player_name,
        password,
        display_name
        )
    values (
    'qjun', '123456', 'Tian Qingjun'
); 

INSERT INTO player(
        player_name,
        password,
        display_name
        )
    values (
    'peter', '123456', 'ZhengP'
); 

|

delimiter ;
