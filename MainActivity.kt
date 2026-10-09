package com.example.tasmeequran

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue

class MainActivity : Activity() {
    private val requestAudioCode = 101
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var statusView: TextView
    private lateinit var heardView: TextView
    private lateinit var resultView: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    private val targetVerse =
        "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            statusView.text = "خدمة التعرف على الصوت غير متاحة على هذا الجهاز. تحقق من وجود خدمة التعرف على الصوت وتحديثها."
            startButton.isEnabled = false
        } else {
            setupRecognizer()
        }
    }

    private fun buildUi() {
        val scroll = ScrollView(this)
        scroll.setBackgroundColor(Color.rgb(243, 247, 245))
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
        }
        scroll.addView(content)
        setContentView(scroll)

        val title = TextView(this).apply {
            text = "📖 تسميع القرآن الكريم بالصوت"
            setTextColor(Color.rgb(0, 105, 92))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 25f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        content.addView(title, matchWrap())

        val intro = TextView(this).apply {
            text = "اضغط ابدأ الاستماع، واسمح باستخدام الميكروفون، ثم اقرأ الآية بصوت واضح."
            setTextColor(Color.rgb(69, 90, 100))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(10))
        }
        content.addView(intro, matchWrap())

        val verse = TextView(this).apply {
            text = targetVerse
            setTextColor(Color.rgb(38, 56, 74))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f)
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(18), dp(14), dp(18))
            setBackgroundColor(Color.rgb(232, 245, 233))
        }
        val verseParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(10); bottomMargin = dp(12) }
        content.addView(verse, verseParams)

        startButton = Button(this).apply { text = "🎙️ ابدأ الاستماع" }
        content.addView(startButton, matchWrap())
        stopButton = Button(this).apply {
            text = "إيقاف الاستماع"
            isEnabled = false
        }
        content.addView(stopButton, matchWrap())

        statusView = section(content, "حالة الاستماع", "جاهز. اضغط ابدأ الاستماع.")
        heardView = section(content, "الكلام الذي التقطه الهاتف", "لم يتم التقاط كلام بعد.")
        resultView = section(content, "نتيجة المقارنة", "لم تبدأ المحاولة بعد.")

        val note = TextView(this).apply {
            text = "ملاحظة: يعتمد التعرف على الصوت على خدمة متوفرة في الهاتف وقد يحتاج إلى الإنترنت. قد يخطئ التعرف الآلي في التلاوة؛ استخدم النتيجة للمساعدة في المراجعة وليس كحكم نهائي على التجويد."
            setTextColor(Color.rgb(96, 125, 139))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, dp(18), 0, 0)
        }
        content.addView(note, matchWrap())

        startButton.setOnClickListener { requestPermissionAndListen() }
        stopButton.setOnClickListener { speechRecognizer?.stopListening() }
    }

    private fun section(parent: LinearLayout, heading: String, initial: String): TextView {
        val label = TextView(this).apply {
            text = heading
            setTextColor(Color.rgb(96, 125, 139))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundColor(Color.rgb(245, 247, 248))
        }
        box.addView(label, matchWrap())
        val value = TextView(this).apply {
            text = initial
            setTextColor(Color.rgb(38, 50, 56))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(0, dp(5), 0, 0)
        }
        box.addView(value, matchWrap())
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(10) }
        parent.addView(box, params)
        return value
    }

    private fun setupRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).also { recognizer ->
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    statusView.text = "الميكروفون جاهز. اقرأ الآية الآن."
                    startButton.isEnabled = false
                    stopButton.isEnabled = true
                }
                override fun onBeginningOfSpeech() {
                    statusView.text = "أسمع صوتك الآن..."
                }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    statusView.text = "تم التقاط الصوت، جارٍ تحليل الكلام..."
                    stopButton.isEnabled = false
                }
                override fun onError(error: Int) {
                    statusView.text = errorMessage(error)
                    startButton.isEnabled = true
                    stopButton.isEnabled = false
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spoken = matches?.firstOrNull().orEmpty()
                    heardView.text = if (spoken.isBlank()) "لم يتم التعرف على كلمات. جرّب مرة أخرى." else spoken
                    compare(spoken)
                    statusView.text = if (spoken.isBlank()) "لم يلتقط النظام كلمات واضحة." else "انتهى الاستماع."
                    startButton.isEnabled = true
                    stopButton.isEnabled = false
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!partial.isNullOrBlank()) {
                        heardView.text = partial
                        statusView.text = "أتعرف على كلامك..."
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun requestPermissionAndListen() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), requestAudioCode)
            return
        }
        startListening()
    }

    private fun startListening() {
        val recognizer = speechRecognizer ?: run {
            statusView.text = "خدمة التعرف على الصوت غير جاهزة على هذا الجهاز."
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-EG")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-EG")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "اقرأ الآية بصوت واضح")
        }
        heardView.text = "بانتظار الكلام..."
        resultView.text = "بانتظار نتيجة التعرف على الصوت."
        statusView.text = "جارٍ تشغيل الميكروفون..."
        try {
            recognizer.startListening(intent)
        } catch (e: Exception) {
            statusView.text = "تعذر بدء الاستماع. أغلق أي تطبيق يستخدم الميكروفون ثم حاول مرة أخرى."
            startButton.isEnabled = true
            stopButton.isEnabled = false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestAudioCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListening()
            } else {
                statusView.text = "لا يمكن الاستماع دون السماح بإذن الميكروفون من إعدادات التطبيق."
            }
        }
    }

    private fun normalize(text: String): String {
        return text.lowercase()
            .replace(Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]"), "")
            .replace(Regex("[إأآٱ]"), "ا")
            .replace("ى", "ي")
            .replace(Regex("[ـ۞۩،؛:,.!?؟\\\"“”()\\[\\]{}]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun compare(spoken: String) {
        val expected = normalize(targetVerse)
        val actual = normalize(spoken)
        if (actual.isBlank()) {
            resultView.text = "لم يتم التعرف على كلام واضح. حاول مرة أخرى."
            resultView.setTextColor(Color.rgb(198, 40, 40))
        } else if (actual == expected) {
            resultView.text = "✅ النص الذي تعرّف عليه الهاتف مطابق للآية المكتوبة."
            resultView.setTextColor(Color.rgb(46, 125, 50))
        } else {
            val expectedWords = expected.split(" ")
            val actualWords = actual.split(" ")
            val missing = expectedWords.filter { it !in actualWords }.distinct()
            resultView.text = "⚠️ يوجد اختلاف في النص المتعرّف عليه. كلمات من الآية لم تظهر: " +
                (missing.joinToString("، ").ifBlank { "لم تُحدد كلمات مفقودة بوضوح" }) +
                ". قد يكون السبب اختلاف النطق أو خطأ التعرف الصوتي."
            resultView.setTextColor(Color.rgb(198, 40, 40))
        }
    }

    private fun errorMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "خطأ في تسجيل الصوت. تحقق من الميكروفون."
        SpeechRecognizer.ERROR_CLIENT -> "حدث خطأ في خدمة التعرف. أغلق التطبيق وافتحه مجددًا."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "إذن الميكروفون غير مسموح. فعّله من إعدادات التطبيق."
        SpeechRecognizer.ERROR_NETWORK -> "تعذر الاتصال بخدمة التعرف على الصوت. تحقق من الإنترنت."
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "انتهت مهلة الاتصال. جرّب اتصالًا أفضل."
        SpeechRecognizer.ERROR_NO_MATCH -> "لم يتعرف النظام على كلمات واضحة. أعد المحاولة."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "خدمة التعرف مشغولة. انتظر قليلًا ثم حاول."
        SpeechRecognizer.ERROR_SERVER -> "خدمة التعرف على الصوت لا تستجيب حاليًا."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "لم يلتقط النظام كلامًا. تحدث بعد الضغط مباشرة."
        else -> "تعذر التعرف على الصوت (رمز الخطأ: $error). تحقق من الإنترنت والميكروفون."
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )

    override fun onDestroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        super.onDestroy()
    }
}
