create table artists(
                        artist_id serial primary key,
                        name text not null
);

create table albums(
                       album_id serial primary key ,
                       title text not null ,
                       artist_id int references artists(artist_id) not null
);

create table songs(
                     song_id serial primary key,
                     album_id int references albums(album_id),
                     title text not null,
                     length int not null default 0
);