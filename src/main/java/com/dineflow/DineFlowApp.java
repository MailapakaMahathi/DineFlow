package com.dineflow;

import java.net.URL;

import netscape.javascript.JSObject;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.stage.Stage;
import javafx.concurrent.Worker;

public class DineFlowApp extends Application {

    // Keep a strong reference to the Java bridge.
    private DashboardBridge dashboardBridge;

    @Override
    public void start(Stage primaryStage) {

        try {

            WebView webView = new WebView();
            WebEngine engine = webView.getEngine();

            engine.setJavaScriptEnabled(true);

            URL htmlURL = getClass().getResource(
                    "/frontend/pages/index.html"
            );

            if (htmlURL == null) {
                throw new IllegalStateException(
                        "Frontend index.html not found!"
                );
            }

            // Create and retain the bridge at application level.
            dashboardBridge = new DashboardBridge();

            engine.getLoadWorker().stateProperty().addListener(
                    (observable, oldState, newState) -> {

                if (newState == Worker.State.SUCCEEDED) {

                    try {

                        JSObject window = (JSObject)
                                engine.executeScript("window");

                        // Connect the retained Java bridge.
                        window.setMember("dineFlow", dashboardBridge);

                        System.out.println(
                                "JavaScript bridge connected successfully!"
                        );

                        // Test Java bridge method.
                        System.out.println(
                                "JAVA TEST: " +
                                dashboardBridge.testMenuManagementBridge()
                        );

                        // Test JSON retrieval directly from Java.
                        String menuJson =
                                dashboardBridge.getAllMenuItemsForManagementJson();

                        System.out.println(
                                "JAVA MENU JSON: " + menuJson
                        );

                        // Test JavaScript access to the bridge.
                        Object bridgeType = engine.executeScript(
                                "typeof window.dineFlow"
                        );

                        Object methodType = engine.executeScript(
                                "typeof window.dineFlow.getMenuItemsJson"
                        );

                        System.out.println(
                                "JAVASCRIPT BRIDGE TYPE: " + bridgeType
                        );

                        System.out.println(
                                "JAVASCRIPT METHOD TYPE: " + methodType
                        );

                        // Test the actual JavaScript return value.
                        Object jsResult = engine.executeScript(
                                "window.dineFlow.getMenuItemsJson()"
                        );

                        System.out.println(
                                "JAVASCRIPT RETURN TYPE: " +
                                engine.executeScript(
                                        "typeof window.dineFlow.getMenuItemsJson()"
                                )
                        );

                        System.out.println(
                                "JAVASCRIPT RETURN VALUE: " + jsResult
                        );

                        // Load initial application data.
                        engine.executeScript(
                                "if (typeof loadManagementMenuItems === 'function') {" +
                                "    loadManagementMenuItems();" +
                                "}" +
                                "if (typeof loadDashboardStats === 'function') {" +
                                "    loadDashboardStats();" +
                                "}" +
                                "if (typeof loadRecentOrders === 'function') {" +
                                "    loadRecentOrders();" +
                                "}" +
                                "if (typeof loadMenuItems === 'function') {" +
                                "    loadMenuItems();" +
                                "}"
                        );

                    } catch (Exception e) {

                        System.err.println(
                                "JavaScript bridge initialization failed!"
                        );

                        e.printStackTrace();
                    }
                }
            });

            engine.load(htmlURL.toExternalForm());

            StackPane root = new StackPane(webView);

            Scene scene = new Scene(root, 1350, 850);

            primaryStage.setTitle(
                    "DineFlow | Restaurant Management System"
            );

            primaryStage.setScene(scene);
            primaryStage.setWidth(1350);
            primaryStage.setHeight(850);

            primaryStage.setMinWidth(1050);
            primaryStage.setMinHeight(700);

            primaryStage.centerOnScreen();
            primaryStage.show();

        } catch (Exception e) {

            System.out.println("Unable to launch DineFlow!");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}