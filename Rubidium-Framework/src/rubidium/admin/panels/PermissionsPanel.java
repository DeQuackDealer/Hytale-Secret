package rubidium.admin.panels;

import rubidium.admin.AdminPanel;
import rubidium.api.player.Player;
import rubidium.api.server.Server;
import rubidium.api.permissions.PermissionGroup;
import rubidium.ui.RubidiumUI;
import rubidium.ui.components.*;

import java.util.*;

public class PermissionsPanel implements AdminPanel {
    
    @Override
    public String getId() {
        return "permissions";
    }
    
    @Override
    public String getName() {
        return "Permissions";
    }
    
    @Override
    public String getDescription() {
        return "Manage player permissions and groups";
    }
    
    @Override
    public String getIcon() {
        return "key";
    }
    
    @Override
    public String getPermission() {
        return "rubidium.admin.permissions";
    }
    
    @Override
    public int getPriority() {
        return 30;
    }
    
    @Override
    public UIContainer createUI(Player admin) {
        UIContainer panel = new UIContainer("permissions")
            .setTitle("Permissions Manager")
            .setSize(500, 550)
            .setBackground(0x1E1E23)
            .setCentered(true);
        
        UIText title = new UIText("title")
            .setText("Permission Management")
            .setFontSize(20)
            .setColor(0x8A2BE2)
            .setPosition(20, 20);
        panel.addChild(title);
        
        UIButton playersTab = new UIButton("players_tab")
            .setText("Players")
            .setSize(150, 35)
            .setPosition(20, 55)
            .setBackground(0x4169E1)
            .onClick(() -> showPlayerPermissions(admin));
        panel.addChild(playersTab);
        
        UIButton groupsTab = new UIButton("groups_tab")
            .setText("Groups")
            .setSize(150, 35)
            .setPosition(180, 55)
            .setBackground(0x2D2D35)
            .onClick(() -> showGroupPermissions(admin));
        panel.addChild(groupsTab);
        
        UIButton createGroupBtn = new UIButton("create_group")
            .setText("+ New Group")
            .setSize(150, 35)
            .setPosition(340, 55)
            .setBackground(0x32CD32)
            .onClick(() -> openCreateGroupDialog(admin));
        panel.addChild(createGroupBtn);
        
        UIContainer listContainer = new UIContainer("list")
            .setPosition(20, 100)
            .setSize(460, 360)
            .setScrollable(true)
            .setBackground(0x14141A);
        
        int y = 10;
        for (Player player : Server.getOnlinePlayers()) {
            UIContainer row = createPlayerPermissionRow(admin, player, y);
            listContainer.addChild(row);
            y += 55;
        }
        
        panel.addChild(listContainer);
        
        UIButton backBtn = new UIButton("back")
            .setText("Back to Menu")
            .setSize(460, 40)
            .setPosition(20, 480)
            .setBackground(0x505060)
            .onClick(() -> rubidium.admin.AdminUIModule.getInstance().openMainMenu(admin));
        panel.addChild(backBtn);
        
        return panel;
    }
    
    private UIContainer createPlayerPermissionRow(Player admin, Player target, int y) {
        UIContainer row = new UIContainer("player_" + target.getUniqueId())
            .setPosition(10, y)
            .setSize(440, 50)
            .setBackground(0x2D2D35);
        
        UIText name = new UIText("name")
            .setText(target.getName())
            .setFontSize(14)
            .setColor(0xF0F0F5)
            .setPosition(10, 8);
        row.addChild(name);
        
        String groupName = target.getPrimaryGroup() != null ? target.getPrimaryGroup().getName() : "default";
        UIText group = new UIText("group")
            .setText("Group: " + groupName)
            .setFontSize(10)
            .setColor(0x808090)
            .setPosition(10, 28);
        row.addChild(group);
        
        UIButton editBtn = new UIButton("edit")
            .setText("Edit")
            .setSize(60, 30)
            .setPosition(300, 10)
            .setBackground(0x4169E1)
            .onClick(() -> openPlayerPermissionEditor(admin, target));
        row.addChild(editBtn);
        
        UIButton opBtn = new UIButton("op")
            .setText(target.isOp() ? "DeOP" : "OP")
            .setSize(60, 30)
            .setPosition(370, 10)
            .setBackground(target.isOp() ? 0x8B0000 : 0x32CD32)
            .onClick(() -> {
                target.setOp(!target.isOp());
                admin.sendMessage("&a" + (target.isOp() ? "Opped" : "De-opped") + " " + target.getName());
                RubidiumUI.showUI(admin, createUI(admin));
            });
        row.addChild(opBtn);
        
        return row;
    }
    
