package eu.greev.dcbot.database;

import eu.greev.dcbot.Main;

import java.io.*;
import java.sql.*;
import java.util.List;
import java.util.stream.Collectors;

public class Database {

    public Database() throws IOException {
        new File("./GreevTickets").mkdirs();
        new File("./GreevTickets/JTP.db").createNewFile();
    }

    public Connection connect() {
        String url = "jdbc:sqlite:./GreevTickets/SSSIT.db";
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }

    public void createNewDatabase(String fileName) {
        String url = "jdbc:sqlite:./GreevTickets/" + fileName;

        try {
            Connection conn = DriverManager.getConnection(url);
            if (conn != null) {
                DatabaseMetaData meta = conn.getMetaData();
                System.out.println("The driver name is " + meta.getDriverName());
                System.out.println("A new database has been created.");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void createNewTable() {
        String url = "jdbc:sqlite:./GreevTickets/SSSIT.db";

        String sql = "";
        InputStream in = Main.class.getClassLoader().getResourceAsStream("dbsetup.sql");
        sql = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));


        try{
            Connection conn = DriverManager.getConnection(url);
            Statement stmt = conn.createStatement();
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}