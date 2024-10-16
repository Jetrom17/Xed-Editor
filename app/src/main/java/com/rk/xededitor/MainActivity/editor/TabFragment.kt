package com.rk.xededitor.MainActivity.editor

import android.graphics.Typeface
import android.os.Bundle
import android.os.Environment
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.rk.settings.PreferencesData
import com.rk.settings.PreferencesData.getBoolean
import com.rk.settings.PreferencesKeys
import com.rk.xededitor.MainActivity.MainActivity
import com.rk.xededitor.R
import com.rk.xededitor.SetupEditor
import com.rk.xededitor.rkUtils
import io.github.rosemoe.sora.text.ContentIO
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TabFragment : Fragment() {

    var file: File? = null
    var editor: CodeEditor? = null

    // see @MenuClickHandler.update()
    var setListener = false

    fun showSuggestions(yes: Boolean) {
        if (yes) {
            editor?.inputType = InputType.TYPE_TEXT_VARIATION_NORMAL
        } else {
            editor?.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
    }

    fun isShowSuggestion(): Boolean {
        return editor?.inputType != InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val context = requireContext()
            arguments?.let {
                val filePath = it.getString(ARG_FILE_PATH)
                if (filePath != null) {
                    file = File(filePath)
                }
            }
            editor = CodeEditor(context)
            showSuggestions(getBoolean(PreferencesKeys.SHOW_SUGGESTIONS, false))

            val setupEditor = SetupEditor(editor!!, context)
            setupEditor.ensureTextmateTheme(context)
            lifecycleScope.launch(Dispatchers.Default) {
                launch(Dispatchers.IO) {
                    try {
                        val inputStream: InputStream = FileInputStream(file)
                        val content = ContentIO.createFrom(inputStream)
                        inputStream.close()
                        withContext(Dispatchers.Main) { editor!!.setText(content) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // this throw error for some reason
                        // editor!!.setText(getString(R.string.file_exist_not))
                    }
                }
                launch(Dispatchers.Default) { setupEditor.setupLanguage(file!!.name) }
            }
            with(editor!!) {
                val tabSize = PreferencesData.getString(PreferencesKeys.TAB_SIZE, "4").toInt()
                props.deleteMultiSpaces = tabSize
                tabWidth = tabSize
                props.deleteEmptyLineFast = false
                props.useICULibToSelectWords = true
                setPinLineNumber(getBoolean(PreferencesKeys.PIN_LINE_NUMBER, false))
                isLineNumberEnabled = getBoolean(PreferencesKeys.SHOW_LINE_NUMBERS, true)
                isCursorAnimationEnabled =
                    getBoolean(PreferencesKeys.CURSOR_ANIMATION_ENABLED, true)
                isWordwrap = getBoolean(PreferencesKeys.WORD_WRAP_ENABLED, false)
                setTextSize(PreferencesData.getString(PreferencesKeys.TEXT_SIZE, "14").toFloat())
                getComponent(EditorAutoCompletion::class.java).isEnabled = true

                File(Environment.getExternalStorageDirectory(), "karbon/font.ttf").let {
                    typefaceText =
                        if (getBoolean(PreferencesKeys.EDITOR_FONT, false) and it.exists()) {
                            Typeface.createFromFile(it)
                        } else {
                            Typeface.createFromAsset(
                                requireContext().assets,
                                "JetBrainsMono-Regular.ttf",
                            )
                        }
                }
                
            }
        } catch (e: Exception) {
            // this fragment is detached and should be garbage collected
            e.printStackTrace()
            editor?.release()
            editor = null
        }
    }

    fun save(showToast: Boolean = true) {
        lifecycleScope.launch(Dispatchers.IO) {
            if (file!!.exists().not() and showToast) {
                withContext(Dispatchers.Main) { rkUtils.toast(getString(R.string.file_exist_not)) }
            }
            try {
                val content = withContext(Dispatchers.Main) { editor?.text }

                val outputStream = FileOutputStream(file, false)
                if (content != null) {
                    ContentIO.writeTo(content, outputStream, true)
                    if (showToast) {
                        withContext(Dispatchers.Main) {
                            rkUtils.toast(getString(R.string.file_saved))
                        }
                    }
                }

                try {
                    MainActivity.activityRef.get()?.let { activity ->
                        val index = activity.tabViewModel.fragmentFiles.indexOf(file)
                        activity.tabViewModel.fragmentTitles.let {
                            if (file!!.name != it[index]) {
                                it[index] = file!!.name
                                withContext(Dispatchers.Main) {
                                    activity.tabLayout.getTabAt(index)?.text = file!!.name
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) { rkUtils.toast(e.message) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { rkUtils.toast(e.message) }
            }
        }
    }

    fun undo() {
        editor?.undo()
        MainActivity.activityRef.get()?.let {
            it.menu.findItem(R.id.redo).isEnabled = editor?.canRedo() == true
            it.menu.findItem(R.id.undo).isEnabled = editor?.canUndo() == true
        }
    }

    fun redo() {
        editor?.redo()
        MainActivity.activityRef.get()?.let {
            it.menu.findItem(R.id.redo).isEnabled = editor?.canRedo() == true
            it.menu.findItem(R.id.undo).isEnabled = editor?.canUndo() == true
        }
    }

    private var isSearching: Boolean = false

    fun isSearching(): Boolean {
        return isSearching
    }

    fun setSearching(s: Boolean) {
        isSearching = s
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return editor
    }

    override fun onDestroyView() {
        super.onDestroyView()
        editor?.release()
    }

    override fun onDestroy() {
        super.onDestroy()
        editor?.release()
    }

    companion object {
        private const val ARG_FILE_PATH = "file_path"

        fun newInstance(file: File): TabFragment {
            val fragment = TabFragment()
            val args = Bundle().apply { putString(ARG_FILE_PATH, file.absolutePath) }
            fragment.arguments = args
            return fragment
        }
    }
}
