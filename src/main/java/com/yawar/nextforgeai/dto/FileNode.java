package com.yawar.nextforgeai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
public class FileNode implements Serializable {
    private String path;

    @Override
    public String toString() {
        return path;
    }
}
