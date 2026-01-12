package com.yellowtale.rubidium.integration.modtale;

import java.lang.reflect.*;
import java.util.List;

public class GsonJsonParser implements ModtaleClient.JsonParser {
    
    private final Object gson;
    private final Method fromJsonMethod;
    private final Method fromJsonTypeMethod;
    private final Type stringListType;
    
    public GsonJsonParser() throws Exception {
        Class<?> gsonClass = Class.forName("com.google.gson.Gson");
        Class<?> gsonBuilderClass = Class.forName("com.google.gson.GsonBuilder");
        
        Object builder = gsonBuilderClass.getConstructor().newInstance();
        this.gson = gsonBuilderClass.getMethod("create").invoke(builder);
        
        this.fromJsonMethod = gsonClass.getMethod("fromJson", String.class, Class.class);
        this.fromJsonTypeMethod = gsonClass.getMethod("fromJson", String.class, Type.class);
        
        this.stringListType = new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[]{String.class};
            }
            
            @Override
            public Type getRawType() {
                return List.class;
            }
            
            @Override
            public Type getOwnerType() {
                return null;
            }
        };
    }
    
    public static GsonJsonParser createIfAvailable() {
        try {
            return new GsonJsonParser();
        } catch (Exception e) {
            return null;
        }
    }
    
    public static boolean isGsonAvailable() {
        try {
            Class.forName("com.google.gson.Gson");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    @Override
    public ModtaleClient.SearchResult parseSearchResult(String json) {
        try {
            return (ModtaleClient.SearchResult) fromJsonMethod.invoke(gson, json, ModtaleClient.SearchResult.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse search result with Gson", e);
        }
    }
    
    @Override
    public ModtaleClient.ProjectDetails parseProjectDetails(String json) {
        try {
            return (ModtaleClient.ProjectDetails) fromJsonMethod.invoke(gson, json, ModtaleClient.ProjectDetails.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse project details with Gson", e);
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public List<String> parseStringArray(String json) {
        try {
            return (List<String>) fromJsonTypeMethod.invoke(gson, json, stringListType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse string array with Gson", e);
        }
    }
}
