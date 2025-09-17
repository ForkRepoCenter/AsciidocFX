package com.kodedu.other;

import org.eclipse.jgit.revwalk.RevCommit;

import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class RevContent {
    private final RevCommit commit;
    private LocalDateTime commitDateTime;
    private Map<String, String> revMap = new ConcurrentHashMap<>();
    private Map<String, String> inheritMap = new ConcurrentHashMap<>();

    public RevContent(RevCommit commit) {
        this.commit = commit;
    }

    public String getContent() {
        List<String> values = new ArrayList<>(revMap.values());
        if (!values.isEmpty() && values.size() == 1) {
            return values.getFirst();
        }
        return "";
    }

    public String getCommitId() {
        return commit.getName();
    }

    public String getCommitMessage() {
        return commit.getFullMessage();
    }

    public LocalDateTime getCommitDateTime() {
        if (Objects.nonNull(commitDateTime)) {
            return commitDateTime;
        }
        Instant instant = Instant.ofEpochSecond(commit.getCommitTime());
        LocalDateTime commitDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        this.commitDateTime = commitDateTime;
        return commitDateTime;
    }

    public RevCommit getCommit() {
        return commit;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (RevContent) obj;
        return
                Objects.equals(this.getCommitId(), that.getCommitId()) &&
                        Objects.equals(this.getCommitMessage(), that.getCommitMessage()) &&
                        Objects.equals(this.getCommitDateTime(), that.getCommitDateTime()) &&
                        Objects.equals(this.commit, that.commit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCommitId(), getCommitMessage(), getCommitDateTime(), commit);
    }

    @Override
    public String toString() {
        return "RevContent[" +
                "commitDateTime=" + commitDateTime + ", " +
                "commit=" + commit + ']';
    }


    public void addRevPath(String newPath, String content) {
        revMap.putIfAbsent(newPath, content);
    }

    public void addInheritedPath(String newPath, String content) {
//        revMap.putIfAbsent(newPath, content);
        inheritMap.putIfAbsent(newPath, content);
    }

    public List<String> getRevPaths() {
        return revMap.keySet().stream().toList();
    }

    public List<String> getAllRevPaths() {
        Set<String> all = new LinkedHashSet<>();
        all.addAll(revMap.keySet());
        all.addAll(inheritMap.keySet());
        return new ArrayList<>(all);
    }

    public String getRevPath() {
        List<String> values = new ArrayList<>(revMap.keySet());
        if (!values.isEmpty() && values.size() == 1) {
            return Paths.get(values.getFirst()).getFileName().toString();
        }
        return "<Empty>";
    }

    public String getSinglePath() {
        List<String> values = new ArrayList<>(revMap.keySet());
        if (!values.isEmpty() && values.size() == 1) {
            return values.getFirst();
        }
        values = new ArrayList<>(inheritMap.keySet());
        if (!values.isEmpty() && values.size() == 1) {
            return values.getFirst();
        }
        return null;
    }

    public String getContent(String path) {
        return revMap.getOrDefault(path, inheritMap.get(path));
    }
}