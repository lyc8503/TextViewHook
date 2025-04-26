package me.textviewhook

import android.widget.TextView
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class MainHook : IXposedHookLoadPackage {
    fun doReplace(charSeq: CharSequence): CharSequence {
        return charSeq.replace(Regex("lyc8503"), "lyc8504")
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == "me.textviewhook") return

        val methodHook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (param.args[0] == null) return

                XposedBridge.log("TextView setText arg: ${param.args[0]} Class: ${param.args[0].javaClass.name}")

                // Modify the text before it is set
                val originalText = param.args[0] as CharSequence
                param.args[0] = doReplace(originalText)
//                val modifiedText = "Hooked: $originalText"
//                param.args[0] = modifiedText
            }
        }

        // setTextMethod calls setTextMethodWithType, so we just need to hook the latter
        // val setTextMethod = XposedHelpers.findMethodExact(TextView::class.java, "setText", CharSequence::class.java)
        val setTextMethodWithType = XposedHelpers.findMethodExact(TextView::class.java, "setText", CharSequence::class.java, TextView.BufferType::class.java)

        XposedBridge.log("Hooking setText method of TextView in package: ${lpparam.packageName}, method: $setTextMethodWithType")
        XposedBridge.hookMethod(setTextMethodWithType, methodHook)
    }
}