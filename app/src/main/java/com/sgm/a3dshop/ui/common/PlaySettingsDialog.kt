package com.sgm.a3dshop.ui.common

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import com.sgm.a3dshop.databinding.DialogPlaySettingsBinding

class PlaySettingsDialog(
    context: Context,
    private val onStart: (Int) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogPlaySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogPlaySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
    }

    private fun setupViews() {
        binding.apply {
            btnCancel.setOnClickListener {
                dismiss()
            }

            btnStart.setOnClickListener {
                val intervalSeconds = when (radioGroupInterval.checkedRadioButtonId) {
                    rb5Sec.id -> 5
                    rb30Sec.id -> 30
                    rb1Min.id -> 60
                    rb5Min.id -> 300
                    rb10Min.id -> 600
                    else -> 5
                }
                onStart(intervalSeconds)
                dismiss()
            }

            // 默认选中5秒
            rb5Sec.isChecked = true
        }
    }
} 