    private void showPlayerPermissions(Player admin) {
        RubidiumUI.showUI(admin, createUI(admin));
    }
    
    private void showGroupPermissions(Player admin) {
        UIContainer panel = new UIContainer("group_permissions")
            .setTitle("Permission Groups")
            .setSize(500, 550)
            .setBackground(0x1E1E23)
            .setCentered(true);
        
        UIText title = new UIText("title")
            .setText("Permission Groups")
            .setFontSize(20)
            .setColor(0x8A2BE2)
            .setPosition(20, 20);
        panel.addChild(title);
        
        UIButton playersTab = new UIButton("players_tab")
            .setText("Players")
            .setSize(150, 35)
            .setPosition(20, 55)
            .setBackground(0x2D2D35)
            .onClick(() -> showPlayerPermissions(admin));
        panel.addChild(playersTab);
        
        UIButton groupsTab = new UIButton("groups_tab")
            .setText("Groups")
            .setSize(150, 35)
            .setPosition(180, 55)
            .setBackground(0x4169E1)
            .onClick(() -> {});
        panel.addChild(groupsTab);
        
        UIButton createGroupBtn = new UIButton("create_group")
            .setText("+ New Group")
            .setSize(150, 35)
            .setPosition(340, 55)
            .setBackground(0x32CD32)
            .onClick(() -> openCreateGroupDialog(admin));
        panel.addChild(createGroupBtn);
        
        UIContainer listContainer = new UIContainer("list")
            .setPosition(20, 100)
            .setSize(460, 360)
            .setScrollable(true)
            .setBackground(0x14141A);
        
        Collection<PermissionGroup> groups = Server.getPermissionGroups();
        int y = 10;
        
        for (PermissionGroup group : groups) {
            UIContainer row = createGroupRow(admin, group, y);
            listContainer.addChild(row);
            y += 55;
        }
        
        panel.addChild(listContainer);
        
        UIButton backBtn = new UIButton("back")
            .setText("Back to Menu")
            .setSize(460, 40)
            .setPosition(20, 480)
            .setBackground(0x505060)
            .onClick(() -> rubidium.admin.AdminUIModule.getInstance().openMainMenu(admin));
        panel.addChild(backBtn);
        
        RubidiumUI.showUI(admin, panel);
    }
    
    private UIContainer createGroupRow(Player admin, PermissionGroup group, int y) {
        UIContainer row = new UIContainer("group_" + group.getName())
            .setPosition(10, y)
            .setSize(440, 50)
            .setBackground(0x2D2D35);
        
        UIText name = new UIText("name")
            .setText(group.getName())
            .setFontSize(14)
            .setColor(group.getColor())
            .setPosition(10, 8);
        row.addChild(name);
        
        UIText info = new UIText("info")
            .setText("Permissions: " + group.getPermissions().size() + " | Priority: " + group.getPriority())
            .setFontSize(10)
            .setColor(0x808090)
            .setPosition(10, 28);
        row.addChild(info);
        
        UIButton editBtn = new UIButton("edit")
            .setText("Edit")
            .setSize(60, 30)
            .setPosition(300, 10)
            .setBackground(0x4169E1)
            .onClick(() -> openGroupEditor(admin, group));
        row.addChild(editBtn);
        
        if (!group.getName().equals("default")) {
            UIButton deleteBtn = new UIButton("delete")
                .setText("Delete")
                .setSize(60, 30)
                .setPosition(370, 10)
                .setBackground(0x8B0000)
                .onClick(() -> openDeleteGroupDialog(admin, group));
            row.addChild(deleteBtn);
        }
        
        return row;
    }
    
