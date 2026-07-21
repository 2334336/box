package com.github.tvbox.osc.bbox.api;

import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;

import com.github.tvbox.osc.bbox.constant.URL;
import com.github.tvbox.osc.bbox.util.HawkConfig;
import com.github.tvbox.osc.bbox.util.LOG;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;
import com.orhanobut.hawk.Hawk;

import java.util.ArrayList;
import java.util.HashMap;

public class StoreApiConfig {
    private static volatile StoreApiConfig instance;

    public static StoreApiConfig get() {
        if (instance == null) {
            synchronized (StoreApiConfig.class) {
                if (instance == null) {
                    instance = new StoreApiConfig();
                }
            }
        }
        return instance;
    }

    public void doGet(String url, StoreApiConfigCallback callback) {
        LOG.i("request url : " + url);
        if (TextUtils.isEmpty(url)) {
            callback.error("地址为空，无法请求");
            return;
        }
        OkGo.<String>get(url)
                .headers("User-Agent", "okhttp/3.15")
                .headers("Accept", "text/html," + "application/xhtml+xml,application/xml;q=0.9,image/avif," + "image/webp,image/apng," + "*/*;q=0.8,application/signed-exchange;v=b3;" + "q=0.9")
                .execute(new StringCallback() {
                    @Override
                    public void onError(Response<String> response) {
                        callback.error("请求失败，没有获取到数据");
                    }

                    @Override
                    public void onSuccess(Response<String> response) {
                        String body = response.body();
                        if (TextUtils.isEmpty(body)) {
                            callback.error("请求成功但内容为空");
                            return;
                        }
                        callback.success(body);
                    }
                });
    }

    public void Subscribe(Context context) {
        Subscribe(context, null);
    }

    /**
     * @param statusCallback 可选；有则把过程状态回传给 UI（对话框状态栏），否则仍用 Toast
     */
    public void Subscribe(Context context, StatusCallback statusCallback) {
        notifyStatus(context, statusCallback, "正在获取订阅…", false);

        HashMap<String, String> storeMap = Hawk.get(HawkConfig.STORE_API_MAP, new HashMap<>());
        ArrayList<String> storeNameHistory = Hawk.get(HawkConfig.STORE_API_NAME_HISTORY, new ArrayList<>());

        String currentName = Hawk.get(HawkConfig.STORE_API_NAME, "");
        String storeUrl;
        if (storeMap.isEmpty()) {
            storeUrl = URL.DEFAULT_STORE_API_URL;
        } else if (storeMap.containsKey(currentName)) {
            storeUrl = storeMap.get(currentName);
        } else if (storeMap.containsValue(currentName)) {
            // 历史里可能用 URL 当 name
            storeUrl = currentName;
        } else {
            storeUrl = currentName;
        }

        if (TextUtils.isEmpty(storeUrl)) {
            notifyFinish(context, statusCallback, false, "订阅地址为空，请重新输入");
            return;
        }

        LOG.i("订阅仓库地址：" + storeUrl);
        notifyStatus(context, statusCallback, "正在请求：\n" + storeUrl, false);

        StoreApiConfig.get().doGet(storeUrl, new StoreApiConfigCallback() {
            @Override
            public void success(String sourceJson) {
                try {
                    JsonObject json = new Gson().fromJson(sourceJson.trim(), JsonObject.class);
                    if (json == null) {
                        notifyFinish(context, statusCallback, false, "解析失败：返回内容不是有效 JSON");
                        return;
                    }

                    if (json.has("urls") && json.get("urls") != null && !json.get("urls").isJsonNull()) {
                        // 多线路（单仓 urls）
                        String result = MutiUrl(sourceJson);
                        boolean ok = !result.contains("失败");
                        String currentStoreName = Hawk.get(HawkConfig.STORE_API_NAME, "");
                        if (!currentStoreName.isEmpty() && !storeNameHistory.contains(currentStoreName)) {
                            storeNameHistory.add(0, currentStoreName);
                        }
                        Hawk.put(HawkConfig.STORE_API_MAP, storeMap);
                        Hawk.put(HawkConfig.STORE_API_NAME_HISTORY, storeNameHistory);
                        notifyFinish(context, statusCallback, ok, "多线路订阅完成\n" + result + "\n可到「配置历史」切换线路");
                        return;
                    }

                    if (!json.has("storeHouse") || json.get("storeHouse") == null || json.get("storeHouse").isJsonNull()) {
                        notifyFinish(context, statusCallback, false,
                                "解析失败：既没有 urls 也没有 storeHouse\n请确认地址是多源/多仓配置");
                        return;
                    }

                    JsonArray storeHouses = json.get("storeHouse").getAsJsonArray();
                    if (storeHouses.size() == 0) {
                        notifyFinish(context, statusCallback, false, "多仓列表为空");
                        return;
                    }

                    JsonObject defStoreHouse = storeHouses.get(0).getAsJsonObject();
                    for (int i = 0; i < storeHouses.size(); i++) {
                        JsonObject storeHouse = storeHouses.get(i).getAsJsonObject();
                        String sourceName = storeHouse.get("sourceName").getAsString();
                        String sourceUrl = storeHouse.get("sourceUrl").getAsString();
                        if (!storeMap.containsValue(sourceUrl)) {
                            storeMap.put(sourceName, sourceUrl);
                            if (!storeNameHistory.contains(sourceName)) {
                                storeNameHistory.add(sourceName);
                            }
                        }
                    }

                    String name = defStoreHouse.get("sourceName").getAsString();
                    String url = defStoreHouse.get("sourceUrl").getAsString();
                    Hawk.put(HawkConfig.STORE_API, url);
                    Hawk.put(HawkConfig.STORE_API_NAME, name);
                    Hawk.put(HawkConfig.STORE_API_MAP, storeMap);
                    Hawk.put(HawkConfig.STORE_API_NAME_HISTORY, storeNameHistory);

                    notifyStatus(context, statusCallback,
                            "多仓共 " + storeHouses.size() + " 个，正在加载默认仓：\n" + name, false);

                    StoreApiConfig.get().doGet(url, new StoreApiConfigCallback() {
                        @Override
                        public void success(String urlsJson) {
                            String result = MutiUrl(urlsJson);
                            boolean ok = !result.contains("失败");
                            notifyFinish(context, statusCallback, ok,
                                    "多仓订阅完成\n默认仓：" + name + "\n" + result + "\n可到「多源历史」切换仓库");
                        }

                        @Override
                        public void error(String msg) {
                            notifyFinish(context, statusCallback, false,
                                    "多仓列表已保存，但默认仓加载失败：\n" + msg);
                        }
                    });
                } catch (Exception e) {
                    LOG.e("store subscribe parse error: " + e.getMessage());
                    String preview = sourceJson == null ? "" : sourceJson.trim();
                    if (preview.length() > 80) {
                        preview = preview.substring(0, 80) + "…";
                    }
                    notifyFinish(context, statusCallback, false,
                            "解析异常：" + e.getMessage() + "\n内容预览：" + preview);
                }
            }

            @Override
            public void error(String msg) {
                notifyFinish(context, statusCallback, false, msg);
            }
        });
    }

