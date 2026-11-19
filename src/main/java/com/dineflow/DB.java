
package com.dineflow;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DB {

    // Database connection details
    private static final String URL =
            "jdbc:mysql://localhost:3306/dineflow_db";

    private static final String USER =
            "root";

    private static final String PASSWORD =
            "Mahathiammu3470!";

    // Establish database connection
    public static Connection getConnection() {

        Connection connection = null;

        try {
            connection = DriverManager.getConnection(
                    URL, USER, PASSWORD
            );

            System.out.println("Database connected successfully!");

        } catch (SQLException e) {

            System.out.println("Database connection failed!");
            e.printStackTrace();

        }

        return connection;
    }

}
