package rubidium.permissions;

import java.util.*;

/**
 * Permission track for promotions/demotions.
 */
public class Track {
    
    private final String id;
    private final String displayName;
    private final List<String> groups;
    
    public Track(String id, String displayName, List<String> groups) {
        this.id = id;
        this.displayName = displayName;
        this.groups = new ArrayList<>(groups);
    }
    
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public List<String> getGroups() { return groups; }
    
    public String getCurrentGroup(Set<String> playerGroups) {
        for (int i = groups.size() - 1; i >= 0; i--) {
            if (playerGroups.contains(groups.get(i))) {
                return groups.get(i);
            }
        }
        return null;
    }
    
    public String getNextGroup(String currentGroup) {
        if (currentGroup == null) {
            return groups.isEmpty() ? null : groups.get(0);
        }
        
        int index = groups.indexOf(currentGroup);
        if (index >= 0 && index < groups.size() - 1) {
            return groups.get(index + 1);
        }
        
        return null;
    }
    
    public String getPreviousGroup(String currentGroup) {
        if (currentGroup == null) return null;
        
        int index = groups.indexOf(currentGroup);
        if (index > 0) {
            return groups.get(index - 1);
        }
        
        return null;
    }
    
    public int getPosition(String groupId) {
        return groups.indexOf(groupId);
    }
}
