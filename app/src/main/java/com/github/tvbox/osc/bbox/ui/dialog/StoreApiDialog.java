package com.github.tvbox.osc.bbox.ui.dialog;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.bbox.R;
import com.github.tvbox.osc.bbox.api.StoreApiConfig;
import com.github.tvbox.osc.bbox.event.RefreshEvent;
import com.github.tvbox.osc.bbox.server.ControlManager;
import com.github.tvbox.osc.bbox.ui.tv.QRCodeGen;
import com.github.tvbox.osc.bbox.util.HawkConfig;
import com.orhanobut.hawk.Hawk;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;

import me.jessyan.autosize.utils.AutoSizeUtils;

/**
 * 多源/多仓配置对话框。
 * 确定后在状态栏显示请求进度与结果，不再立刻关闭导致“空白无反馈”。
 */
public class StoreApiDialog extends BaseDialog {
    private final ImageView ivQRCode;
    private final TextView tvAddress;
    private final TextView tvStatus;
    private final TextView inputSubmit;
    private EditText inputStoreApiUrl;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean subscribing = false;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {
        if (event.type == RefreshEvent.TYPE_STORE_CONFIG_CHANGE) {
            inputStoreApiUrl.setText((String) event.obj);
            setStatus("已通过扫码/远程填入地址，请点确定订阅", false);
        }
    }

    public StoreApiDialog(@NonNull @NotNull Context context) {
        super(context);
        setContentView(R.layout.dialog_store_api);
        setCanceledOnTouchOutside(false);
        ivQRCode = findViewById(R.id.ivQRCode);
        tvAddress = findViewById(R.id.tvAddress);
        tvStatus = findViewById(R.id.tvStoreApiStatus);
        inputSubmit = findViewById(R.id.inputSubmit);
        inputStoreApiUrl = findViewById(R.id.inputStoreApiUrl);

        String storeApiName = Hawk.get(HawkConfig.STORE_API_NAME, "");
        HashMap<String, String> map = Hawk.get(HawkConfig.STORE_API_MAP, new HashMap<>());
        if (map.containsKey(storeApiName)) {
            inputStoreApiUrl.setText(map.get(storeApiName));
            setStatus("当前已保存：\n" + shortText(storeApiName) + "\n修改后点确定重新订阅", false);
        } else if (!TextUtils.isEmpty(storeApiName)) {
            inputStoreApiUrl.setText(storeApiName);
            setStatus("当前地址：\n" + shortText(storeApiName) + "\n点确定开始订阅", false);
        } else {
            setStatus("状态：等待输入地址后点确定", false);
        }

        inputSubmit.setOnClickListener(v -> {
            if (subscribing) {
                return;
            }
            String url = inputStoreApiUrl.getText().toString().trim();
            if (TextUtils.isEmpty(url)) {
                setStatus("错误：地址不能为空", true);
                return;
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")
                    && !url.startsWith("clan://") && !url.startsWith("file://")) {
                setStatus("提示：地址通常以 http/https 开头，仍将尝试订阅…", false);
            }

            // 用 URL 作为名称键，保证历史可回读
            if (!map.containsValue(url)) {
                map.put(url, url);
            } else {
                // 已存在 value 时，确保 key 也能用 url 取到
                boolean hasKey = false;
                for (String k : map.keySet()) {
                    if (url.equals(map.get(k))) {
                        hasKey = true;
                        break;
                    }
                }
                if (!hasKey) {
                    map.put(url, url);
                }
            }

            ArrayList<String> nameHistory = Hawk.get(HawkConfig.STORE_API_NAME_HISTORY, new ArrayList<>());
            if (!nameHistory.contains(url)) {
                nameHistory.add(0, url);
            }
            Hawk.put(HawkConfig.STORE_API_MAP, map);
            Hawk.put(HawkConfig.STORE_API_NAME, url);
            Hawk.put(HawkConfig.STORE_API_NAME_HISTORY, nameHistory);

            if (listener != null) {
                listener.onchange(url);
            }

            subscribing = true;
            inputSubmit.setEnabled(false);
            inputSubmit.setAlpha(0.5f);
            inputStoreApiUrl.setEnabled(false);
            setStatus("状态：正在订阅，请稍候…\n" + shortText(url), false);

            try {
                StoreApiConfig.get().Subscribe(getContext(), new StoreApiConfig.StatusCallback() {
                    @Override
                    public void onStatus(String msg) {
                        setStatus(msg, false);
                    }

                    @Override
                    public void onFinish(boolean success, String msg) {
                        subscribing = false;
                        inputSubmit.setEnabled(true);
                        inputSubmit.setAlpha(1f);
                        inputStoreApiUrl.setEnabled(true);
                        setStatus((success ? "成功：\n" : "失败：\n") + msg, !success);
                        if (success) {
                            // 成功时停留片刻再关，方便看清结果
                            mainHandler.postDelayed(() -> {
                                if (isShowing()) {
                                    dismiss();
                                }
                            }, 1800);
                        }
                    }
                });
            } catch (Exception e) {
                subscribing = false;
                inputSubmit.setEnabled(true);
                inputSubmit.setAlpha(1f);
                inputStoreApiUrl.setEnabled(true);
                setStatus("失败：" + e.getMessage(), true);
            }
        });

        refreshQRCode();
    }

    private void setStatus(String msg, boolean error) {
        if (tvStatus == null) {
            return;
        }
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(msg);
        // 失败用更深背景提示，成功/过程保持浅底
        tvStatus.setBackgroundColor(error ? 0x55FF5252 : 0x33FFFFFF);
        tvStatus.setTextColor(0xCC000000);
    }

    private String shortText(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 90 ? s.substring(0, 90) + "…" : s;
    }

    private void refreshQRCode() {
        String address = ControlManager.get().getAddress(false);
        tvAddress.setText(String.format("手机/电脑扫描二维码或浏览器访问\n%s", address));
        ivQRCode.setImageBitmap(QRCodeGen.generateBitmap(address,
                AutoSizeUtils.mm2px(getContext(), 300),
                AutoSizeUtils.mm2px(getContext(), 300)));
    }

    @Override
    public void dismiss() {
        mainHandler.removeCallbacksAndMessages(null);
        super.dismiss();
    }

    public void setOnListener(OnListener listener) {
        this.listener = listener;
    }

    OnListener listener = null;

    public interface OnListener {
        void onchange(String data);
    }
}