    private void notifyStatus(Context context, StatusCallback cb, String msg, boolean finish) {
        if (cb != null) {
            cb.onStatus(msg);
            return;
        }
        if (!finish) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
        }
    }

    private void notifyFinish(Context context, StatusCallback cb, boolean success, String msg) {
        if (cb != null) {
            cb.onFinish(success, msg);
            return;
        }
        Toast.makeText(context, msg, success ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
    }

    private String MutiUrl(String urlsJson) {
        ArrayList<String> history = new ArrayList<>();
        HashMap<String, String> map = new HashMap<>();

        String apiName = Hawk.get(HawkConfig.API_NAME, "");
        String apiUrl = Hawk.get(HawkConfig.API_URL, "");

        if (!apiName.isEmpty()) {
            history.add(apiName);
            map.put(apiName, apiUrl);
        }

        try {
            JsonObject urlsObject = new Gson().fromJson(urlsJson.trim(), JsonObject.class);
            if (urlsObject == null || urlsObject.get("urls") == null || urlsObject.get("urls").isJsonNull()) {
                return "订阅出错，失败：无 urls 字段";
            }

            JsonArray urlsObjects = urlsObject.get("urls").getAsJsonArray();
            for (JsonElement element : urlsObjects) {
                JsonObject obj = element.getAsJsonObject();
                String name = obj.get("name").getAsString();
                String url = obj.get("url").getAsString();
                if (!map.containsValue(url)) {
                    history.add(name);
                    map.put(name, url);
                }
            }
            Hawk.put(HawkConfig.API_NAME_HISTORY, history);
            Hawk.put(HawkConfig.API_MAP, map);
            return "共 " + history.size() + " 条线路";
        } catch (Exception e) {
            LOG.e("MutiUrl parse error: " + e.getMessage());
            return "订阅出错，失败：" + e.getMessage();
        }
    }

    public interface StoreApiConfigCallback {
        void success(String json);

        void error(String msg);
    }

    public interface StatusCallback {
        void onStatus(String msg);

        void onFinish(boolean success, String msg);
    }
}
