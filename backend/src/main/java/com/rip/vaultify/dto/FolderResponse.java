package com.rip.vaultify.dto;

import com.rip.vaultify.model.Folder;
import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

public class FolderResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private ParentInfo parent;
    private List<ChildInfo> children;

    public FolderResponse(Folder folder) {
        this.id = folder.getId();
        this.name = folder.getName();

        // parent info
        if (folder.getParent() != null) {
            this.parent = new ParentInfo(folder.getParent().getId(), folder.getParent().getName());
        }

        // children info
        if (folder.getChildren() != null && !folder.getChildren().isEmpty()) {
            this.children = folder.getChildren().stream()
                    .map(child -> new ChildInfo(child.getId(), child.getName()))
                    .collect(Collectors.toList());
        }
    }

    public static class ParentInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private String name;

        public ParentInfo(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
    }

    public static class ChildInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private String name;

        public ChildInfo(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public ParentInfo getParent() { return parent; }
    public List<ChildInfo> getChildren() { return children; }
}
