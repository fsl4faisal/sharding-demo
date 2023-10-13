set -x #buidling the image


#spin up the containers
docker container stop shard1 shard2 shard3 pgadmin
docker container rm shard1 shard2 shard3 pgadmin
docker run -d --name shard1 -e POSTGRES_PASSWORD=password -p 5432:5432 pgshard
docker run -d --name shard2 -e POSTGRES_PASSWORD=password -p 5433:5432 pgshard
docker run -d --name shard3 -e POSTGRES_PASSWORD=password -p 5434:5432 pgshard


#spin up pgadmin
docker run -d -e PGADMIN_DEFAULT_EMAIL="faisal@gmail.com" -e PGADMIN_DEFAULT_PASSWORD="password" -p 5555:80 --name pgadmin dpage/pgadmin4


