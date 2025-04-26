package me.textviewhook

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.widget.TextView
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import org.w3c.dom.Text

class MainHook : IXposedHookLoadPackage {
    fun doSpannedReplace(original: Spanned, oldStr: String, newStr: String): SpannableStringBuilder {
        data class SpanInfo(
            val span: Any,
            var start: Int,
            var end: Int,
            val flags: Int
        )

        val spanList = mutableListOf<SpanInfo>()
        original.getSpans(0, original.length, Any::class.java).forEach {
            val start = original.getSpanStart(it)
            val end = original.getSpanEnd(it)
            val flags = original.getSpanFlags(it)
            spanList.add(SpanInfo(it, start, end, flags))
            XposedBridge.log("SpanInfo: $it $start $end $flags Class: ${it.javaClass.name}")
        }

        var modifiedText = original.toString()
        outer@ for (current in original.indices) {
            // Match text to be replaced
            XposedBridge.log("DEBUG current $current")
            if (current + oldStr.length <= original.length && original.subSequence(current, current + oldStr.length).toString() == oldStr) {
                XposedBridge.log("DEBUG found $oldStr at $current")
                val strStart = current
                val strEnd = current + oldStr.length

                // See if the replaced text has different spans, doesn't replace if it does
                for (spanInfo in spanList) {
                    if (spanInfo.start in (strStart + 1)..<strEnd) {
                        XposedBridge.log("DEBUG continued due to $spanInfo start")
                        continue@outer
                    }
                    if (spanInfo.end in (strStart + 1)..<strEnd) {
                        XposedBridge.log("DEBUG continued due to $spanInfo end")
                        continue@outer
                    }
                }

                modifiedText = original.subSequence(0, current).toString() + newStr + original.subSequence(strEnd, original.length)
                // Move spans to the new text
                for (spanInfo in spanList) {
                    spanInfo.start = if (spanInfo.start < strEnd) spanInfo.start else spanInfo.start + newStr.length - oldStr.length
                    spanInfo.end = if (spanInfo.end <= strStart) spanInfo.end else spanInfo.end + newStr.length - oldStr.length
                }
            }
        }

        val modified = SpannableStringBuilder(modifiedText)
        spanList.forEach {
            modified.setSpan(it.span, it.start, it.end, it.flags)
        }
        return modified
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Don't hook itself
        if (lpparam.packageName == "me.textviewhook") return

        val methodHook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                // Change the text to be displayed in TextView (while keeping the spans)
                if (param.args[0] == null) return

                XposedBridge.log("TextView setText arg: ${param.args[0]} Class: ${param.args[0].javaClass.name}")

//                param.args[0] = (param.thisObject as TextView).id.toString()

                if (param.args[0] is Spanned) {
                    if ((param.args[0] as Spanned).length != 8) return
                    param.args[0] = doSpannedReplace(param.args[0] as Spanned, "lyc8503", "lyc8504")
                    return
                }

                val originalText = param.args[0] as CharSequence
                param.args[0] = originalText.replace(Regex("lyc8503"), "lyc8504")
            }
        }

        // setTextMethod calls setTextMethodWithType, so we just need to hook the latter
        // val setTextMethod = XposedHelpers.findMethodExact(TextView::class.java, "setText", CharSequence::class.java)
        val setTextMethodWithType = XposedHelpers.findMethodExact(TextView::class.java, "setText", CharSequence::class.java, TextView.BufferType::class.java)

        XposedBridge.log("Hooking setText method of TextView in package: ${lpparam.packageName}, method: $setTextMethodWithType")
        XposedBridge.hookMethod(setTextMethodWithType, methodHook)
    }
}