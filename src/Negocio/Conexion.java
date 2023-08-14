package Negocio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public abstract class Conexion extends ConexionBaseDatos{
    protected PreparedStatement ps;
    protected ResultSet rs;
    public Connection connection;
}