    private void openPlayerPermissionEditor(Player admin, Player target) {
        UIContainer editor = new UIContainer("perm_editor")
            .setTitle("Edit Permissions: " + target.getName())
            .setSize(450, 500)
            .setBackground(0x1E1E23)
            .setCentered(true);
        
        UIText titleText = new UIText("title")
            .setText("Permissions for " + target.getName())
            .setFontSize(18)
            .setColor(0x8A2BE2)
            .setPosition(20, 20);
        editor.addChild(titleText);
        
        UIText groupLabel = new UIText("group_label")
            .setText("Primary Group:")
            .setFontSize(12)
            .setColor(0xA0A0AA)
            .setPosition(20, 55);
        editor.addChild(groupLabel);
        
        UIDropdown groupDropdown = new UIDropdown("group")
            .setSize(200, 35)
            .setPosition(20, 75);
        
        for (PermissionGroup group : Server.getPermissionGroups()) {
            groupDropdown.addOption(group.getName(), group.getName());
        }
        
        if (target.getPrimaryGroup() != null) {
            groupDropdown.setSelected(target.getPrimaryGroup().getName());
        }
        
        groupDropdown.onChange(value -> {
            PermissionGroup group = Server.getPermissionGroup(value);
            if (group != null) {
                target.setPrimaryGroup(group);
                admin.sendMessage("&aSet " + target.getName() + "'s group to " + value);
            }
        });
        editor.addChild(groupDropdown);
        
        UIText addPermLabel = new UIText("add_perm_label")
            .setText("Add Permission:")
            .setFontSize(12)
            .setColor(0xA0A0AA)
            .setPosition(20, 125);
        editor.addChild(addPermLabel);
        
        UITextField permField = new UITextField("perm_input")
            .setPlaceholder("e.g. rubidium.admin.players")
            .setSize(300, 35)
            .setPosition(20, 145);
        editor.addChild(permField);
        
        UIButton addBtn = new UIButton("add_perm")
            .setText("+")
            .setSize(40, 35)
            .setPosition(330, 145)
            .setBackground(0x32CD32)
            .onClick(() -> {
                String perm = permField.getValue();
                if (!perm.isEmpty()) {
                    target.addPermission(perm);
                    admin.sendMessage("&aAdded permission: " + perm);
                    RubidiumUI.showUI(admin, openPlayerPermissionEditor_internal(admin, target));
                }
            });
        editor.addChild(addBtn);
        
        UIContainer permList = new UIContainer("perm_list")
            .setPosition(20, 195)
            .setSize(410, 220)
            .setScrollable(true)
            .setBackground(0x14141A);
        
        Set<String> permissions = target.getPermissions();
        int y = 5;
        for (String perm : permissions) {
            UIContainer permRow = new UIContainer("perm_" + perm.hashCode())
                .setPosition(5, y)
                .setSize(395, 30)
                .setBackground(0x2D2D35);
            
            UIText permText = new UIText("text")
                .setText(perm)
                .setFontSize(11)
                .setColor(0xF0F0F5)
                .setPosition(10, 8);
            permRow.addChild(permText);
            
            UIButton removeBtn = new UIButton("remove")
                .setText("-")
                .setSize(25, 20)
                .setPosition(365, 5)
                .setBackground(0x8B0000)
                .onClick(() -> {
                    target.removePermission(perm);
                    admin.sendMessage("&aRemoved permission: " + perm);
                    RubidiumUI.showUI(admin, openPlayerPermissionEditor_internal(admin, target));
                });
            permRow.addChild(removeBtn);
            
            permList.addChild(permRow);
            y += 35;
        }
        
        editor.addChild(permList);
        
        UIButton backBtn = new UIButton("back")
            .setText("Back")
            .setSize(410, 40)
            .setPosition(20, 430)
            .setBackground(0x505060)
            .onClick(() -> RubidiumUI.showUI(admin, createUI(admin)));
        editor.addChild(backBtn);
        
        RubidiumUI.showUI(admin, editor);
    }
    
