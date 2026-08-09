package com.yawar.nextforgeai.entity.enums;

public enum ChatEventType {
    THOUGHT, // "Thought for 2s"
    MESSAGE,// Standard conversational text
    FILE_EDIT, // Code Generation <FIle>
    TOOL_LOG // "Reading file...." <tool>
}
