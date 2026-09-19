package com.kimi.fakemobiledata;

import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {

    private static final String MY_PKG = "com.kimi.fakemobiledata";
    private static final String PREF = "fakesim";

    private static long lastLoad;
    private static XSharedPreferences prefs;

    private static XSharedPreferences prefs() {
        if (System.currentTimeMillis() - lastLoad > 1000) {
            lastLoad = System.currentTimeMillis();
            prefs = new XSharedPreferences(MY_PKG, PREF);
            prefs.makeWorldReadable();
            prefs.reload();
        }
        return prefs;
    }

    private static boolean on()        { return prefs().getBoolean("enabled", true); }
    private static String  opName()    { return prefs().getString("name", "中国电信"); }
    private static int     netType()   { return prefs().getInt("type", 20); }
    private static int     level()     { return prefs().getInt("level", 4); }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        if (!"com.android.systemui".equals(lpparam.packageName)) return;

        // ============ 1. TelephonyManager：让系统以为有 SIM 卡 ============
        XposedBridge.hookAllMethods(TelephonyManager.class, "getSimState", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (on()) param.setResult(TelephonyManager.SIM_STATE_READY);
            }
        });

        // 运营商名称
        XC_MethodHook nameHook = new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                String n = opName();
                if (on() && n != null && !n.isEmpty()) param.setResult(n);
            }
        };
        XposedBridge.hookAllMethods(TelephonyManager.class, "getNetworkOperatorName", nameHook);
        XposedBridge.hookAllMethods(TelephonyManager.class, "getSimOperatorName", nameHook);

        // 网络类型（5G/4G/3G/2G）
        XC_MethodHook typeHook = new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (on()) param.setResult(netType());
            }
        };
        String[] typeMethods = {"getDataNetworkType", "getNetworkType", "getVoiceNetworkType",
                "getDataNetworkTypeName", "getNetworkTypeName"};
        for (String m : typeMethods) {
            try {
                XposedBridge.hookAllMethods(TelephonyManager.class, m, typeHook);
            } catch (Throwable ignored) {
            }
        }

        // ============ 2. ServiceState：注册状态=在网、注册网络类型 ============
        try {
            XposedBridge.hookAllMethods(ServiceState.class, "getState", new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    if (on()) param.setResult(ServiceState.STATE_IN_SERVICE);
                }
            });
            XposedBridge.hookAllMethods(ServiceState.class, "getDataRegistrationState", new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    if (on()) param.setResult(ServiceState.STATE_IN_SERVICE);
                }
            });
            for (String m : new String[]{"getDataNetworkType", "getVoiceNetworkType"}) {
                try {
                    XposedBridge.hookAllMethods(ServiceState.class, m, typeHook);
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }

        // ============ 3. SignalStrength：信号满格 ============
        try {
            XposedBridge.hookAllMethods(SignalStrength.class, "getLevel", new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    if (on()) param.setResult(level());
                }
            });
            XposedBridge.hookAllMethods(SignalStrength.class, "getDbm", new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    if (on()) param.setResult(-65);
                }
            });
        } catch (Throwable ignored) {
        }

        // ============ 4. SubscriptionManager：没卡也假装有订阅（关键） ============
        try {
            XposedBridge.hookAllMethods(SubscriptionManager.class, "getActiveSubscriptionInfoCount",
                    new XC_MethodHook() {
                        @Override protected void afterHookedMethod(MethodHookParam param) {
                            if (on() && (Integer) param.getResult() == 0) param.setResult(1);
                        }
                    });
            XposedBridge.hookAllMethods(SubscriptionManager.class, "getCompleteActiveSubscriptionInfoList",
                    new XC_MethodHook() {
                        @Override protected void afterHookedMethod(MethodHookParam param) {
                            // 不伪造列表本身，避免崩溃；仅保证计数不为 0
                        }
                    });
        } catch (Throwable ignored) {
        }

        // ============ 5. SystemUI 内部状态：有服务 / 有数据连接 ============
        XC_MethodHook trueHook = new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (on()) param.setResult(Boolean.TRUE);
            }
        };
        String[] controllerClasses = {
                "com.android.systemui.statusbar.policy.MobileSignalController",
                "com.android.systemui.statusbar.policy.MiuiMobileSignalController",
                "com.android.systemui.statusbar.policy.NetworkControllerImpl",
                "com.android.systemui.statusbar.policy.MiuiNetworkControllerImpl"
        };
        String[] boolMethods = {
                "hasService", "hasDataConnection", "isDataConnected",
                "hasMobileData", "hasVoiceCallingFeature", "isEmergencyOnly", "shouldShowMobileData"
        };
        for (String cls : controllerClasses) {
            try {
                Class<?> c = XposedHelpers.findClassIfExists(cls, lpparam.classLoader);
                if (c == null) continue;
                for (String m : boolMethods) {
                    try {
                        XposedBridge.hookAllMethods(c, m, trueHook);
                    } catch (Throwable ignored) {
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
