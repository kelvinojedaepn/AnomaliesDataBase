package Negocio;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionBaseDatos {
    private static final String SERVER = "jdbc:sqlserver://localhost:1433;databaseName=";
    private static final String ENCRYPT = ";encrypt=false";
    private static final String USERNAME = "sa";
    private static final String PASSWORD = "P@ssw0rd";
    private static String URL = "";

    static {
        try {
            // Carga el controlador JDBC
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection(String BD) throws SQLException {
        URL = SERVER + BD + ENCRYPT;
        Connection con = DriverManager.getConnection(URL, USERNAME, PASSWORD);
        return con;
        
    }
}