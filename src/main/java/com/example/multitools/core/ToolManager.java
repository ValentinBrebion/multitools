package com.example.multitools.core;

import java.util.ArrayList;
import java.util.List;

public class ToolManager {
    
    private static ToolManager instance;
    private List<Tool> tools;
    
    private ToolManager() {
        tools = new ArrayList<>();
        initializeDefaultTools();
    }
    
    public static ToolManager getInstance() {
        if (instance == null) {
            instance = new ToolManager();
        }
        return instance;
    }
    
    private void initializeDefaultTools() {
        // Ajouter l'outil de compression par défaut
        tools.add(new com.example.multitools.tools.compression.CompressionTool());
        
        // Ajouter les outils placeholder pour le futur
        tools.add(new com.example.multitools.tools.text.TextTool());
        tools.add(new com.example.multitools.tools.image.ImageTool());
    }
    
    public List<Tool> getTools() {
        return new ArrayList<>(tools);
    }
    
    public void addTool(Tool tool) {
        tools.add(tool);
    }
    
    public void removeTool(Tool tool) {
        tools.remove(tool);
    }
    
    public Tool getToolByName(String name) {
        for (Tool tool : tools) {
            if (tool.getName().equals(name)) {
                return tool;
            }
        }
        return null;
    }
}
