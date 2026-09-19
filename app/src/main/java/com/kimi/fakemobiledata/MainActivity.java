package com.kimi.fakemobiledata;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    public static final String PREF = "fakesim";
    private static final String[] LABELS = {"5G (NR)", "4G (LTE)", "3G (UMTS)", "2G (EDGE)"};
    private static final int[] VALUES = {20, 13, 3, 2};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Switch swEnable = findViewById(R.id.sw_enable);
        EditText etName = findViewById(R.id.et_name);
        Spinner spType = findViewById(R.id.sp_type);
        SeekBar sbLevel = findViewById(R.id.sb_level);
        TextView tvLevel = findViewById(R.id.tv_level);

        spType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, LABELS));

        final SharedPreferences p = getSharedPreferences(PREF, MODE_PRIVATE);
        swEnable.setChecked(p.getBoolean("enabled", true));
        etName.setText(p.getString("name", "中国电信"));
        int savedType = p.getInt("type", 20);
        int idx = 0;
        for (int i = 0; i < VALUES.length; i++) {
            if (VALUES[i] == savedType) idx = i;
        }
        spType.setSelection(idx);
        int savedLevel = p.getInt("level", 4);
        sbLevel.setMax(4);
        sbLevel.setProgress(savedLevel);
        tvLevel.setText(savedLevel + "/4");

        sbLevel.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvLevel.setText(progress + "/4");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        findViewById(R.id.btn_save).setOnClickListener(v -> {
            p.edit()
                    .putBoolean("enabled", swEnable.isChecked())
                    .putString("name", etName.getText().toString())
                    .putInt("type", VALUES[spType.getSelectedItemPosition()])
                    .putInt("level", sbLevel.getProgress())
                    .apply();
            // 放开配置文件读取权限，让 SystemUI 里的 XSharedPreferences 能读到
            try {
                Runtime.getRuntime().exec(new String[]{"su", "-c",
                        "chmod 644 /data/user/0/" + getPackageName() + "/shared_prefs/" + PREF + ".xml"});
            } catch (Throwable ignored) {
            }
            Toast.makeText(this, "已保存，请点下方按钮重启系统界面", Toast.LENGTH_LONG).show();
        });

        findViewById(R.id.btn_kill).setOnClickListener(v -> {
            try {
                Runtime.getRuntime().exec(new String[]{"su", "-c", "killall com.android.systemui"});
                Toast.makeText(this, "系统界面已重启", Toast.LENGTH_SHORT).show();
            } catch (Throwable t) {
                Toast.makeText(this, "没有 Root，请手动重启手机", Toast.LENGTH_LONG).show();
            }
        });
    }
}
