package com.rk.xededitor.MainActivity.handlers

import android.content.Intent
import android.view.KeyEvent
import androidx.core.app.ActivityCompat.startActivityForResult
import com.google.android.material.tabs.TabLayoutMediator
import com.rk.libcommons.Printer
import com.rk.xededitor.MainActivity.BatchReplacement
import com.rk.xededitor.MainActivity.MainActivity
import com.rk.xededitor.MainActivity.editor.TabFragment
import com.rk.xededitor.MainActivity.file.FileAction.Companion.to_save_file
import com.rk.xededitor.MainActivity.file.REQUEST_CODE_OPEN_DIRECTORY
import com.rk.xededitor.R
import io.github.rosemoe.sora.interfaces.KeyEventHandler

object KeyEventHandler {
    
    fun onAppKeyEvent(keyEvent: KeyEvent) {
        val currentFragment = MainActivity.activityRef.get()?.adapter?.getCurrentFragment()
        
        if (currentFragment?.editor == null) {
            return
        }
        
        val editor = currentFragment.editor!!
        
        if (keyEvent.isShiftPressed) {
            when (keyEvent.keyCode) {
                KeyEvent.KEYCODE_S -> {
                    currentFragment?.let {
                        to_save_file = it.file
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                        MainActivity.activityRef.get()?.let { activity ->
                            startActivityForResult(
                                activity,
                                intent,
                                REQUEST_CODE_OPEN_DIRECTORY,
                                null,
                            )
                        }
                    }
                }
            }
        }
        
        if (keyEvent.isCtrlPressed) {
            when (keyEvent.keyCode) {
                KeyEvent.KEYCODE_W -> {
                    MainActivity.activityRef.get()?.apply {
                        adapter.removeFragment(tabLayout.selectedTabPosition)
                        binding.tabs.invalidate()
                        binding.tabs.requestLayout()
                        
                        // Detach and re-attach the TabLayoutMediator
                        TabLayoutMediator(binding.tabs, viewPager) { tab, position ->
                            tab.text = tabViewModel.fragmentTitles[position]
                        }
                            .attach()
                        MenuItemHandler.update(this)
                        
                    }
                }
                
                KeyEvent.KEYCODE_K -> {
                    MainActivity.activityRef.get()?.let {
                        if (it.tabLayout.selectedTabPosition == 0) {
                            return
                        }
                        it.tabLayout.selectTab(
                            it.tabLayout.getTabAt(it.tabLayout.selectedTabPosition - 1)
                        )
                    }
                }
                
                KeyEvent.KEYCODE_L -> {
                    MainActivity.activityRef.get()?.let {
                        if (it.tabLayout.selectedTabPosition == it.tabLayout.tabCount - 1) {
                            return
                        }
                        it.tabLayout.selectTab(
                            it.tabLayout.getTabAt(it.tabLayout.selectedTabPosition + 1)
                        )
                    }
                }
                
                KeyEvent.KEYCODE_S -> {
                    currentFragment.save(false)
                }
                
                KeyEvent.KEYCODE_PLUS,
                70 -> {
                    editor.let {
                        if (it.textSizePx < 57) {
                            it.textSizePx += 2
                        }
                    }
                }
                
                KeyEvent.KEYCODE_MINUS -> {
                    editor.let {
                        if (it.textSizePx > 8) {
                            it.textSizePx -= 2
                        }
                    }
                }
                
                KeyEvent.KEYCODE_F -> {
                    MainActivity.activityRef.get()?.let {
                        MenuClickHandler.handle(it, it.menu.findItem(R.id.search))
                    }
                }
                
                KeyEvent.KEYCODE_H -> {
                    MainActivity.activityRef.get()?.let {
                        it.startActivity(Intent(it, BatchReplacement::class.java))
                    }
                }
                
                KeyEvent.KEYCODE_P -> {
                    MainActivity.activityRef.get()?.let {
                        Printer.print(it, editor?.text.toString())
                    }
                }
                
                KeyEvent.KEYCODE_G -> {
                    val line = 0
                    var column =
                        (editor.text.getLine(line).length).coerceAtMost(
                            editor.cursor.leftColumn
                        )
                    if (column < 0) {
                        column = 0
                    }
                    editor.cursor.set(line, column)
                }
                
                KeyEvent.KEYCODE_B -> {
                    val line = editor.text.lineCount - 1
                    var column =
                        (editor.text.getLine(line).length - 1).coerceAtMost(
                            editor.cursor.leftColumn
                        )
                    if (column < 0) {
                        column = 0
                    }
                    editor.cursor.set(line, column)
                }
            }
            
        }
    }
}
