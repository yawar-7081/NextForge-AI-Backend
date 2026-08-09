package com.yawar.nextforgeai.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Builder
public class ProjectFileContentResponse implements Serializable {
    private String path;
    private String content;
}
