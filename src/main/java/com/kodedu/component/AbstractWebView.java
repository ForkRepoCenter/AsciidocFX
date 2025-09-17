package com.kodedu.component;

import com.kodedu.controller.ApplicationController;
import com.kodedu.helper.FxHelper;
import com.kodedu.service.ThreadService;
import jakarta.annotation.PostConstruct;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractWebView extends AnchorPane implements ExecShortcuts {

    @Autowired
    protected ThreadService threadService;

    @Autowired
    protected ApplicationController controller;

    private WebView webView;
    private Stage stage;

    @PostConstruct
    public void init() {
        webView = threadService.supply(WebView::new);
        stage = threadService.supply(() -> {
            Stage stage = new Stage();
            stage.setScene(new Scene(this, 1200, 800));
            return stage;
        });
        enableLoadWorker();
        threadService.runTaskLater(() -> {
            String viewUrl = getViewUrl();
            threadService.runActionLater(() -> {
                webEngine().load(viewUrl);
            }, true);
        });
    }

    private void enableLoadWorker() {
        threadService.runActionLater(() -> {
            webEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
                threadService.runActionLater(() -> {
                    getWindow().setMember("__java", this);
                    if (newValue == Worker.State.SUCCEEDED) {
                        onContentLoaded();
                    }
                });
            });
        });
    }

    public void showStage() {
        threadService.runActionLater(() -> {
            if (!stage.isShowing()) {
                stage.show();
            }
            stage.toFront();
        });
    }

    public void onContentLoaded() {
        this.getChildren().add(webView);
        FxHelper.fitToParent(webView);
        webView.requestFocus();
        showStage();
    }

    protected abstract String getViewUrl();

    public WebView getWebView() {
        return webView;
    }

    public WebEngine webEngine() {
        return webView.getEngine();
    }

    public JSObject getWindow() {
        return (JSObject) webEngine().executeScript("window");
    }
}
