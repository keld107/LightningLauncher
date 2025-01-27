package com.threethan.launcher.adapter;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.threethan.launcher.R;
import com.threethan.launcher.helper.App;
import com.threethan.launcher.helper.Dialog;
import com.threethan.launcher.helper.Platform;
import com.threethan.launcher.helper.Settings;
import com.threethan.launcher.launcher.LauncherActivity;
import com.threethan.launcher.lib.StringLib;
import com.threethan.launcher.support.SettingsManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/*
    GroupsAdapter

    The adapter for the groups grid view.

    It handles the appearance of groups and the edit group dialog.
    Notably, it does not handle group selection; that's done in LauncherActivity
 */
public class GroupsAdapter extends BaseAdapter {
    private LauncherActivity launcherActivity;
    private final List<String> appGroups;
    private final Set<String> selectedGroups;
    private final SettingsManager settingsManager;
    private final boolean isEditMode;
    public GroupsAdapter(LauncherActivity activity, boolean editMode) {
        launcherActivity = activity;
        isEditMode = editMode;
        settingsManager = SettingsManager.getInstance(activity);

        SettingsManager settings = SettingsManager.getInstance(launcherActivity);
        appGroups = Collections.synchronizedList(settings.getAppGroupsSorted(false));

        if (!editMode) appGroups.remove(Settings.HIDDEN_GROUP);
        if (editMode && appGroups.size() <= Settings.MAX_GROUPS) appGroups.add("+ " + launcherActivity.getString(R.string.add_group));

        selectedGroups = settings.getSelectedGroups();
        if (!editMode) selectedGroups.remove(Settings.HIDDEN_GROUP);
        if (selectedGroups.isEmpty()) selectedGroups.addAll(appGroups);
    }
    public void setLauncherActivity(LauncherActivity val) {
        launcherActivity = val;
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
        if (Settings.HIDDEN_GROUP.equals(value)) textView.setText(launcherActivity.getString(R.string.apps_hidden));
        else textView.setText(value);
    }
    @Override
    @SuppressLint({"UseSwitchCompatOrMaterialCode", "UseCompatLoadingForDrawables"})
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            convertView = inflater.inflate(R.layout.lv_group, parent, false);

            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else holder = (ViewHolder) convertView.getTag();

        setTextViewValue(holder.textView, getItem(position));

        holder.textView.setOnClickListener((view) -> {
            if (launcherActivity == null) return;
            if (launcherActivity.clickGroup(position)) holder.menu.callOnClick();
        });
        holder.textView.setOnLongClickListener((view) -> {
            if (launcherActivity == null) return false;
            if (launcherActivity.longClickGroup(position)) holder.menu.callOnClick();
            return true;
        });

