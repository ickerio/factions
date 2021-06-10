# Factions

Highly customizable lightweight fabric mod for minecraft.
Very much a work in progress at this point...

config
```
max_teams: -1
max_team_size: 4
team_member_power: 20
team_base_power: 20
ticks_for_power: 12000 // 10 mins
friendly_fire: false  
teleport_delay: 5
```

DB files explained
demodb.mv.db – unlike the others, this file is always created and it contains data, transaction log, and indexes
demodb.lock.db – it is a database lock file and H2 recreates it when the database is in use
demodb.trace.db – this file contains trace information
demodb.123.temp.db – used for handling blobs or huge result sets
demodb.newFile – H2 uses this file for database compaction and it contains a new database store file
demodb.oldFile – H2 also uses this file for database compaction and it contains old database store file

# License
[MIT](LICENSE)