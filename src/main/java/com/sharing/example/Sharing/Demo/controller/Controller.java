package com.sharing.example.Sharing.Demo.controller;

import com.sharing.example.Sharing.Demo.service.Service;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.javatuples.Pair;
import org.postgresql.ds.PGSimpleDataSource;
import org.postgresql.shaded.com.ongres.scram.common.bouncycastle.base64.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;

@RestController
public class Controller implements Service {
    private static final Log LOG = LogFactory.getLog(Controller.class);
    private final Environment environment;

    @Autowired
    public Controller(Environment environment) {
        this.environment = environment;
    }

    @Override
    //https://localhost:8081/fhy2h
    public Pair get(String urlId) {
        //var server=hr.get(urlId);
        var dataSource = dataSource();
        var connection = createOrGetConnection(dataSource);

        return getFromDatabase(urlId, connection);

        //var result=clients.get(server).query("Select * from URL_TABLE WHERE URL_ID=$1",urlId);
//        if(result){
//            return buildResponse(urlId,result.row[0],server);
//        }else{
//            //return 404
//        }

    }

    private static Pair getFromDatabase(String urlId, Connection connection) {
        try {
            var query = String.format("SELECT URL,URL_ID FROM URL_TABLE WHERE URL_ID=?");
            var prepareStatement = connection.prepareStatement(query);
            prepareStatement.setString(1, urlId);
            var resultSet = prepareStatement.executeQuery();
            while (resultSet.next()) {
                var url = resultSet.getString("URL");
                return Pair.with(url, urlId);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Pair.with("404", "Not Found");
    }

    @Override
    public Pair post(HttpServletRequest request) {
        var url = request.getRequestURL();
        var urlId = getUrlId(url).substring(0, 5);

        var dataSource = dataSource();
        var connection = createOrGetConnection(dataSource);
        insertIntoDatabase(connection, url, urlId);
        //var url = request.query.url;
        //www.wiki.com/sharding
        //consistently hash this to get a port

//        var hash = crypto.createHash("sha256").update(url).digest("base64");
//        var urlId = hash.substr(0, 5);
//        var server = hr.get(urlId);
//        clients[server].query("INSERT INTO URL_TABLE(URL,URL_ID) VALUES ($1,$2)", url, urlId);
//
//        return buildResponse(urlId, url, server);
        return buildResponse(urlId, url.toString());

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

    private DataSource dataSource() {
        DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.url(environment.getProperty("spring.datasource.url"));
        dataSourceBuilder.username(environment.getProperty("spring.datasource.username"));
        dataSourceBuilder.password(environment.getProperty("spring.datasource.password"));
        dataSourceBuilder.driverClassName(environment.getProperty("spring.datasource.driver"));
        dataSourceBuilder.type(PGSimpleDataSource.class);
        return dataSourceBuilder.build();
    }

    private static String getUrlId(StringBuffer url) {
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
