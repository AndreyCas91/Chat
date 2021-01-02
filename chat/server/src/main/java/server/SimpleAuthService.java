package server;

import java.sql.*;

public class SimpleAuthService implements AuthService{
    private static Connection connection;
    private static Statement stmt;
    private static PreparedStatement psInsert;

    public SimpleAuthService(){
        try {
            connect();
            stmt.executeUpdate("INSERT INTO users (login, password, nickname) VALUES ('qwe', 'qwe', 'qwe'), " +
                    "('asd', 'asd', 'asd'), ('zxc', 'zxc', 'zxc')");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
    }

    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {
        try {
            ResultSet rs = stmt.executeQuery("SELECT * FROM users");
            while (rs.next()){
                if(rs.getString(2).equals(login) && rs.getString(3).equals(password)){
                    return rs.getString(4);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean registration(String login, String password, String nickname) {
        try {
            ResultSet rs = stmt.executeQuery("SELECT * FROM users");
            while (rs.next()){
                if(rs.getString(2).equals(login) || rs.getString(4).equals(nickname)){
                    return false;
                }
            }

            psInsert = connection.prepareStatement("INSERT INTO users (login, password, nickname) VALUES (?, ?, ?)");
            psInsert.setString(1, login);
            psInsert.setString(2, password);
            psInsert.setString(3, nickname);
            psInsert.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public static void connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:main.db");
        stmt = connection.createStatement();
    }

    @Override
    public void disconnect(){
        try {
            stmt.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
}
