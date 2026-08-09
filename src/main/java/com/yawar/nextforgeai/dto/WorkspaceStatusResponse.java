package com.yawar.nextforgeai.dto;


import lombok.Builder;

@Builder
public record WorkspaceStatusResponse(

        boolean initialized,

        boolean hasFileExplorer,

        boolean hasPreview,

        boolean canDownload

) {
}