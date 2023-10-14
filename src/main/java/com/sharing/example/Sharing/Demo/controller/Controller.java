package com.sharing.example.Sharing.Demo.controller;

import com.sharing.example.Sharing.Demo.service.Service;
import com.sharing.example.Sharing.Demo.util.DatabaseConfig;
import com.sharing.example.Sharing.Demo.util.ShardingConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.postgresql.ds.PGSimpleDataSource;
import org.postgresql.shaded.com.ongres.scram.common.bouncycastle.base64.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static com.sharing.example.Sharing.Demo.util.ConsistentHashing.SERVER_NAME;

@RestController
public class Controller implements Service {
    private static final Log LOG = LogFactory.getLog(Controller.class);
    private final Environment environment;
    private final ShardingConfig shardingConfig;
    private final DatabaseConfig databaseConfig;
    private String url;

    @Autowired
    public Controller(Environment environment, ShardingConfig shardingConfig, DatabaseConfig databaseConfig) {
        this.environment = environment;
        this.shardingConfig = shardingConfig;
        this.databaseConfig = databaseConfig;
    }

    @Bean
    public Map<String, DataSource> dataSourceMap() {
        var map = new HashMap<String, DataSource>();
        map.put(SERVER_NAME + 0, buildDataSourceFrom("spring.datasource1.url"));
        map.put(SERVER_NAME + 1, buildDataSourceFrom("spring.datasource2.url"));
        map.put(SERVER_NAME + 2, buildDataSourceFrom("spring.datasource3.url"));
        return map;
    }

    private DataSource buildDataSourceFrom(String url) {
        var dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.url(environment.getProperty(url));
        dataSourceBuilder.username(environment.getProperty("spring.datasource1.username"));
        dataSourceBuilder.password(environment.getProperty("spring.datasource1.password"));
        dataSourceBuilder.driverClassName(environment.getProperty("spring.datasource1.driver"));
        dataSourceBuilder.type(PGSimpleDataSource.class);
        return dataSourceBuilder.build();
    }

    @Override
    //https://localhost:8081/fhy2h
    public Triplet get(String urlId) {
        var consistentHashing = shardingConfig.consistentHashing();
        var server = consistentHashing.getServer(urlId);
        var dataSource = getDataSourceFor(server);
        var connection = createOrGetConnection(dataSource);
        return getFromDatabase(urlId, connection).add(server);
    }

    private static Pair getFromDatabase(String urlId, Connection connection) {
        try {
            Pair<String, String> url = performQuery(urlId, connection);
            if (url != null) {
                return url;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Pair.with("404", "Not Found");
    }

    private static Pair<String, String> performQuery(String urlId, Connection connection) throws SQLException {
        var query = String.format("SELECT URL,URL_ID FROM URL_TABLE WHERE URL_ID=?");
        var prepareStatement = connection.prepareStatement(query);
        prepareStatement.setString(1, urlId);
        var resultSet = prepareStatement.executeQuery();
        while (resultSet.next()) {
            var url = resultSet.getString("URL");
            return Pair.with(url, urlId);
        }
        return null;
    }

    @Override
    public Triplet post(HttpServletRequest request) {
        var url = request.getRequestURL();
        var urlHashCode = getHashCode(url).substring(0, 5);

        var consistentHashing = shardingConfig.consistentHashing();
        var server = consistentHashing.getServer(urlHashCode);

        var dataSource = getDataSourceFor(server);
        var connection = createOrGetConnection(dataSource);
        insertIntoDatabase(connection, url, urlHashCode);
        return buildResponse(urlHashCode, url.toString()).add(server);

    }

    private Pair buildResponse(String urlId, String url) {
        return Pair.with(urlId, url);
    }

    private static void insertIntoDatabase(Connection connection, StringBuffer url, String urlId) {
        try {
            var query = String.format("INSERT INTO URL_TABLE(URL,URL_ID) VALUES (?,?)");
            var prepareStatement = connection.prepareStatement(query);

            prepareStatement.setString(1, url.toString());
            prepareStatement.setString(2, urlId);

            var result = prepareStatement.executeUpdate();
            if (result > 0) {
                LOG.info(String.format("Result of insert into database: %d", result));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static Connection createOrGetConnection(DataSource dataSource) {
        try {
            var connection = dataSource.getConnection();
            if (connection.isClosed()) {
                return dataSource.createConnectionBuilder().build();
            } else {
                return connection;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private DataSource getDataSourceFor(String server) {
        LOG.info(String.format("getting DataSource for:%s", server));
        return dataSourceMap().get(server);
    }

    private static String getHashCode(StringBuffer url) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA3-256");
            var intermediate = digest.digest(url.toString().getBytes(StandardCharsets.UTF_8));
            var urlId = new String(Base64.encode(intermediate));
            return urlId;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public String health() {
        return "Working fine";
    }
}
