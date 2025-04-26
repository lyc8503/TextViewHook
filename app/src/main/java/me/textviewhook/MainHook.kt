package me.textviewhook

import android.text.SpannableStringBuilder
import android.text.Spanned
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

                if (param.args[0] is Spanned) {
                    val originalSpanned = param.args[0] as Spanned
                    val modifiedSpanned = SpannableStringBuilder()

                    var current = 0
                    originalSpanned.getSpans(0, originalSpanned.length, Any::class.java).forEach { span ->
                        val start = originalSpanned.getSpanStart(span)
                        val end = originalSpanned.getSpanEnd(span)
                        val flags = originalSpanned.getSpanFlags(span)
                        XposedBridge.log("Span: $span $start $end $flags Class: ${span.javaClass.name}")

                        if (start > current) {
                            // Copy the gap between the last span and the current span
                            val gap = originalSpanned.subSequence(current, start)
                            modifiedSpanned.append(doReplace(gap))
                        } else {
                            val newText = doReplace(originalSpanned.subSequence(start, end))
                            XposedBridge.log("New Text: $newText")
                            modifiedSpanned.append(newText, span, flags)
                        }
                        current = start
                    }

                    // Copy the remaining text after the last span (also handles the case where there are no spans)
                    if (current < originalSpanned.length) {
                        val remainingText = originalSpanned.subSequence(current, originalSpanned.length)
                        modifiedSpanned.append(doReplace(remainingText))
                    }

                    param.args[0] = modifiedSpanned
                    return
                }
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