    private UIContainer openPlayerPermissionEditor_internal(Player admin, Player target) {
        return null;
    }
    
    private void openGroupEditor(Player admin, PermissionGroup group) {
        admin.sendMessage("&eGroup editor for: " + group.getName());
    }
    
    private void openCreateGroupDialog(Player admin) {
        UIContainer dialog = new UIContainer("create_group")
            .setTitle("Create Permission Group")
            .setSize(400, 250)
            .setBackground(0x1E1E23)
            .setCentered(true);
        
        UIText title = new UIText("title")
            .setText("Create New Group")
            .setFontSize(16)
            .setColor(0xF0F0F5)
            .setPosition(20, 25);
        dialog.addChild(title);
        
        UIText nameLabel = new UIText("name_label")
            .setText("Group Name:")
            .setFontSize(12)
            .setColor(0xA0A0AA)
            .setPosition(20, 60);
        dialog.addChild(nameLabel);
        
        UITextField nameField = new UITextField("name")
            .setPlaceholder("e.g. moderator")
            .setSize(360, 35)
            .setPosition(20, 80);
        dialog.addChild(nameField);
        
        UIText priorityLabel = new UIText("priority_label")
            .setText("Priority (higher = more important):")
            .setFontSize(12)
            .setColor(0xA0A0AA)
            .setPosition(20, 125);
        dialog.addChild(priorityLabel);
        
        UITextField priorityField = new UITextField("priority")
            .setPlaceholder("100")
            .setSize(100, 35)
            .setPosition(20, 145);
        dialog.addChild(priorityField);
        
        UIButton createBtn = new UIButton("create")
            .setText("Create")
            .setSize(170, 40)
            .setPosition(20, 195)
            .setBackground(0x32CD32)
            .onClick(() -> {
                String name = nameField.getValue();
                if (!name.isEmpty()) {
                    int priority = 100;
                    try {
                        priority = Integer.parseInt(priorityField.getValue());
                    } catch (NumberFormatException ignored) {}
                    
                    Server.createPermissionGroup(name, priority);
                    admin.sendMessage("&aCreated group: " + name);
                    showGroupPermissions(admin);
                }
            });
        dialog.addChild(createBtn);
        
        UIButton cancelBtn = new UIButton("cancel")
            .setText("Cancel")
            .setSize(170, 40)
            .setPosition(210, 195)
            .setBackground(0x505060)
            .onClick(() -> RubidiumUI.showUI(admin, createUI(admin)));
        dialog.addChild(cancelBtn);
        
        RubidiumUI.showUI(admin, dialog);
    }
    
    private void openDeleteGroupDialog(Player admin, PermissionGroup group) {
        UIContainer dialog = new UIContainer("delete_group")
            .setTitle("Delete Group")
            .setSize(350, 150)
            .setBackground(0x1E1E23)
            .setCentered(true);
        
        UIText warning = new UIText("warning")
            .setText("Delete group '" + group.getName() + "'?")
            .setFontSize(14)
            .setColor(0xFF4500)
            .setPosition(20, 30);
        dialog.addChild(warning);
        
        UIText note = new UIText("note")
            .setText("Members will be moved to default group.")
            .setFontSize(11)
            .setColor(0x808090)
            .setPosition(20, 55);
        dialog.addChild(note);
        
        UIButton confirmBtn = new UIButton("confirm")
            .setText("Delete")
            .setSize(145, 40)
            .setPosition(20, 90)
            .setBackground(0x8B0000)
            .onClick(() -> {
                Server.deletePermissionGroup(group.getName());
                admin.sendMessage("&aDeleted group: " + group.getName());
                showGroupPermissions(admin);
            });
        dialog.addChild(confirmBtn);
        
        UIButton cancelBtn = new UIButton("cancel")
            .setText("Cancel")
            .setSize(145, 40)
            .setPosition(185, 90)
            .setBackground(0x505060)
            .onClick(() -> showGroupPermissions(admin));
        dialog.addChild(cancelBtn);
        
        RubidiumUI.showUI(admin, dialog);
    }
}
