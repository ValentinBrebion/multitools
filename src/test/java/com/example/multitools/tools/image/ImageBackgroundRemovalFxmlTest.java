package com.example.multitools.tools.image;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ImageBackgroundRemovalFxmlTest {

    @BeforeClass
    public static void startJavaFx() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
        } catch (IllegalStateException alreadyStarted) {
            started.countDown();
        }
        assertTrue("JavaFX ne s'est pas initialisé.", started.await(10, TimeUnit.SECONDS));
    }

    @AfterClass
    public static void stopJavaFx() {
        Platform.exit();
    }

    @Test
    public void loadsBackgroundRemovalScreen() throws Exception {
        assertFxmlLoads("/fxml/image/image-background-removal.fxml");
    }

    @Test
    public void loadsImageColorExtractionScreen() throws Exception {
        assertFxmlLoads("/fxml/image/image-color-extraction.fxml");
    }

    private void assertFxmlLoads(String resource) throws Exception {
        FutureTask<Parent> loadScreen = new FutureTask<>(
            () -> new FXMLLoader(getClass().getResource(resource)).load()
        );
        Platform.runLater(loadScreen);

        Parent root = loadScreen.get(10, TimeUnit.SECONDS);

        assertNotNull(root);
    }
}
