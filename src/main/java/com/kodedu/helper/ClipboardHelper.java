package com.kodedu.helper;

import com.kodedu.component.EditorPane;
import com.kodedu.service.ParserService;
import com.kodedu.service.impl.ThreadServiceImpl;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ClipboardHelper {

    private Logger logger = LoggerFactory.getLogger(ClipboardHelper.class);

    @Autowired
    private ParserService parserService;

    @Autowired
    private ThreadServiceImpl threadService;

    public void cutCopy(String data) {
        threadService.runActionLater(() -> {
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.putString(data.replaceAll("\\R", "\n"));
            Clipboard.getSystemClipboard().setContent(clipboardContent);
        });
    }

    public void pasteRaw(EditorPane editorPane) {
        threadService.runActionLater(() -> {
            Clipboard systemClipboard = Clipboard.getSystemClipboard();
            boolean matched = insertImageBlock(systemClipboard, editorPane);
            if (matched) {
                return;
            }
            editorPane.execCommand("paste", clipboardValue());
        });
    }

    public boolean insertImageBlock(Clipboard systemClipboard, EditorPane editorPane) {
        if (systemClipboard.hasFiles()) {
            Optional<String> block = parserService.toImageBlock(systemClipboard.getFiles());
            if (block.isPresent()) {
                editorPane.insert(block.get());
                return true;
            }
        }

        if (systemClipboard.hasImage()) {
            Image image = systemClipboard.getImage();
            Optional<String> block = parserService.toImageBlock(image);
            if (block.isPresent()) {
                editorPane.insert(block.get());
                return true;
            }
        }

        return false;
    }

    public String clipboardValue() {
        return Clipboard.getSystemClipboard().getString();
    }

    public void paste(EditorPane editorPane) {

        threadService.runActionLater(() -> {
            Clipboard systemClipboard = Clipboard.getSystemClipboard();
            boolean matched = insertImageBlock(systemClipboard, editorPane);
            if (matched) {
                return;
            }

            try {
                if (systemClipboard.hasHtml()) {
                    String content = Optional.ofNullable(systemClipboard.getHtml()).orElse(systemClipboard.getString());
                    editorPane.insert(content);
                    return;
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            editorPane.execCommand("paste", clipboardValue());
        });

    }

    public void copyFiles(List<Path> paths) {
        threadService.runActionLater(() -> {
            Optional.ofNullable(paths)
                    .filter((ps) -> !ps.isEmpty())
                    .ifPresent(ps -> {
                        ClipboardContent clipboardContent = new ClipboardContent();
                        clipboardContent.putFiles(ps
                                .stream()
                                .map(Path::toFile)
                                .collect(Collectors.toList()));
                        Clipboard.getSystemClipboard().setContent(clipboardContent);
                    });
        });
    }
}
