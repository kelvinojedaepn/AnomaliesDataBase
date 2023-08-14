package Negocio;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.JOptionPane;

public class Principal extends Conexion {

    ArrayList<String> tablas = new ArrayList<>();

    Map<String, List<String>> relationships = new HashMap<>();
    FileWriter fw;

    public void obtenerTablas(String BD) throws SQLException {
        connection = getConnection(BD);
        ps = connection.prepareStatement("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES");
        rs = ps.executeQuery();
        while (rs.next()) {
            tablas.add(rs.getString("TABLE_NAME"));
        }
        ps.close();
        rs.close();
        connection.close();
    }

    public ArrayList<String> buscarRelaciones(String BD) throws SQLException {
        connection = getConnection(BD);
        ArrayList<String> relaciones = new ArrayList<>();
        ps = connection.prepareStatement("SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE CONSTRAINT_TYPE ='FOREIGN KEY'");
        rs = ps.executeQuery();
        while (rs.next()) {
            String tablePK = rs.getString("CONSTRAINT_NAME");
            relaciones.add(tablePK);
        }
        ps.close();
        rs.close();
        connection.close();
        return relaciones;
    }

    public Map posiblesRelaciones(String BD) throws SQLException {
        connection = getConnection(BD);
        ps = connection.prepareStatement("SELECT column_name , table_name FROM INFORMATION_SCHEMA.COLUMNS");
        rs = ps.executeQuery();
        Map<String, List<String>> tables_columns = new HashMap<>();
        while (rs.next()) {
            String table = rs.getString("table_name");
            String column = rs.getString("column_name");
            List<String> columns = tables_columns.getOrDefault(table, new ArrayList<>());
            columns.add(column);
            tables_columns.put(table, columns);

        }

        //Encontrar las posibles relaciones
        Map<String, List<String>> posibles = new HashMap<>();
        for (int i = 0; i < tablas.size(); i++) {
            for (int j = tablas.size() - 1; j > 0; j--) {
                if (!tablas.get(i).equals(tablas.get(j))) {
                    for (String ls : tables_columns.get(tablas.get(i))) {
                        for (String ls2 : tables_columns.get(tablas.get(j))) {
                            if (ls.equals(ls2)) {
                                String pkTable = tablas.get(j) + " - " + ls;
                                List<String> pkTables = posibles.getOrDefault(tablas.get(i), new ArrayList<>());
                                pkTables.add(pkTable);
                                posibles.put(tablas.get(i), pkTables);
                            }
                        }
                    }
                }
            }
        }
        ps.close();
        rs.close();
        connection.close();
        return posibles;
    }

    public void CrearLog(String operacion, String contenido, String BD) {
        SimpleDateFormat formato = new SimpleDateFormat("HH.mm.ss dd-MM-yyyy", Locale.getDefault());
        Date Ahora = new Date();
        String name = formato.format(Ahora);
        String archivo = "D:\\" + BD + operacion + "-logs_" + name + ".txt";
        File fichero = new File(archivo);
        try {
            if (fichero.createNewFile()) {
                fw = new FileWriter(fichero);
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(contenido);
                bw.close();
                JOptionPane.showMessageDialog(null, "Se creo el log: " + archivo);
            } else {
                JOptionPane.showMessageDialog(null, "No se ha podido crear el log: " + archivo);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public String CrearTablaAuditoria(String BD) throws SQLException {
        connection = getConnection(BD);
        String tablaAuditoria = "create table tabla_auditoria(\n"
                + "    Usuario    varchar(255) not null,\n"
                + "    Operacion  varchar(255) not null,\n"
                + "    Tabla      varchar(255) null,\n"
                + "    fecha_hora DATETIME null\n"
                + // Eliminado la coma adicional al final
                ")";  // Corregir: Eliminado la coma adicional al final

        ps = connection.prepareStatement(tablaAuditoria);
        ps.executeUpdate(); // Corregir: Cambiado de execute() a executeUpdate()
        ps.close();
        connection.close();
        return tablaAuditoria;
    }

    public String CrearDisparadores(String BD) throws SQLException {
        connection = getConnection(BD);
        String item = "";
        ArrayList<String> lista_disparadores_Auditoria = new ArrayList<>();
        for (int i = 0; i < this.tablas.size(); i++) {
            item = "CREATE TRIGGER auditoria_" + this.tablas.get(i) + "\n"
                    + "ON " + this.tablas.get(i) + "\n"
                    + "AFTER INSERT, UPDATE, DELETE\n"
                    + "AS\n"
                    + "BEGIN\n"
                    + // ... (resto del código)
                    "END;\n";
            lista_disparadores_Auditoria.add(item);
        }
        lista_disparadores_Auditoria.remove(lista_disparadores_Auditoria.size() - 1);
        String listaTriggers = "";
        for (int i = 0; i < lista_disparadores_Auditoria.size(); i++) {
            ps = connection.prepareStatement(lista_disparadores_Auditoria.get(i));
            ps.executeUpdate(); // Corregir: Cambiado de execute() a executeUpdate()
            ps.close();
            listaTriggers += lista_disparadores_Auditoria.get(i);
        }
        connection.close();
        return listaTriggers;
    }

    public Map obtenerTrigger(String BD) throws SQLException {
        connection = getConnection(BD);
        ps = connection.prepareStatement("SELECT name AS TriggerName, OBJECT_NAME(parent_id) AS TableName\n"
                + "FROM sys.triggers\n"
                + "ORDER BY TableName, TriggerName;");
        rs = ps.executeQuery();
        Map<String, List<String>> tables_columns = new HashMap<>();
        while (rs.next()) {
            String table = rs.getString("TriggerName");
            String column = rs.getString("TableName");
            List<String> columns = tables_columns.getOrDefault(table, new ArrayList<>());
            columns.add(column);
            tables_columns.put(table, columns);
        }

        return tables_columns;
    }

    public ArrayList<String> getAnormalies(String BD) throws SQLException {
        ArrayList<String> tabla = new ArrayList<>();
        connection = getConnection(BD);
        String anomalias = "DBCC CHECKCONSTRAINTS WITH ALL_CONSTRAINTS";
        ps = connection.prepareStatement(anomalias);

        // Usar executeQuery() para obtener un conjunto de resultados
        rs = ps.executeQuery();

        while (rs.next()) {
            String table = rs.getString("Table");
            String constraint = rs.getString("Constraint");
            String where = rs.getString("Where");
            String anomalia = table + ": " + constraint + " - " + where;
            tabla.add(anomalia);
        }

        // Cerrar recursos
        rs.close();
        ps.close();
        connection.close();
        return tabla;
    }
}
