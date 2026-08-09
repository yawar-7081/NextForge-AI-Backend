package com.yawar.nextforgeai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class ProjectFileResponse implements Serializable {
    List<FileNode> paths;
}
