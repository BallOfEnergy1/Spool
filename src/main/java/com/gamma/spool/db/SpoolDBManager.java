package com.gamma.spool.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.launchwrapper.Launch;

import com.gamma.spool.config.DebugConfig;
import com.gamma.spool.core.SpoolLogger;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;

public class SpoolDBManager {

    private static File SDBFile = null;

    public static boolean isRunning;

    private static Connection SDBConnection;

    public static void main(String[] args) {
        SDBClientSocket.start();
    }

    public static void init() {
        if (!DebugConfig.fullCompatLogging) return;

        isRunning = true;

        SDBFile = new File(Launch.minecraftHome, "spool/debug.sdb");
        SDBFile.getParentFile()
            .mkdirs();
        if (SDBFile.exists()) SDBFile.delete();

        String url = "jdbc:sqlite:" + SDBFile.getAbsolutePath();
        try {
            SDBConnection = DriverManager.getConnection(url);
        } catch (SQLException e) {
            SpoolLogger.error("Failed to connect to SDB system, extended logging is unavailable.");
            SDBConnection = null;
            return;
        }

        try {
            String sql = """
                CREATE TABLE debug (\s
                  id INT PRIMARY KEY ,\s
                  time BIGINT ,\s
                  type VARCHAR NOT NULL ,\s
                  cause VARCHAR ,\s
                  description VARCHAR NOT NULL\s
                );""";

            Statement stmt = SDBConnection.createStatement();
            stmt.execute(sql);
        } catch (SQLException e) {
            SpoolLogger.error("Failed to initialize SDB system, extended logging is unavailable.");
            try {
                SDBConnection.close();
            } catch (SQLException f) {
                throw new RuntimeException(e);
            }
        }

        allowConnections();
    }

    public static void teardown() {
        isRunning = false;
        try {
            SDBConnection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void allowConnections() {
        if (DebugConfig.allowSDBConnections) SDBSocket.start();
    }

    public static void stopConnections() {
        SDBSocket.stop();
    }

    private static final AtomicInteger id = new AtomicInteger(1);

    public static void log(String type, String cause, String desc) {
        String sql = """
                INSERT INTO debug (id, time, type, cause, description) VALUES (?, ?, ?, ?, ?)
            """;
        try {
            PreparedStatement statement = SDBConnection.prepareStatement(sql);
            statement.setInt(1, id.getAndIncrement());
            statement.setLong(2, System.currentTimeMillis());
            statement.setString(3, type);
            statement.setString(4, cause);
            statement.setString(5, desc);
            statement.execute();
        } catch (SQLException e) {
            SpoolLogger.warn("Failed to log item into SDB system", e);
        }
    }

    public static ObjectList<String> runQuery(String query) {
        try {
            PreparedStatement statement = SDBConnection.prepareStatement(query);
            ResultSet resultSet = statement.executeQuery();
            ObjectList<String> list = new ObjectArrayList<>();

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                long milliTime = resultSet.getLong("time");
                String type = resultSet.getString("type");
                String cause = resultSet.getString("cause");
                String description = resultSet.getString("description");
                String builder = id + ": ["
                    + Instant.ofEpochMilli(milliTime)
                        .toString()
                    + "] Type: "
                    + type
                    + ", Cause: "
                    + cause
                    + ", Description: "
                    + description;
                list.add(builder);
            }

            if (list.isEmpty()) list.add("No results for the given query.");

            return list;
        } catch (SQLException e) {
            return ObjectList.of("Caught exception: ", e.toString());
        }
    }
}
