/*
 * SPDX-License-Identifier: MPL-2.0
 * Copyright © 2020 Skyline Team and Contributors (https://github.com/skyline-emu/)
 */

package emu.skyline.input.onscreen

import android.content.Context
import emu.skyline.input.ButtonId
import emu.skyline.utils.SwitchColors
import emu.skyline.utils.sharedPreferences

interface ControllerConfiguration {
    var alpha : Int
    var textColor : Int
    var backgroundColor : Int
    var enabled : Boolean
    var globalScale : Float
    var relativeX : Float
    var relativeY : Float
}

/**
 * Dummy implementation so layout editor is able to render [OnScreenControllerView] when [android.view.View.isInEditMode] is true
 */
class ControllerConfigurationDummy(defaultRelativeX : Float, defaultRelativeY : Float) : ControllerConfiguration {
    override var alpha : Int = 155
    override var textColor = SwitchColors.BLACK.color
    override var backgroundColor = SwitchColors.WHITE.color
    override var enabled = true
    override var globalScale = 1f
    override var relativeX = defaultRelativeX
    override var relativeY = defaultRelativeY
}

class ControllerConfigurationImpl(private val context : Context, private val buttonId : ButtonId, defaultRelativeX : Float, defaultRelativeY : Float) : ControllerConfiguration {
    private inline fun <reified T> config(default : T, prefix : String = "${buttonId.name}_") = sharedPreferences(context, default, prefix, "controller_config")

    override var alpha by config(155, "")
    override var textColor by config(SwitchColors.BLACK.color)
    override var backgroundColor by config(SwitchColors.WHITE.color)
    override var enabled by config(true)
    override var globalScale by config(1.15f, "")
    override var relativeX by config(defaultRelativeX)
    override var relativeY by config(defaultRelativeY)
}
