package com.example.checklist;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

public class MainActivity extends AppCompatActivity {

    // ----- 数据模型 -----
    public static class Group {
        public String id;
        public String name;
        public String parentId;
        public int level;
    }

    public static class File {
        public String id;
        public String name;
        public String groupId;
    }

    public static class TaskItem {
        public String id;
        public String title;
        public boolean completed;
        public int timerMode;
        public int periodType;
        public int periodDays;
        public int weekDay;
        public int resetHour;
        public int resetMinute;
        public int afterHours;
        public long lastClearTime;
        public String fileId;
    }

    // ----- 数据持久化 -----
    private static final String SP_NAME = "task_data";
    private static final String KEY_DATA = "json";
    private static final int LIGHT_BLUE = 0xFF64B5F6;

    private static JSONObject loadData(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        String json = sp.getString(KEY_DATA, "{}");
        try {
            return new JSONObject(json);
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    private static void saveData(Context context, JSONObject data) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_DATA, data.toString()).apply();
    }

    // 解析
    private static List<Group> parseGroups(JSONObject data) {
        List<Group> list = new ArrayList<>();
        JSONArray arr = data.optJSONArray("groups");
        if (arr == null) return list;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.optJSONObject(i);
            if (obj == null) continue;
            Group g = new Group();
            g.id = obj.optString("id");
            g.name = obj.optString("name");
            g.parentId = obj.optString("parentId");
            if (g.parentId.equals("null")) g.parentId = null;
            g.level = obj.optInt("level");
            list.add(g);
        }
        return list;
    }

    private static List<File> parseFiles(JSONObject data) {
        List<File> list = new ArrayList<>();
        JSONArray arr = data.optJSONArray("files");
        if (arr == null) return list;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.optJSONObject(i);
            if (obj == null) continue;
            File f = new File();
            f.id = obj.optString("id");
            f.name = obj.optString("name");
            f.groupId = obj.optString("groupId");
            if (f.groupId.equals("null")) f.groupId = null;
            list.add(f);
        }
        return list;
    }

    private static List<TaskItem> parseItems(JSONObject data) {
        List<TaskItem> list = new ArrayList<>();
        JSONArray arr = data.optJSONArray("items");
        if (arr == null) return list;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.optJSONObject(i);
            if (obj == null) continue;
            TaskItem t = new TaskItem();
            t.id = obj.optString("id");
            t.title = obj.optString("title");
            t.completed = obj.optBoolean("completed");
            t.timerMode = obj.optInt("timerMode", 0);
            t.periodType = obj.optInt("periodType", 0);
            t.periodDays = obj.optInt("periodDays", 0);
            t.weekDay = obj.optInt("weekDay", 1);
            t.resetHour = obj.optInt("resetHour", 0);
            t.resetMinute = obj.optInt("resetMinute", 0);
            t.afterHours = obj.optInt("afterHours", 0);
            t.lastClearTime = obj.optLong("lastClearTime");
            t.fileId = obj.optString("fileId");
            if (t.fileId.equals("null")) t.fileId = null;
            list.add(t);
        }
        return list;
    }

    // 序列化
    private static JSONArray groupsToJson(List<Group> groups) throws JSONException {
        JSONArray arr = new JSONArray();
        for (Group g : groups) {
            JSONObject obj = new JSONObject();
            obj.put("id", g.id);
            obj.put("name", g.name);
            obj.put("parentId", g.parentId == null ? JSONObject.NULL : g.parentId);
            obj.put("level", g.level);
            arr.put(obj);
        }
        return arr;
    }

    private static JSONArray filesToJson(List<File> files) throws JSONException {
        JSONArray arr = new JSONArray();
        for (File f : files) {
            JSONObject obj = new JSONObject();
            obj.put("id", f.id);
            obj.put("name", f.name);
            obj.put("groupId", f.groupId == null ? JSONObject.NULL : f.groupId);
            arr.put(obj);
        }
        return arr;
    }

    private static JSONArray itemsToJson(List<TaskItem> items) throws JSONException {
        JSONArray arr = new JSONArray();
        for (TaskItem t : items) {
            JSONObject obj = new JSONObject();
            obj.put("id", t.id);
            obj.put("title", t.title);
            obj.put("completed", t.completed);
            obj.put("timerMode", t.timerMode);
            obj.put("periodType", t.periodType);
            obj.put("periodDays", t.periodDays);
            obj.put("weekDay", t.weekDay);
            obj.put("resetHour", t.resetHour);
            obj.put("resetMinute", t.resetMinute);
            obj.put("afterHours", t.afterHours);
            obj.put("lastClearTime", t.lastClearTime);
            obj.put("fileId", t.fileId == null ? JSONObject.NULL : t.fileId);
            arr.put(obj);
        }
        return arr;
    }

    private static Group findGroupById(List<Group> groups, String id) {
        for (Group g : groups) if (g.id.equals(id)) return g;
        return null;
    }

    private static File findFileById(List<File> files, String id) {
        for (File f : files) if (f.id.equals(id)) return f;
        return null;
    }

    // ----- 迁移旧数据 -----
    private void migrateOldDataIfNeeded() {
        JSONObject data = loadData(this);
        if (data.has("files") && data.has("items")) {
            List<TaskItem> items = parseItems(data);
            boolean needUpdate = false;
            for (TaskItem t : items) {
                if (t.periodDays > 0 && t.periodType == 0) {
                    t.periodType = 3;
                    needUpdate = true;
                }
            }
            if (needUpdate) {
                try {
                    data.put("items", itemsToJson(items));
                    saveData(this, data);
                } catch (JSONException e) { e.printStackTrace(); }
            }
            return;
        }
        JSONArray oldTasks = data.optJSONArray("tasks");
        if (oldTasks == null || oldTasks.length() == 0) return;

        List<File> files = new ArrayList<>();
        List<TaskItem> items = new ArrayList<>();
        for (int i = 0; i < oldTasks.length(); i++) {
            JSONObject obj = oldTasks.optJSONObject(i);
            if (obj == null) continue;
            String title = obj.optString("title");
            boolean completed = obj.optBoolean("completed");
            int period = obj.optInt("periodDays");
            long lastClear = obj.optLong("lastClearTime");
            String groupId = obj.optString("groupId");
            if (groupId.equals("null")) groupId = null;

            File f = new File();
            f.id = UUID.randomUUID().toString();
            f.name = title;
            f.groupId = groupId;
            files.add(f);

            TaskItem item = new TaskItem();
            item.id = UUID.randomUUID().toString();
            item.title = title;
            item.completed = completed;
            item.timerMode = 0;
            item.periodType = (period > 0) ? 3 : 0;
            item.periodDays = period;
            item.weekDay = 1;
            item.resetHour = 0;
            item.resetMinute = 0;
            item.afterHours = 0;
            item.lastClearTime = lastClear;
            item.fileId = f.id;
            items.add(item);
        }

        try {
            data.put("files", filesToJson(files));
            data.put("items", itemsToJson(items));
            data.remove("tasks");
            saveData(this, data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // ----- 业务函数 -----
    private List<Group> getChildGroups(String parentId) {
        List<Group> all = parseGroups(loadData(this));
        List<Group> result = new ArrayList<>();
        for (Group g : all) {
            if (parentId == null) {
                if (g.parentId == null) result.add(g);
            } else {
                if (parentId.equals(g.parentId)) result.add(g);
            }
        }
        return result;
    }

    private List<File> getFilesInGroup(String groupId) {
        List<File> all = parseFiles(loadData(this));
        List<File> result = new ArrayList<>();
        for (File f : all) {
            if (groupId == null) {
                if (f.groupId == null) result.add(f);
            } else {
                if (groupId.equals(f.groupId)) result.add(f);
            }
        }
        return result;
    }

    private List<TaskItem> getItemsInFile(String fileId) {
        List<TaskItem> all = parseItems(loadData(this));
        List<TaskItem> result = new ArrayList<>();
        for (TaskItem t : all) {
            if (fileId.equals(t.fileId)) result.add(t);
        }
        return result;
    }

    private int[] getFileProgress(String fileId) {
        List<TaskItem> items = getItemsInFile(fileId);
        int total = items.size();
        int done = 0;
        for (TaskItem t : items) if (t.completed) done++;
        return new int[]{done, total};
    }

    private void addGroup(String name, String parentId) {
        JSONObject data = loadData(this);
        List<Group> groups = parseGroups(data);
        for (Group g : groups) {
            if (Objects.equals(g.parentId, parentId) && g.name.equals(name)) {
                Toast.makeText(this, "同名文件夹已存在", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        int level = 0;
        if (parentId != null) {
            Group parent = findGroupById(groups, parentId);
            if (parent == null) return;
            level = parent.level + 1;
            if (level >= 3) {
                Toast.makeText(this, "文件夹深度不能超过3层", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        Group g = new Group();
        g.id = UUID.randomUUID().toString();
        g.name = name;
        g.parentId = parentId;
        g.level = level;
        groups.add(g);
        try {
            data.put("groups", groupsToJson(groups));
            saveData(this, data);
        } catch (JSONException e) { e.printStackTrace(); }
    }

    private void renameGroup(String groupId, String newName) {
        JSONObject data = loadData(this);
        List<Group> groups = parseGroups(data);
        Group target = findGroupById(groups, groupId);
        if (target == null) return;
        for (Group g : groups) {
            if (Objects.equals(g.parentId, target.parentId) && g.name.equals(newName) && !g.id.equals(groupId)) {
                Toast.makeText(this, "同名文件夹已存在", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        target.name = newName;
        try {
            data.put("groups", groupsToJson(groups));
            saveData(this, data);
        } catch (JSONException e) { e.printStackTrace(); }
    }

    private void deleteGroup(String groupId) {
        JSONObject data = loadData(this);
        List<Group> groups = parseGroups(data);
        List<File> files = parseFiles(data);
        List<TaskItem> items = parseItems(data);

        Set<String> groupIds = new HashSet<>();
        collectAllGroupIds(groups, groupId, groupIds);
        groupIds.add(groupId);

        groups.removeIf(g -> groupIds.contains(g.id));
        Set<String> fileIds = new HashSet<>();
        for (File f : files) {
            if (groupIds.contains(f.groupId)) fileIds.add(f.id);
        }
        files.removeIf(f -> groupIds.contains(f.groupId));
        items.removeIf(t -> fileIds.contains(t.fileId));

        try {
            data.put("groups", groupsToJson(groups));
            data.put("files", filesToJson(files));
            data.put("items", itemsToJson(items));
            saveData(this, data);
        } catch (JSONException e) { e.printStackTrace(); }
    }

    private void collectAllGroupIds(List<Group> groups, String parentId, Set<String> out) {
        for (Group g : groups) {
            if (parentId.equals(g.parentId)) {
                out.add(g.id);
                collectAllGroupIds(groups, g.id, out);
            }
        }
    }

    private void addFile(String name, String groupId) {
        JSONObject data = loadData(this);
        List<File> files = parseFiles(data);
        for (File f : files) {
            if (Objects.equals(f.groupId, groupId) && f.name.equals(name)) {
                Toast.makeText(this, "同名文件已存在", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        File f = new File();
        f.id = UUID.randomUUID().toString();
        f.name = name;
        f.groupId = groupId;
        files.add(f);
        try {
            data.put("files", filesToJson(files));
            saveData(this, data);
        } catch (JSONException e) { e.printStackTrace(); }
    }

    private void renameFile(String fileId, String newName) {
        JSONObject data = loadData(this);
        List<File> files = parseFiles(data);
        File target = findFileById(files, fileId);
        if (target == null) return;
        for (File f : files) {
            if (Objects.equals(f.groupId, target.groupId) && f.name.equals(newName) && !f.id.equals(fileId)) {
                Toast.makeText(this, "同名文件已存在", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        target.name = newName;
        try {
            data.put("files", filesToJson(files));
            saveData(this, data);
        } catch (JSONException e) { e.printStackTrace(); }
    }

    private void deleteFileItem(String fileId) {
        JSONObject data = loadData(this);
        List<File> files = parseFiles(data);
        List<TaskItem> items = parseItems(data);
        files.removeIf(f -> f.id.equals(fileId));
        items.removeIf(t -> t.fileId.equals(fileId));
        try {
            data.put("files", filesToJson(files));
            data.put("items", itemsToJson(items));
            saveData(this, data);
        } catch (JSONException e) { e.printStackTrace(); }
    }

    private void addItem(String title, int timerMode, int periodType, int periodDays, int weekDay,
                         int resetHour, int resetMinute, int afterHours, String fileId, boolean completed) {
        JSONObject data = loadData(this);
        List<TaskItem> items = parseItems(data);
        TaskItem t = new TaskItem();
        t.id = UUID.randomUUID().toString();
        t.title = title;
        t.completed = completed;
        t.timerMode = timerMode;
        t.periodType = periodType;
        t.periodDays = periodDays;
        t.weekDay = weekDay;
        t.resetHour = resetHour;
        t.resetMinute = resetMinute;
        t.afterHours = afterHours;
        t.lastClearTime = System.currentTimeMillis();
        t.fileId = fileId;
        items.add(t);
        try {
            data.put("items", itemsToJson(items));
            saveData(this, data);
        } catch (JSONException e) { e.printStackTrace(); }
    }

    private void updateItem(String itemId, String newTitle, int timerMode, int periodType, int periodDays,
                            int weekDay, int resetHour, int resetMinute, int afterHours, boolean newCompleted) {
        JSONObject data = loadData(this);
        List<TaskItem> items = parseItems(data);
        for (TaskItem t : items) {
            if (t.id.equals(itemId)) {
                t.title = newTitle;
                t.timerMode = timerMode;
                t.periodType = periodType;
                t.periodDays = periodDays;
                t.weekDay = weekDay;
                t.resetHour = resetHour;
                t.resetMinute = resetMinute;
                t.afterHours = afterHours;
                t.completed = newCompleted;
                break;
            }
        }
        try {
            data.put("items", itemsToJson(items));
            saveData(this, data);
        } catch (JSONException e) { e.printStackTrace(); }
    }

    private void toggleItemCompleted(String itemId) {
        JSONObject data = loadData(this);
        List<TaskItem> items = parseItems(data);
        for (TaskItem t : items) {
            if (t.id.equals(itemId)) {
                t.completed = !t.completed;
                if (t.completed && t.timerMode == 1) {
                    t.lastClearTime = System.currentTimeMillis();
                }
                break;
            }
        }
        try {
            data.put("items", itemsToJson(items));
            saveData(this, data);
        } catch (JSONException e) { e.printStackTrace(); }
    }

    private void deleteItem(String itemId) {
        JSONObject data = loadData(this);
        List<TaskItem> items = parseItems(data);
        items.removeIf(t -> t.id.equals(itemId));
        try {
            data.put("items", itemsToJson(items));
            saveData(this, data);
        } catch (JSONException e) { e.printStackTrace(); }
    }

    private long getRemainingMillis(TaskItem t) {
        long now = System.currentTimeMillis();
        if (t.timerMode == 0) {
            if (t.periodType == 0) return Long.MAX_VALUE;
            Calendar resetCal = Calendar.getInstance();
            resetCal.set(Calendar.HOUR_OF_DAY, t.resetHour);
            resetCal.set(Calendar.MINUTE, t.resetMinute);
            resetCal.set(Calendar.SECOND, 0);
            resetCal.set(Calendar.MILLISECOND, 0);

            if (t.periodType == 1) {
                if (resetCal.getTimeInMillis() <= now) {
                    resetCal.add(Calendar.DAY_OF_YEAR, 1);
                }
            } else if (t.periodType == 2) {
                int targetWeekDay = t.weekDay + 1;
                if (targetWeekDay == 8) targetWeekDay = 1;
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(now);
                int currentWeekDay = cal.get(Calendar.DAY_OF_WEEK);
                if (currentWeekDay == targetWeekDay) {
                    Calendar todayReset = Calendar.getInstance();
                    todayReset.set(Calendar.HOUR_OF_DAY, t.resetHour);
                    todayReset.set(Calendar.MINUTE, t.resetMinute);
                    todayReset.set(Calendar.SECOND, 0);
                    todayReset.set(Calendar.MILLISECOND, 0);
                    if (todayReset.getTimeInMillis() > now) {
                        resetCal = todayReset;
                    } else {
                        cal.add(Calendar.DAY_OF_YEAR, 7);
                        cal.set(Calendar.HOUR_OF_DAY, t.resetHour);
                        cal.set(Calendar.MINUTE, t.resetMinute);
                        cal.set(Calendar.SECOND, 0);
                        cal.set(Calendar.MILLISECOND, 0);
                        resetCal = cal;
                    }
                } else {
                    int daysToAdd = (targetWeekDay - currentWeekDay + 7) % 7;
                    if (daysToAdd == 0) daysToAdd = 7;
                    cal.add(Calendar.DAY_OF_YEAR, daysToAdd);
                    cal.set(Calendar.HOUR_OF_DAY, t.resetHour);
                    cal.set(Calendar.MINUTE, t.resetMinute);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    resetCal = cal;
                }
            } else if (t.periodType == 3) {
                Calendar last = Calendar.getInstance();
                last.setTimeInMillis(t.lastClearTime);
                last.add(Calendar.DAY_OF_YEAR, t.periodDays);
                last.set(Calendar.HOUR_OF_DAY, t.resetHour);
                last.set(Calendar.MINUTE, t.resetMinute);
                last.set(Calendar.SECOND, 0);
                last.set(Calendar.MILLISECOND, 0);
                while (last.getTimeInMillis() <= now) {
                    last.add(Calendar.DAY_OF_YEAR, t.periodDays);
                }
                resetCal = last;
            }
            return Math.max(0, resetCal.getTimeInMillis() - now);
        } else {
            if (!t.completed) return Long.MAX_VALUE;
            long targetTime = t.lastClearTime + t.afterHours * 3600 * 1000L;
            if (targetTime <= now) return 0;
            else return targetTime - now;
        }
    }

    private String formatRemaining(long millis) {
        if (millis == Long.MAX_VALUE) return "∞";
        if (millis <= 0) return "已超时";
        long totalSeconds = millis / 1000;
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        if (days > 0) return days + "天" + hours + "小时" + minutes + "分";
        else if (hours > 0) return hours + "小时" + minutes + "分";
        else return minutes + "分";
    }

    private String getPeriodDescription(TaskItem t) {
        if (t.timerMode == 0) {
            switch (t.periodType) {
                case 0: return "永不";
                case 1: return "每天 " + String.format("%02d:%02d", t.resetHour, t.resetMinute);
                case 2: {
                    String[] weekDays = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
                    return "每周" + weekDays[t.weekDay - 1] + " " + String.format("%02d:%02d", t.resetHour, t.resetMinute);
                }
                case 3: return "每" + t.periodDays + "天 " + String.format("%02d:%02d", t.resetHour, t.resetMinute);
                default: return "";
            }
        } else {
            return "完成后 " + t.afterHours + "小时";
        }
    }

    private void autoResetExpiredItems() {
        JSONObject data = loadData(this);
        List<TaskItem> items = parseItems(data);
        long now = System.currentTimeMillis();
        boolean changed = false;
        for (TaskItem t : items) {
            if (t.timerMode == 1 && t.completed) {
                long targetTime = t.lastClearTime + t.afterHours * 3600 * 1000L;
                if (targetTime <= now) {
                    t.completed = false;
                    changed = true;
                }
            }
        }
        if (changed) {
            try {
                data.put("items", itemsToJson(items));
                saveData(this, data);
            } catch (JSONException e) { e.printStackTrace(); }
        }
    }

    // ----- UI 控件 -----
    private LinearLayout rootLayout;
    private TextView tvTitle;
    private LinearLayout addressBarLayout;
    private LinearLayout bottomBar;
    private TextView btnBack;
    private TextView tvItemCount;
    private LinearLayout listContainer;
    private ScrollView scrollView;
    private FloatingActionButton fabNew;
    private LinearLayout filterSortBar;
    private Spinner spinnerFilter;
    private Spinner spinnerSort;

    private static final int VIEW_MAIN = 0;
    private static final int VIEW_FILE_DETAIL = 1;
    private int currentView = VIEW_MAIN;
    private String currentGroupId = null;
    private String currentFileId = null;
    private String searchKeyword = null;
    private String filterMode = "all";
    private String sortMode = "reset_asc";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        migrateOldDataIfNeeded();

        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(0xFFF5F7FA);
        rootLayout.setPadding(dp(14), dp(14), dp(14), dp(14));

        tvTitle = new TextView(this);
        tvTitle.setText("根目录");
        tvTitle.setTextSize(28);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setTextColor(0xFF1F2937);
        tvTitle.setPadding(0, 0, 0, dp(18));
        rootLayout.addView(tvTitle);

        addressBarLayout = new LinearLayout(this);
        addressBarLayout.setOrientation(LinearLayout.HORIZONTAL);
        addressBarLayout.setGravity(Gravity.CENTER_VERTICAL);
        addressBarLayout.setPadding(0, 0, 0, dp(5));
        rootLayout.addView(addressBarLayout);

        bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setGravity(Gravity.CENTER_VERTICAL);
        bottomBar.setPadding(0, 0, 0, dp(6));

        btnBack = new TextView(this);
        btnBack.setText("←");
        btnBack.setTextSize(20);
        btnBack.setTextColor(LIGHT_BLUE);
        int btnSize = dp(30);
        btnBack.setLayoutParams(new LinearLayout.LayoutParams(btnSize, btnSize));
        btnBack.setGravity(Gravity.CENTER);
        btnBack.setBackground(null);
        btnBack.setOnClickListener(v -> onBackPressed());
        bottomBar.addView(btnBack);

        tvItemCount = new TextView(this);
        tvItemCount.setText("共 0 个项目");
        tvItemCount.setTextColor(0xFF6B7280);
        tvItemCount.setTextSize(14);
        tvItemCount.setGravity(Gravity.START);
        bottomBar.addView(tvItemCount, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        rootLayout.addView(bottomBar);

        // 筛选排序
        filterSortBar = new LinearLayout(this);
        filterSortBar.setOrientation(LinearLayout.HORIZONTAL);
        filterSortBar.setGravity(Gravity.CENTER_VERTICAL);
        filterSortBar.setPadding(0, 0, 0, dp(6));
        filterSortBar.setVisibility(View.GONE);

        spinnerFilter = new Spinner(this);
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"全部", "已完成", "未完成"});
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(filterAdapter);
        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: filterMode = "all"; break;
                    case 1: filterMode = "completed"; break;
                    case 2: filterMode = "uncompleted"; break;
                }
                if (currentView == VIEW_FILE_DETAIL) refreshFileDetail();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        ViewGroup filterParent = (ViewGroup) spinnerFilter.getChildAt(0);
        if (filterParent != null && filterParent.getChildAt(0) instanceof TextView) {
            ((TextView) filterParent.getChildAt(0)).setTextSize(14);
        }
        filterSortBar.addView(spinnerFilter, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        spinnerSort = new Spinner(this);
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"重置时间 ↑", "重置时间 ↓", "剩余时间 ↑", "剩余时间 ↓"});
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(sortAdapter);
        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: sortMode = "reset_asc"; break;
                    case 1: sortMode = "reset_desc"; break;
                    case 2: sortMode = "remain_asc"; break;
                    case 3: sortMode = "remain_desc"; break;
                }
                if (currentView == VIEW_FILE_DETAIL) refreshFileDetail();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        ViewGroup sortParent = (ViewGroup) spinnerSort.getChildAt(0);
        if (sortParent != null && sortParent.getChildAt(0) instanceof TextView) {
            ((TextView) sortParent.getChildAt(0)).setTextSize(14);
        }
        filterSortBar.addView(spinnerSort, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        rootLayout.addView(filterSortBar);

        // 列表容器
        scrollView = new ScrollView(this);
        LinearLayout containerWrapper = new LinearLayout(this);
        containerWrapper.setOrientation(LinearLayout.VERTICAL);
        containerWrapper.setBackground(createRoundedBackground(0xFFFFFFFF, dp(8)));
        containerWrapper.setPadding(dp(5), dp(5), dp(5), dp(5));

        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        containerWrapper.addView(listContainer);

        scrollView.addView(containerWrapper);
        rootLayout.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        // 浮动按钮
        fabNew = new FloatingActionButton(this);
        Drawable addIcon = ContextCompat.getDrawable(this, android.R.drawable.ic_input_add);
        if (addIcon != null) {
            fabNew.setImageDrawable(addIcon);
        } else {
            fabNew.setImageResource(android.R.drawable.ic_menu_add);
        }
        fabNew.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        fabNew.setPadding(0, 0, 0, 0);
        fabNew.setBackgroundTintList(android.content.res.ColorStateList.valueOf(LIGHT_BLUE));
        fabNew.setColorFilter(Color.WHITE);
        int fabSize = dp(50);
        fabNew.setLayoutParams(new FrameLayout.LayoutParams(fabSize, fabSize));
        fabNew.setOnClickListener(v -> showNewPopupMenu(v));

        FrameLayout frameRoot = new FrameLayout(this);
        frameRoot.addView(rootLayout);
        FrameLayout.LayoutParams fabParams = new FrameLayout.LayoutParams(
                fabSize, fabSize, Gravity.BOTTOM | Gravity.END);
        fabParams.setMargins(0, 0, dp(18), dp(18));
        frameRoot.addView(fabNew, fabParams);
        setContentView(frameRoot);

        showMainView();
    }

    // ----- 视图切换 -----
    private void showMainView() {
        currentView = VIEW_MAIN;
        currentFileId = null;
        filterSortBar.setVisibility(View.GONE);
        refreshMainList();
        updateTitleAndAddressBar();
        updateBottomBar();
    }

    private void showFileDetail(String fileId) {
        currentView = VIEW_FILE_DETAIL;
        currentFileId = fileId;
        filterSortBar.setVisibility(View.VISIBLE);
        filterMode = "all";
        sortMode = "reset_asc";
        spinnerFilter.setSelection(0);
        spinnerSort.setSelection(0);
        autoResetExpiredItems();
        refreshFileDetail();
        updateTitleAndAddressBar();
        updateBottomBar();
    }

    // ----- 更新地址栏 -----
    private void updateTitleAndAddressBar() {
        if (currentView == VIEW_MAIN) {
            if (currentGroupId == null) {
                tvTitle.setText("根目录");
            } else {
                Group g = findGroupById(parseGroups(loadData(this)), currentGroupId);
                tvTitle.setText(g != null ? g.name : "根目录");
            }
            buildBreadcrumb(currentGroupId);
        } else if (currentView == VIEW_FILE_DETAIL) {
            File f = findFileById(parseFiles(loadData(this)), currentFileId);
            tvTitle.setText(f != null ? f.name : "文件");
            String groupId = f != null ? f.groupId : null;
            buildBreadcrumbWithFile(groupId, f);
        }
    }

    private void buildBreadcrumb(String groupId) {
        addressBarLayout.removeAllViews();
        List<Group> allGroups = parseGroups(loadData(this));
        List<String> pathNames = new ArrayList<>();
        List<String> pathIds = new ArrayList<>();
        Group cur = findGroupById(allGroups, groupId);
        while (cur != null) {
            pathNames.add(0, cur.name);
            pathIds.add(0, cur.id);
            cur = findGroupById(allGroups, cur.parentId);
        }
        TextView root = createBreadcrumbItem("根目录", null);
        addressBarLayout.addView(root);
        for (int i = 0; i < pathNames.size(); i++) {
            TextView sep = new TextView(this);
            sep.setText(" > ");
            sep.setTextSize(15);
            sep.setTextColor(0xFF6B7280);
            addressBarLayout.addView(sep);
            final String targetId = pathIds.get(i);
            TextView seg = createBreadcrumbItem(pathNames.get(i), targetId);
            addressBarLayout.addView(seg);
        }
    }

    private void buildBreadcrumbWithFile(String groupId, File file) {
        addressBarLayout.removeAllViews();
        List<Group> allGroups = parseGroups(loadData(this));
        List<String> pathNames = new ArrayList<>();
        List<String> pathIds = new ArrayList<>();
        Group cur = findGroupById(allGroups, groupId);
        while (cur != null) {
            pathNames.add(0, cur.name);
            pathIds.add(0, cur.id);
            cur = findGroupById(allGroups, cur.parentId);
        }
        TextView root = createBreadcrumbItem("根目录", null);
        addressBarLayout.addView(root);
        for (int i = 0; i < pathNames.size(); i++) {
            TextView sep = new TextView(this);
            sep.setText(" > ");
            sep.setTextSize(15);
            sep.setTextColor(0xFF6B7280);
            addressBarLayout.addView(sep);
            final String targetId = pathIds.get(i);
            TextView seg = createBreadcrumbItem(pathNames.get(i), targetId);
            addressBarLayout.addView(seg);
        }
        if (file != null) {
            TextView sep = new TextView(this);
            sep.setText(" > ");
            sep.setTextSize(15);
            sep.setTextColor(0xFF6B7280);
            addressBarLayout.addView(sep);
            TextView fileSeg = new TextView(this);
            fileSeg.setText(file.name);
            fileSeg.setTextSize(15);
            fileSeg.setTextColor(0xFF1F2937);
            fileSeg.setPadding(dp(4), 0, dp(4), 0);
            addressBarLayout.addView(fileSeg);
        }
    }

    private TextView createBreadcrumbItem(String text, final String targetGroupId) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(15);
        tv.setTextColor(LIGHT_BLUE);
        tv.setPadding(dp(5), 0, dp(5), 0);
        tv.setClickable(true);
        tv.setOnClickListener(v -> {
            currentGroupId = targetGroupId;
            searchKeyword = null;
            showMainView();
        });
        return tv;
    }

    private void updateBottomBar() {
        if (currentView == VIEW_MAIN) {
            btnBack.setVisibility(currentGroupId != null ? View.VISIBLE : View.INVISIBLE);
            List<Group> groups = getChildGroups(currentGroupId);
            List<File> files = getFilesInGroup(currentGroupId);
            int total = groups.size() + files.size();
            tvItemCount.setText("共 " + total + " 个项目");
        } else if (currentView == VIEW_FILE_DETAIL) {
            btnBack.setVisibility(View.VISIBLE);
            List<TaskItem> items = getItemsInFile(currentFileId);
            tvItemCount.setText("共 " + items.size() + " 个清单项");
        }
    }

    // ----- 主列表 -----
    private void refreshMainList() {
        listContainer.removeAllViews();

        List<Group> groups = getChildGroups(currentGroupId);
        List<File> files = getFilesInGroup(currentGroupId);

        if (searchKeyword != null && !searchKeyword.isEmpty()) {
            String kw = searchKeyword.toLowerCase();
            groups.removeIf(g -> !g.name.toLowerCase().contains(kw));
            files.removeIf(f -> !f.name.toLowerCase().contains(kw));
        }

        if (groups.isEmpty() && files.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("当前文件夹为空");
            empty.setTextColor(0xFF9CA3AF);
            empty.setPadding(0, dp(24), 0, dp(24));
            empty.setGravity(Gravity.CENTER);
            empty.setTextSize(15);
            listContainer.addView(empty);
            return;
        }

        int total = groups.size() + files.size();
        int index = 0;
        for (Group g : groups) {
            LinearLayout item = createGroupItem(g);
            listContainer.addView(item);
            if (index < total - 1) {
                View divider = new View(this);
                divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
                divider.setBackgroundColor(0xFFE5E7EB);
                listContainer.addView(divider);
            }
            index++;
        }
        for (File f : files) {
            LinearLayout item = createFileItem(f);
            listContainer.addView(item);
            if (index < total - 1) {
                View divider = new View(this);
                divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
                divider.setBackgroundColor(0xFFE5E7EB);
                listContainer.addView(divider);
            }
            index++;
        }
    }

    private LinearLayout createGroupItem(final Group g) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(dp(18), dp(15), dp(18), dp(15));
        item.setClickable(true);
        item.setOnClickListener(v -> {
            currentGroupId = g.id;
            searchKeyword = null;
            showMainView();
        });

        TextView text = new TextView(this);
        text.setText("📁 " + g.name);
        text.setTextSize(16);
        text.setTextColor(0xFF1F2937);
        item.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView more = new TextView(this);
        more.setText("⋮");
        more.setTextSize(17);
        more.setTextColor(LIGHT_BLUE);
        int btnSize = dp(30);
        more.setLayoutParams(new LinearLayout.LayoutParams(btnSize, btnSize));
        more.setGravity(Gravity.CENTER);
        more.setBackground(null);
        more.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, more);
            popup.getMenu().add("重命名").setOnMenuItemClickListener(menuItem -> {
                showRenameGroupDialog(g);
                return true;
            });
            popup.getMenu().add("删除").setOnMenuItemClickListener(menuItem -> {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("删除文件夹")
                        .setMessage("确定要删除文件夹 \"" + g.name + "\" 及其所有内容吗？")
                        .setPositiveButton("确定", (dialog, which) -> {
                            deleteGroup(g.id);
                            showMainView();
                        })
                        .setNegativeButton("取消", null)
                        .show();
                return true;
            });
            popup.show();
        });
        item.addView(more);
        return item;
    }

    private LinearLayout createFileItem(final File f) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(dp(18), dp(15), dp(18), dp(15));
        item.setClickable(true);
        item.setOnClickListener(v -> showFileDetail(f.id));

        int[] progress = getFileProgress(f.id);
        int done = progress[0];
        int total = progress[1];
        String progressText = " (" + done + "/" + total + " 个任务)";

        TextView text = new TextView(this);
        text.setText("📄 " + f.name + progressText);
        text.setTextSize(16);
        if (total > 0 && done == total) {
            text.setTextColor(0xFF9CA3AF);
        } else {
            text.setTextColor(0xFF1F2937);
        }
        item.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView more = new TextView(this);
        more.setText("⋮");
        more.setTextSize(17);
        more.setTextColor(LIGHT_BLUE);
        int btnSize = dp(30);
        more.setLayoutParams(new LinearLayout.LayoutParams(btnSize, btnSize));
        more.setGravity(Gravity.CENTER);
        more.setBackground(null);
        more.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, more);
            popup.getMenu().add("重命名").setOnMenuItemClickListener(menuItem -> {
                showRenameFileDialog(f);
                return true;
            });
            popup.getMenu().add("删除").setOnMenuItemClickListener(menuItem -> {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("删除文件")
                        .setMessage("确定要删除文件 \"" + f.name + "\" 及其所有清单项吗？")
                        .setPositiveButton("确定", (dialog, which) -> {
                            deleteFileItem(f.id);
                            showMainView();
                        })
                        .setNegativeButton("取消", null)
                        .show();
                return true;
            });
            popup.show();
        });
        item.addView(more);
        return item;
    }

    private void showRenameGroupDialog(final Group g) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("重命名文件夹");
        final EditText input = new EditText(this);
        input.setText(g.name);
        input.setSelection(g.name.length());
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(12), dp(20), dp(12));
        layout.addView(input);
        builder.setView(layout);
        builder.setPositiveButton("确定", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(this, "名称不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            renameGroup(g.id, newName);
            showMainView();
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showRenameFileDialog(final File f) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("重命名文件");
        final EditText input = new EditText(this);
        input.setText(f.name);
        input.setSelection(f.name.length());
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(12), dp(20), dp(12));
        layout.addView(input);
        builder.setView(layout);
        builder.setPositiveButton("确定", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(this, "名称不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            renameFile(f.id, newName);
            showMainView();
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    // ----- 文件详情 -----
    private void refreshFileDetail() {
        listContainer.removeAllViews();

        autoResetExpiredItems();

        List<TaskItem> items = getItemsInFile(currentFileId);

        if ("completed".equals(filterMode)) {
            items.removeIf(t -> !t.completed);
        } else if ("uncompleted".equals(filterMode)) {
            items.removeIf(t -> t.completed);
        }

        if ("reset_asc".equals(sortMode)) {
            items.sort(Comparator.comparingLong(t -> t.lastClearTime));
        } else if ("reset_desc".equals(sortMode)) {
            items.sort((a, b) -> Long.compare(b.lastClearTime, a.lastClearTime));
        } else if ("remain_asc".equals(sortMode) || "remain_desc".equals(sortMode)) {
            items.sort((a, b) -> {
                long remainA = getRemainingMillis(a);
                long remainB = getRemainingMillis(b);
                if ("remain_asc".equals(sortMode)) {
                    return Long.compare(remainA, remainB);
                } else {
                    return Long.compare(remainB, remainA);
                }
            });
        }

        // 表头
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(dp(9), dp(7), dp(9), dp(7));
        header.setBackgroundColor(0xFFE8ECF0);
        header.setGravity(Gravity.CENTER_VERTICAL);

        View emptyView = new View(this);
        emptyView.setLayoutParams(new LinearLayout.LayoutParams(dp(40), ViewGroup.LayoutParams.MATCH_PARENT));
        header.addView(emptyView);

        TextView headerName = new TextView(this);
        headerName.setText("名称");
        headerName.setTextSize(13);
        headerName.setTypeface(null, android.graphics.Typeface.BOLD);
        headerName.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2));
        header.addView(headerName);

        TextView headerRemain = new TextView(this);
        headerRemain.setText("剩余时间");
        headerRemain.setTextSize(13);
        headerRemain.setTypeface(null, android.graphics.Typeface.BOLD);
        headerRemain.setGravity(Gravity.CENTER);
        headerRemain.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        header.addView(headerRemain);

        TextView headerPeriod = new TextView(this);
        headerPeriod.setText("周期");
        headerPeriod.setTextSize(13);
        headerPeriod.setTypeface(null, android.graphics.Typeface.BOLD);
        headerPeriod.setGravity(Gravity.CENTER);
        headerPeriod.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        header.addView(headerPeriod);

        TextView headerAction = new TextView(this);
        headerAction.setText("操作");
        headerAction.setTextSize(13);
        headerAction.setTypeface(null, android.graphics.Typeface.BOLD);
        headerAction.setGravity(Gravity.END);
        headerAction.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        header.addView(headerAction);

        listContainer.addView(header);

        if (items.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("此文件暂无清单项");
            empty.setTextColor(0xFF9CA3AF);
            empty.setPadding(0, dp(24), 0, dp(24));
            empty.setGravity(Gravity.CENTER);
            empty.setTextSize(14);
            listContainer.addView(empty);
        } else {
            for (int i = 0; i < items.size(); i++) {
                TaskItem t = items.get(i);
                LinearLayout itemView = createTaskItem(t);
                listContainer.addView(itemView);
                if (i < items.size() - 1) {
                    View divider = new View(this);
                    divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
                    divider.setBackgroundColor(0xFFE5E7EB);
                    listContainer.addView(divider);
                }
            }
        }
        updateBottomBar();
    }

    private LinearLayout createTaskItem(final TaskItem t) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(dp(14), dp(14), dp(14), dp(14));
        item.setBackground(null);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 0);
        item.setLayoutParams(params);

        CheckBox checkBox = new CheckBox(this);
        checkBox.setChecked(t.completed);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            toggleItemCompleted(t.id);
            refreshFileDetail();
        });
        LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(dp(40), ViewGroup.LayoutParams.WRAP_CONTENT);
        checkParams.gravity = Gravity.CENTER_VERTICAL;
        checkBox.setLayoutParams(checkParams);
        item.addView(checkBox);

        TextView nameView = new TextView(this);
        nameView.setText(t.title);
        nameView.setTextSize(15);
        if (t.completed) {
            nameView.setTextColor(0xFF9CA3AF);
        } else {
            nameView.setTextColor(0xFF1F2937);
        }
        nameView.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2);
        nameParams.gravity = Gravity.CENTER_VERTICAL;
        nameView.setLayoutParams(nameParams);
        item.addView(nameView);

        TextView remainView = new TextView(this);
        long remainMillis = getRemainingMillis(t);
        remainView.setText(formatRemaining(remainMillis));
        remainView.setTextSize(13);
        remainView.setTextColor(0xFF6B7280);
        remainView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams remainParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        remainParams.gravity = Gravity.CENTER_VERTICAL;
        remainView.setLayoutParams(remainParams);
        item.addView(remainView);

        TextView periodView = new TextView(this);
        periodView.setText(getPeriodDescription(t));
        periodView.setTextSize(13);
        periodView.setTextColor(0xFF6B7280);
        periodView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams periodParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        periodParams.gravity = Gravity.CENTER_VERTICAL;
        periodView.setLayoutParams(periodParams);
        item.addView(periodView);

        // 操作
        LinearLayout actionLayout = new LinearLayout(this);
        actionLayout.setOrientation(LinearLayout.HORIZONTAL);
        actionLayout.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        actionParams.gravity = Gravity.CENTER_VERTICAL;
        actionLayout.setLayoutParams(actionParams);

        TextView editBtn = new TextView(this);
        editBtn.setText("编辑");
        editBtn.setTextSize(13);
        editBtn.setTextColor(LIGHT_BLUE);
        editBtn.setPadding(dp(5), dp(3), dp(5), dp(3));
        editBtn.setBackground(null);
        editBtn.setOnClickListener(v -> showEditItemDialog(t));
        actionLayout.addView(editBtn);

        item.addView(actionLayout);
        return item;
    }

    // ----- 对话框工具 -----
    private void addFormView(View view, LinearLayout parent) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(14));
        view.setLayoutParams(params);
        parent.addView(view);
    }

    private void showEditItemDialog(final TaskItem t) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("编辑清单项");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(22), dp(14), dp(22), dp(14));

        final EditText etTitle = new EditText(this);
        etTitle.setText(t.title);
        etTitle.setHint("清单项名称");
        addFormView(etTitle, layout);

        final RadioGroup timerGroup = new RadioGroup(this);
        timerGroup.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton rbFixed = new RadioButton(this);
        rbFixed.setText("固定周期");
        rbFixed.setId(View.generateViewId());
        RadioButton rbAfter = new RadioButton(this);
        rbAfter.setText("完成后计时");
        rbAfter.setId(View.generateViewId());
        timerGroup.addView(rbFixed);
        timerGroup.addView(rbAfter);
        if (t.timerMode == 0) rbFixed.setChecked(true);
        else rbAfter.setChecked(true);
        addFormView(timerGroup, layout);

        final LinearLayout fixedLayout = new LinearLayout(this);
        fixedLayout.setOrientation(LinearLayout.VERTICAL);

        final Spinner spinnerPeriodType = new Spinner(this);
        String[] periodTypes = {"永不", "每天", "每周", "自定义天数"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, periodTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPeriodType.setAdapter(adapter);
        spinnerPeriodType.setSelection(t.periodType);
        addFormView(spinnerPeriodType, fixedLayout);

        final Spinner weekDaySpinner = new Spinner(this);
        String[] weekOptions = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        ArrayAdapter<String> weekAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, weekOptions);
        weekAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        weekDaySpinner.setAdapter(weekAdapter);
        weekDaySpinner.setSelection(t.weekDay - 1);
        weekDaySpinner.setVisibility(t.periodType == 2 ? View.VISIBLE : View.GONE);
        addFormView(weekDaySpinner, fixedLayout);

        final EditText etPeriodDays = new EditText(this);
        etPeriodDays.setHint("自定义天数");
        etPeriodDays.setInputType(InputType.TYPE_CLASS_NUMBER);
        etPeriodDays.setText(t.periodDays > 0 ? String.valueOf(t.periodDays) : "");
        etPeriodDays.setVisibility(t.periodType == 3 ? View.VISIBLE : View.GONE);
        addFormView(etPeriodDays, fixedLayout);

        LinearLayout timeLayout = new LinearLayout(this);
        timeLayout.setOrientation(LinearLayout.HORIZONTAL);
        timeLayout.setGravity(Gravity.CENTER_VERTICAL);

        final Spinner hourSpinner = new Spinner(this);
        String[] hours = new String[24];
        for (int i = 0; i < 24; i++) hours[i] = String.format("%02d", i);
        ArrayAdapter<String> hourAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, hours);
        hourAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        hourSpinner.setAdapter(hourAdapter);
        hourSpinner.setSelection(t.resetHour);
        timeLayout.addView(hourSpinner, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView colon = new TextView(this);
        colon.setText(" : ");
        colon.setTextSize(16);
        timeLayout.addView(colon);

        final Spinner minuteSpinner = new Spinner(this);
        String[] minutes = new String[60];
        for (int i = 0; i < 60; i++) minutes[i] = String.format("%02d", i);
        ArrayAdapter<String> minuteAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, minutes);
        minuteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        minuteSpinner.setAdapter(minuteAdapter);
        minuteSpinner.setSelection(t.resetMinute);
        timeLayout.addView(minuteSpinner, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        addFormView(timeLayout, fixedLayout);
        layout.addView(fixedLayout);

        final LinearLayout afterLayout = new LinearLayout(this);
        afterLayout.setOrientation(LinearLayout.VERTICAL);

        final Spinner afterSpinner = new Spinner(this);
        Integer[] afterOptions = {1, 2, 3, 4, 6, 8, 12, 18, 24, 36, 48, 72, 96, 120, 168, 240, 336, 720};
        String[] afterStrings = new String[afterOptions.length];
        for (int i = 0; i < afterOptions.length; i++) afterStrings[i] = afterOptions[i] + " 小时";
        ArrayAdapter<String> afterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, afterStrings);
        afterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        afterSpinner.setAdapter(afterAdapter);
        int selectedIndex = 0;
        for (int i = 0; i < afterOptions.length; i++) {
            if (afterOptions[i] == t.afterHours) { selectedIndex = i; break; }
        }
        afterSpinner.setSelection(selectedIndex);
        addFormView(afterSpinner, afterLayout);
        afterLayout.setVisibility(t.timerMode == 1 ? View.VISIBLE : View.GONE);
        layout.addView(afterLayout);

        final CheckBox cbCompleted = new CheckBox(this);
        cbCompleted.setText("已完成");
        cbCompleted.setChecked(t.completed);
        addFormView(cbCompleted, layout);

        timerGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == rbFixed.getId()) {
                fixedLayout.setVisibility(View.VISIBLE);
                afterLayout.setVisibility(View.GONE);
            } else {
                fixedLayout.setVisibility(View.GONE);
                afterLayout.setVisibility(View.VISIBLE);
            }
        });

        spinnerPeriodType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                etPeriodDays.setVisibility(position == 3 ? View.VISIBLE : View.GONE);
                weekDaySpinner.setVisibility(position == 2 ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        builder.setView(layout);

        builder.setPositiveButton("保存", (dialog, which) -> {
            String title = etTitle.getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(this, "名称不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            int timerMode = (timerGroup.getCheckedRadioButtonId() == rbFixed.getId()) ? 0 : 1;
            int periodType = 0, periodDays = 0, weekDay = 1, resetHour = 0, resetMinute = 0, afterHours = 0;
            if (timerMode == 0) {
                periodType = spinnerPeriodType.getSelectedItemPosition();
                if (periodType == 2) {
                    weekDay = weekDaySpinner.getSelectedItemPosition() + 1;
                }
                if (periodType == 3) {
                    try {
                        periodDays = Integer.parseInt(etPeriodDays.getText().toString().trim());
                        if (periodDays <= 0) {
                            Toast.makeText(this, "自定义天数必须大于0", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "请输入有效天数", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                resetHour = hourSpinner.getSelectedItemPosition();
                resetMinute = minuteSpinner.getSelectedItemPosition();
            } else {
                int pos = afterSpinner.getSelectedItemPosition();
                afterHours = afterOptions[pos];
                if (afterHours <= 0) {
                    Toast.makeText(this, "请选择有效小时数", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            boolean completed = cbCompleted.isChecked();
            updateItem(t.id, title, timerMode, periodType, periodDays, weekDay, resetHour, resetMinute, afterHours, completed);
            refreshFileDetail();
        });

        builder.setNegativeButton("取消", null);

        builder.setNeutralButton("删除", (dialog, which) -> {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("删除清单项")
                    .setMessage("确定要删除 \"" + t.title + "\" 吗？")
                    .setPositiveButton("确定", (delDialog, delWhich) -> {
                        deleteItem(t.id);
                        refreshFileDetail();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        builder.show();
    }

    // 新建清单项对话框
    private void showNewItemDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("新建清单项");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(22), dp(14), dp(22), dp(14));

        final EditText etTitle = new EditText(this);
        etTitle.setHint("清单项名称");
        addFormView(etTitle, layout);

        final RadioGroup timerGroup = new RadioGroup(this);
        timerGroup.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton rbFixed = new RadioButton(this);
        rbFixed.setText("固定周期");
        rbFixed.setId(View.generateViewId());
        RadioButton rbAfter = new RadioButton(this);
        rbAfter.setText("完成后计时");
        rbAfter.setId(View.generateViewId());
        timerGroup.addView(rbFixed);
        timerGroup.addView(rbAfter);
        rbFixed.setChecked(true);
        addFormView(timerGroup, layout);

        final LinearLayout fixedLayout = new LinearLayout(this);
        fixedLayout.setOrientation(LinearLayout.VERTICAL);

        final Spinner spinnerPeriodType = new Spinner(this);
        String[] periodTypes = {"永不", "每天", "每周", "自定义天数"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, periodTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPeriodType.setAdapter(adapter);
        spinnerPeriodType.setSelection(0);
        addFormView(spinnerPeriodType, fixedLayout);

        final Spinner weekDaySpinner = new Spinner(this);
        String[] weekOptions = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        ArrayAdapter<String> weekAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, weekOptions);
        weekAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        weekDaySpinner.setAdapter(weekAdapter);
        weekDaySpinner.setVisibility(View.GONE);
        addFormView(weekDaySpinner, fixedLayout);

        final EditText etPeriodDays = new EditText(this);
        etPeriodDays.setHint("自定义天数");
        etPeriodDays.setInputType(InputType.TYPE_CLASS_NUMBER);
        etPeriodDays.setVisibility(View.GONE);
        addFormView(etPeriodDays, fixedLayout);

        LinearLayout timeLayout = new LinearLayout(this);
        timeLayout.setOrientation(LinearLayout.HORIZONTAL);
        timeLayout.setGravity(Gravity.CENTER_VERTICAL);

        final Spinner hourSpinner = new Spinner(this);
        String[] hours = new String[24];
        for (int i = 0; i < 24; i++) hours[i] = String.format("%02d", i);
        ArrayAdapter<String> hourAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, hours);
        hourAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        hourSpinner.setAdapter(hourAdapter);
        hourSpinner.setSelection(0);
        timeLayout.addView(hourSpinner, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView colon = new TextView(this);
        colon.setText(" : ");
        colon.setTextSize(16);
        timeLayout.addView(colon);

        final Spinner minuteSpinner = new Spinner(this);
        String[] minutes = new String[60];
        for (int i = 0; i < 60; i++) minutes[i] = String.format("%02d", i);
        ArrayAdapter<String> minuteAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, minutes);
        minuteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        minuteSpinner.setAdapter(minuteAdapter);
        minuteSpinner.setSelection(0);
        timeLayout.addView(minuteSpinner, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        addFormView(timeLayout, fixedLayout);
        layout.addView(fixedLayout);

        final LinearLayout afterLayout = new LinearLayout(this);
        afterLayout.setOrientation(LinearLayout.VERTICAL);

        final Spinner afterSpinner = new Spinner(this);
        Integer[] afterOptions = {1, 2, 3, 4, 6, 8, 12, 18, 24, 36, 48, 72, 96, 120, 168, 240, 336, 720};
        String[] afterStrings = new String[afterOptions.length];
        for (int i = 0; i < afterOptions.length; i++) afterStrings[i] = afterOptions[i] + " 小时";
        ArrayAdapter<String> afterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, afterStrings);
        afterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        afterSpinner.setAdapter(afterAdapter);
        afterSpinner.setSelection(5);
        addFormView(afterSpinner, afterLayout);
        afterLayout.setVisibility(View.GONE);
        layout.addView(afterLayout);

        final CheckBox cbCompleted = new CheckBox(this);
        cbCompleted.setText("已完成");
        cbCompleted.setChecked(false);
        addFormView(cbCompleted, layout);

        timerGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == rbFixed.getId()) {
                fixedLayout.setVisibility(View.VISIBLE);
                afterLayout.setVisibility(View.GONE);
            } else {
                fixedLayout.setVisibility(View.GONE);
                afterLayout.setVisibility(View.VISIBLE);
            }
        });

        spinnerPeriodType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                etPeriodDays.setVisibility(position == 3 ? View.VISIBLE : View.GONE);
                weekDaySpinner.setVisibility(position == 2 ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        builder.setView(layout);
        builder.setPositiveButton("确定", (dialog, which) -> {
            String title = etTitle.getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(this, "名称不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            int timerMode = (timerGroup.getCheckedRadioButtonId() == rbFixed.getId()) ? 0 : 1;
            int periodType = 0, periodDays = 0, weekDay = 1, resetHour = 0, resetMinute = 0, afterHours = 0;
            if (timerMode == 0) {
                periodType = spinnerPeriodType.getSelectedItemPosition();
                if (periodType == 2) {
                    weekDay = weekDaySpinner.getSelectedItemPosition() + 1;
                }
                if (periodType == 3) {
                    try {
                        periodDays = Integer.parseInt(etPeriodDays.getText().toString().trim());
                        if (periodDays <= 0) {
                            Toast.makeText(this, "自定义天数必须大于0", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "请输入有效天数", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                resetHour = hourSpinner.getSelectedItemPosition();
                resetMinute = minuteSpinner.getSelectedItemPosition();
            } else {
                int pos = afterSpinner.getSelectedItemPosition();
                afterHours = afterOptions[pos];
                if (afterHours <= 0) {
                    Toast.makeText(this, "请选择有效小时数", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            boolean completed = cbCompleted.isChecked();
            addItem(title, timerMode, periodType, periodDays, weekDay,
                    resetHour, resetMinute, afterHours, currentFileId, completed);
            refreshFileDetail();
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    // 新建文件夹/文件
    private void showNewGroupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("新建文件夹");
        final EditText input = new EditText(this);
        input.setHint("输入文件夹名称");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(22), dp(14), dp(22), dp(14));
        addFormView(input, layout);
        builder.setView(layout);
        builder.setPositiveButton("确定", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "名称不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            addGroup(name, currentGroupId);
            showMainView();
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showNewFileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("新建文件");
        final EditText input = new EditText(this);
        input.setHint("输入文件名称");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(22), dp(14), dp(22), dp(14));
        addFormView(input, layout);
        builder.setView(layout);
        builder.setPositiveButton("确定", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "名称不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            addFile(name, currentGroupId);
            showMainView();
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showNewPopupMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        if (currentView == VIEW_MAIN) {
            popup.getMenu().add("新建文件夹").setOnMenuItemClickListener(item -> {
                showNewGroupDialog();
                return true;
            });
            popup.getMenu().add("新建文件").setOnMenuItemClickListener(item -> {
                showNewFileDialog();
                return true;
            });
        } else if (currentView == VIEW_FILE_DETAIL) {
            popup.getMenu().add("新建清单项").setOnMenuItemClickListener(item -> {
                showNewItemDialog();
                return true;
            });
        }
        popup.show();
    }

    @Override
    public void onBackPressed() {
        if (currentView == VIEW_FILE_DETAIL) {
            showMainView();
        } else if (currentGroupId != null) {
            List<Group> all = parseGroups(loadData(this));
            Group cur = findGroupById(all, currentGroupId);
            if (cur != null) {
                currentGroupId = cur.parentId;
            } else {
                currentGroupId = null;
            }
            searchKeyword = null;
            showMainView();
        } else {
            super.onBackPressed();
        }
    }

    // ----- 工具方法 -----
    private int dp(int px) {
        return (int) (px * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable createRoundedBackground(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentView == VIEW_MAIN) {
            showMainView();
        } else {
            refreshFileDetail();
            updateTitleAndAddressBar();
            updateBottomBar();
        }
    }
}