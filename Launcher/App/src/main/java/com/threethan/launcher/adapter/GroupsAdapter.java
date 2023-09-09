package com.threethan.launcher.adapter;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.threethan.launcher.helper.Dialog;
import com.threethan.launcher.helper.Settings;
import com.threethan.launcher.R;
import com.threethan.launcher.launcher.LauncherActivity;
import com.threethan.launcher.support.SettingsManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class GroupsAdapter extends BaseAdapter {
    public static final int MAX_GROUPS = 12;
    public static final String HIDDEN_GROUP = "HIDDEN!";
    public static final String UNSUPPORTED_GROUP = "UNSUPPORTED!";
    private final LauncherActivity launcherActivity;
    private final List<String> appGroups;
    private final Set<String> selectedGroups;
    private final SettingsManager settingsManager;
    private final boolean isEditMode;

    /**
     * Create new adapter
     */
    public GroupsAdapter(LauncherActivity activity, boolean editMode) {
        launcherActivity = activity;
        isEditMode = editMode;
        settingsManager = SettingsManager.getInstance(activity);

        SettingsManager settings = SettingsManager.getInstance(launcherActivity);
        appGroups = settings.getAppGroupsSorted(false);
        if (!editMode) {
            appGroups.remove(GroupsAdapter.HIDDEN_GROUP);
        }
        if (editMode && appGroups.size() < MAX_GROUPS) {
            appGroups.add("+ " + launcherActivity.getString(R.string.add_group));
        }

        selectedGroups = settings.getSelectedGroups();
    }

    public int getCount() {
        return appGroups.size();
    }

    public String getItem(int position) {
        return appGroups.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    static class ViewHolder {
        final TextView textView;
        final View menu;

        ViewHolder(View itemView) {
            textView = itemView.findViewById(R.id.textLabel);
            menu = itemView.findViewById(R.id.menu);
        }
    }

    private void setTextViewValue(TextView textView, String value) {
        if (HIDDEN_GROUP.equals(value)) {
            textView.setText(launcherActivity.getString(R.string.apps_hidden));
        } else {
            textView.setText(value);
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            convertView = inflater.inflate(R.layout.lv_group, parent, false);

            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        setTextViewValue(holder.textView, getItem(position));

        // set menu action
        holder.menu.getContext().getDrawable(R.drawable.ic_info);
        holder.menu.setOnClickListener(view -> {

            final Map<String, String> apps = SettingsManager.getAppGroupMap();
            final Set<String> appGroupsList = settingsManager.getAppGroups();
            final String groupName = settingsManager.getAppGroupsSorted(false).get(position);

            AlertDialog dialog = Dialog.build(launcherActivity, R.layout.dialog_group_details);

            final EditText groupNameInput = dialog.findViewById(R.id.groupName);
            groupNameInput.setText(groupName);

            @SuppressLint("UseSwitchCompatOrMaterialCode")
            Switch switch_2d = dialog.findViewById(R.id.default2dSwitch);
            @SuppressLint("UseSwitchCompatOrMaterialCode")
            Switch switch_vr = dialog.findViewById(R.id.defaultVrSwitch);
            @SuppressLint("UseSwitchCompatOrMaterialCode")
            Switch switch_web = dialog.findViewById(R.id.defaultWebSwitch);
            switch_2d .setChecked(SettingsManager.getDefaultGroup(false, false).equals(groupName));
            switch_vr .setChecked(SettingsManager.getDefaultGroup(true, false) .equals(groupName));
            switch_web.setChecked(SettingsManager.getDefaultGroup(false, true) .equals(groupName));
            switch_2d .setOnCheckedChangeListener((switchView, value) -> {
                String newDefault = value ? groupName : Settings.DEFAULT_GROUP_2D;
                if ((!value && groupName.equals(newDefault)) || !appGroups.contains(newDefault)) newDefault = null;
                launcherActivity.sharedPreferenceEditor.putString(Settings.KEY_GROUP_2D , newDefault);
            });
            switch_vr .setOnCheckedChangeListener((switchView, value) -> {
                String newDefault = value ? groupName : Settings.DEFAULT_GROUP_VR;
                if ((!value && groupName.equals(newDefault)) || !appGroups.contains(newDefault)) newDefault = null;
                launcherActivity.sharedPreferenceEditor.putString(Settings.KEY_GROUP_VR , newDefault);
            });
            switch_web .setOnCheckedChangeListener((switchView, value) -> {
                String newDefault = value ? groupName : Settings.DEFAULT_GROUP_VR;
                if ((!value && groupName.equals(newDefault)) || !appGroups.contains(newDefault)) newDefault = null;
                launcherActivity.sharedPreferenceEditor.putString(Settings.KEY_GROUP_WEB, newDefault);
            });

            dialog.findViewById(R.id.confirm).setOnClickListener(view1 -> {
                String newGroupName = groupNameInput.getText().toString();
                if (newGroupName.equals(UNSUPPORTED_GROUP)) newGroupName = "UNSUPPORTED"; // Prevent permanently hiding apps

                // Move the default group when we rename
                if (SettingsManager.getDefaultGroup(false, false).equals(groupName))
                    launcherActivity.sharedPreferenceEditor.putString(Settings.KEY_GROUP_2D, newGroupName);
                if (SettingsManager.getDefaultGroup(true, false).equals(groupName))
                    launcherActivity.sharedPreferenceEditor.putString(Settings.KEY_GROUP_VR, newGroupName);
                if (SettingsManager.getDefaultGroup(false, true).equals(groupName))
                    launcherActivity.sharedPreferenceEditor.putString(Settings.KEY_GROUP_WEB,newGroupName);


                if (newGroupName.length() > 0) {
                    appGroupsList.remove(groupName);
                    appGroupsList.add(newGroupName);

                    // Move apps when we rename
                    Map<String, String> updatedAppGroupMap = new HashMap<>();
                    for (String packageName : apps.keySet()) {
                        if (apps.get(packageName) != null) {
                            if (Objects.requireNonNull(apps.get(packageName)).compareTo(groupName) == 0) {
                                updatedAppGroupMap.put(packageName, newGroupName);
                            } else {
                                updatedAppGroupMap.put(packageName, apps.get(packageName));
                            }
                        }
                    }

                    HashSet<String> selectedGroup = new HashSet<>();
                    selectedGroup.add(newGroupName);
                    settingsManager.setSelectedGroups(selectedGroup);
                    settingsManager.setAppGroups(appGroupsList);
                    SettingsManager.setAppGroupMap(updatedAppGroupMap);
                    launcherActivity.refresh();
                }
                dialog.cancel();
            });

            dialog.findViewById(R.id.deleteGroupButton).setOnClickListener(view2 -> {
                HashMap<String, String> appGroupMap = new HashMap<>();
                for (String packageName : apps.keySet()) {
                    if (groupName.equals(apps.get(packageName))) {
                        appGroupMap.put(packageName, null);
                    } else {
                        appGroupMap.put(packageName, apps.get(packageName));
                    }
                }
                SettingsManager.setAppGroupMap(appGroupMap);

                appGroupsList.remove(groupName);

                if (appGroupsList.size() <= 1) {
                    settingsManager.resetGroups();
                } else {

                    settingsManager.setAppGroups(appGroupsList);

                    Set<String> firstSelectedGroup = new HashSet<>();
                    firstSelectedGroup.add(settingsManager.getAppGroupsSorted(false).get(0));
                    settingsManager.setSelectedGroups(firstSelectedGroup);
                }
                dialog.dismiss();

                launcherActivity.refresh();
            });
        });

        // set the look
        setLook(position, convertView, holder.menu);

        TextView textView = convertView.findViewById(R.id.textLabel);
        setTextViewValue(textView, appGroups.get(position));

        return convertView;
    }

    public void setGroup(String packageName, String groupName) {
        Map<String, String> appGroupMap = SettingsManager.getAppGroupMap();
        appGroupMap.remove(packageName);
        appGroupMap.put(packageName, groupName);
        SettingsManager.setAppGroupMap(appGroupMap);
    }

    private void setLook(int position, View itemView, View menu) {
        boolean isSelected = selectedGroups.contains(appGroups.get(position));

        if (isSelected) {
            boolean isLeft = (position == 0) || !selectedGroups.contains(appGroups.get(position - 1));
            boolean isRight = (position + 1 >= appGroups.size()) || !selectedGroups.contains(appGroups.get(position + 1));

            int shapeResourceId;
            if (isLeft && isRight) {
                shapeResourceId = R.drawable.tab_selected;
            } else if (isLeft) {
                shapeResourceId = R.drawable.tab_selected_left;
            } else if (isRight) {
                shapeResourceId = R.drawable.tab_selected_right;
            } else {
                shapeResourceId = R.drawable.tab_selected_middle;
            }
            itemView.setBackgroundResource(shapeResourceId);
            itemView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(launcherActivity.darkMode ? "#50000000" : "#FFFFFF")));
            TextView textView = itemView.findViewById(R.id.textLabel);
            textView.setTextColor(Color.parseColor(launcherActivity.darkMode ? "#FFFFFFFF" : "#FF000000")); // set selected tab text color
            if (isEditMode && (position < getCount() - 2)) {
                menu.setVisibility(View.VISIBLE);
//                menu.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(mainActivity.darkMode ? "#FFFFFFFF" : "#FF000000"))); // set selected tab text color

            } else {
                menu.setVisibility(View.GONE);
            }
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT);
            TextView textView = itemView.findViewById(R.id.textLabel);
            textView.setTextColor(Color.parseColor(launcherActivity.darkMode ? "#98FFFFFF" : "#98000000")); // set unselected tab text color
            menu.setVisibility(View.GONE);
        }
    }
}