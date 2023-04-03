/*
 * SPDX-License-Identifier: MPL-2.0
 * Copyright © 2020 Skyline Team and Contributors (https://github.com/skyline-emu/)
 */

package emu.skyline

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import emu.skyline.adapter.*
import emu.skyline.data.AppItem
import emu.skyline.data.AppItemTag
import emu.skyline.data.DataItem
import emu.skyline.data.HeaderItem
import emu.skyline.databinding.MainActivityBinding
import emu.skyline.loader.AppEntry
import emu.skyline.loader.LoaderResult
import emu.skyline.loader.RomFormat
import emu.skyline.provider.DocumentsProvider
import emu.skyline.settings.AppSettings
import emu.skyline.settings.EmulationSettings
import emu.skyline.settings.SettingsActivity
import emu.skyline.utils.GpuDriverHelper
import emu.skyline.utils.WindowInsetsHelper
import javax.inject.Inject
import kotlin.math.ceil
import java.io.File
import java.io.InputStream

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    companion object {
        private val formatOrder = listOf(RomFormat.NSP, RomFormat.XCI, RomFormat.NRO, RomFormat.NSO, RomFormat.NCA)
    }

    private val binding by lazy { MainActivityBinding.inflate(layoutInflater) }

    @Inject
    lateinit var appSettings : AppSettings

    private val adapter = GenericAdapter()

    private val layoutType get() = LayoutType.values()[appSettings.layoutType]

    private val viewModel by viewModels<MainViewModel>()

    private var formatFilter : RomFormat? = null
    private var appEntries : Map<RomFormat, List<AppEntry>>? = null

    enum class SortingOrder {
        AlphabeticalAsc,
        AlphabeticalDesc
    }

    private fun copyFileToPrivateDir() { val fileName = "example.txt" val inputStream = assets.open(fileName) val file = File(filesDir, fileName) val outputStream = FileOutputStream(file) inputStream.copyTo(outputStream) }
    private var refreshIconVisible = false
        set(visible) {
            field = visible
            binding.refreshIcon.apply {
                if (visible != isVisible) {
                    binding.refreshIcon.alpha = if (visible) 0f else 1f
                    animate().alpha(if (visible) 1f else 0f).withStartAction { isVisible = true }.withEndAction { isInvisible = !visible }.apply { duration = 500 }.start()
                }
            }
        }

    private val documentPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
        it?.let { uri ->
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            appSettings.searchLocation = uri.toString()

            loadRoms(false)
        }
    }

    private val settingsCallback = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (appSettings.refreshRequired) loadRoms(false)
    }

    private fun AppItem.toViewItem() = AppViewItem(layoutType, this, ::selectStartGame, ::selectShowGameDialog)

    override fun onCreate(savedInstanceState : Bundle?) {
    super.onCreate(savedInstanceState) binding.root.apply { setContentView(this) adapter.setLayoutType(layoutType) binding.recyclerView.adapter = adapter } copyFileToPrivateDir() //... }

        // Try to return to normal GPU clocks upon resuming back to main activity, to avoid GPU being stuck at max clocks after a crash
        GpuDriverHelper.forceMaxGpuClocks(false)

        var layoutTypeChanged = false
        for (appViewItem in adapter.allItems.filterIsInstance(AppViewItem::class.java)) {
            if (layoutType != appViewItem.layoutType) {
                appViewItem.layoutType = layoutType
                layoutTypeChanged = true
            } else {
                break
            }
        }

        if (layoutTypeChanged) {
            setAppListDecoration()
            adapter.notifyItemRangeChanged(0, adapter.currentItems.size)
        }

        viewModel.checkRomHash(Uri.parse(appSettings.searchLocation), EmulationSettings.global.systemLanguage)
    }
}