        holder.textView.setOnHoverListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_HOVER_ENTER)
                holder.textView.setBackgroundResource(R.drawable.bkg_hover_button_bar_hovered);
            else if (event.getAction() == MotionEvent.ACTION_HOVER_EXIT)
                holder.textView.setBackground(null);
            return false;
        });
        holder.textView.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus || view.isHovered()) holder.textView.setBackgroundResource(R.drawable.bkg_hover_button_bar_hovered);
            else holder.textView.setBackground(null);
        });

        // set menu action
        holder.menu.setOnClickListener(view -> {

            final Map<String, String> apps = SettingsManager.getAppGroupMap();
            final Set<String> appGroupsSet = SettingsManager.getAppGroups();
            final String groupName = settingsManager.getAppGroupsSorted(false).get(position);
            if (groupName == null) return;

            AlertDialog dialog = Dialog.build(launcherActivity, R.layout.dialog_group_details);
            if (dialog == null) return;

            final EditText groupNameInput = dialog.findViewById(R.id.groupName);
            groupNameInput.setText(StringLib.withoutStar(groupName));

            final boolean[] starred = {StringLib.hasStar(groupName)};
            ImageView starButton = dialog.findViewById(R.id.starGroupButton);
            starButton.setImageResource(starred[0] ? R.drawable.ic_star_on : R.drawable.ic_star_off);
            starButton.setOnClickListener(view1 -> {
                    starred[0] = !starred[0];
                    starButton.setImageResource(starred[0] ? R.drawable.ic_star_on : R.drawable.ic_star_off);
            });

            final Map<App.Type, Switch> switchByType = new HashMap<>();
            switchByType.put(App.Type.TYPE_PHONE, dialog.findViewById(R.id.default2dSwitch));
            switchByType.put(App.Type.TYPE_VR, dialog.findViewById(R.id.defaultVrSwitch));
            switchByType.put(App.Type.TYPE_TV, dialog.findViewById(R.id.defaultTvSwitch));
            switchByType.put(App.Type.TYPE_PANEL, dialog.findViewById(R.id.defaultPanelSwitch));
            switchByType.put(App.Type.TYPE_WEB, dialog.findViewById(R.id.defaultWebSwitch));
            final Map<App.Type, View> switchContainerByType = new HashMap<>();
            switchContainerByType.put(App.Type.TYPE_PHONE, dialog.findViewById(R.id.defaultPhoneContainer));
            switchContainerByType.put(App.Type.TYPE_VR, dialog.findViewById(R.id.defaultVrContainer));
            switchContainerByType.put(App.Type.TYPE_TV, dialog.findViewById(R.id.defaultTvContainer));
            switchContainerByType.put(App.Type.TYPE_PANEL, dialog.findViewById(R.id.defaultPanelContainer));
            switchContainerByType.put(App.Type.TYPE_WEB, dialog.findViewById(R.id.defaultWebsiteContainer));

            for (App.Type type : switchByType.keySet()) {
                if (Platform.getSupportedAppTypes(launcherActivity).contains(type)) {
                    Objects.requireNonNull(switchContainerByType.get(type)).setVisibility(View.VISIBLE);

                    final Switch cSwitch = switchByType.get(type);
                    if (cSwitch == null) continue;
                    cSwitch.setChecked(App.getDefaultGroupFor(type).equals(groupName));
                    cSwitch.setOnCheckedChangeListener((switchView, value) -> {
                        String newDefault = value ? groupName : Settings.FALLBACK_GROUPS.get(type);
                        if ((!value && groupName.equals(newDefault)) || !appGroups.contains(newDefault))
                            newDefault = null;
                        launcherActivity.sharedPreferenceEditor.putString(Settings.KEY_DEFAULT_GROUP + type, newDefault).apply();
                        final boolean newChecked = App.getDefaultGroupFor(type).equals(groupName);
                        if (newChecked && !value) Dialog.toast(launcherActivity.getString(R.string.toast_cant_unset_group));
                        cSwitch.setChecked(newChecked);
                    });
                } else {
                    Objects.requireNonNull(switchContainerByType.get(type)).setVisibility(View.GONE);
                }
            }

            dialog.findViewById(R.id.confirm).setOnClickListener(view1 -> {
                String newGroupName = StringLib.setStarred(groupNameInput.getText().toString(), starred[0]);
                if (newGroupName.equals(Settings.UNSUPPORTED_GROUP)) newGroupName = "UNSUPPORTED"; // Prevent permanently hiding apps

                // Move the default group when we rename
                for (App.Type type : Platform.getSupportedAppTypes(launcherActivity))
                    if (App.getDefaultGroupFor(type).equals(groupName))
                        launcherActivity.sharedPreferenceEditor.putString(Settings.KEY_DEFAULT_GROUP + type, newGroupName);

                if (newGroupName.length() > 0) {
                    appGroupsSet.remove(groupName);
                    appGroupsSet.add(newGroupName);

                    // Move apps when we rename
                    Map<String, String> updatedAppGroupMap = new HashMap<>();
                    for (String packageName : apps.keySet()) {
                        if (apps.get(packageName) != null) {
                            if (Objects.requireNonNull(apps.get(packageName)).compareTo(groupName) == 0)
                                updatedAppGroupMap.put(packageName, newGroupName);
                            else
                                updatedAppGroupMap.put(packageName, apps.get(packageName));
                        }
                    }
                    HashSet<String> selectedGroup = new HashSet<>();
                    selectedGroup.add(newGroupName);
                    settingsManager.setSelectedGroups(selectedGroup);
                    settingsManager.setAppGroups(appGroupsSet);
                    SettingsManager.setAppGroupMap(updatedAppGroupMap);
                    launcherActivity.refreshInterfaceAll();
                }
                dialog.cancel();
            });

            dialog.findViewById(R.id.deleteGroupButton).setOnClickListener(view1 -> {
                HashMap<String, String> appGroupMap = new HashMap<>();
                for (String packageName : apps.keySet())
                    if (!Objects.equals(groupName, apps.get(packageName)))
                        appGroupMap.put(packageName, apps.get(packageName));

                SettingsManager.setAppGroupMap(appGroupMap);
                appGroupsSet.remove(groupName);

                boolean hasNormalGroup = false;
                for (String groupNameIterator : appGroupsSet) {
                    if (Objects.equals(groupNameIterator, Settings.HIDDEN_GROUP)) continue;
                    if (Objects.equals(groupNameIterator, Settings.UNSUPPORTED_GROUP)) continue;
                    hasNormalGroup = true; break;
                }
                if (!hasNormalGroup) {
                    settingsManager.resetGroups();
                } else {
                    settingsManager.setAppGroups(appGroupsSet);
                    Set<String> firstSelectedGroup = new HashSet<>();
                    firstSelectedGroup.add(settingsManager.getAppGroupsSorted(false).get(0));
                    settingsManager.setSelectedGroups(firstSelectedGroup);
                }
                dialog.dismiss();

                launcherActivity.refreshInterfaceAll();
            });
        });

        setLook(position, convertView, holder.menu);
        TextView textView = convertView.findViewById(R.id.textLabel);
        setTextViewValue(textView, appGroups.get(position));

        if (Objects.equals(launcherActivity.lastSelectedGroup, position)) {
            convertView.post(convertView::requestFocus);
        }
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

            if (isEditMode && (position < getCount() - 2)) menu.setVisibility(View.VISIBLE);
            else                                           menu.setVisibility(View.GONE);
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT);
            TextView textView = itemView.findViewById(R.id.textLabel);
            textView.setTextColor(Color.parseColor(launcherActivity.darkMode ? "#98FFFFFF" : "#98000000")); // set unselected tab text color
            menu.setVisibility(View.GONE);
        }
    }
}