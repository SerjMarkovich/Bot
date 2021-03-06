package com.justai.jaicf.channel.viber.sdk.api

import com.justai.jaicf.builder.ScenarioDsl
import com.justai.jaicf.channel.viber.sdk.message.Button
import com.justai.jaicf.channel.viber.sdk.message.Keyboard
import com.justai.jaicf.channel.viber.sdk.message.RichMediaObject

class ViberKeyboard {

    private val rows: MutableList<List<Button>> = mutableListOf()

    val buttons: List<Button>
        get() = rows.flatten()

    val rowsCount: Int
        get() = rows.size

    fun addRow(vararg row: FunctionalButton) {
        addRow(row.asList())
    }

    fun addRow(row: List<FunctionalButton>) {
        require(row.size <= Keyboard.DEFAULT_COLUMNS_COUNT) {
            "Maximum buttons row size of ${Keyboard.DEFAULT_COLUMNS_COUNT} exceeded. Received buttons size: ${row.size}"
        }
        require(row.isNotEmpty()) { "Empty row of buttons are not allowed" }

        val buttonWidth = maxOf(Keyboard.DEFAULT_COLUMNS_COUNT / row.size, 1)
        row
            .map { it.toButton() }
            .map { it.copy(rows = 1, columns = buttonWidth) }
            .also { rows.add(it) }
    }
}

fun ViberKeyboard.toRichMediaObject() = RichMediaObject(buttons, buttonsGroupRows = rowsCount)

fun ViberKeyboard.toKeyboard() = Keyboard(buttons)

class KeyboardBuilder(private val defaultStyle: ViberButton.Style = ViberButton.Style()) {

    private val viberKeyboard = ViberKeyboard()

    @ScenarioDsl
    fun row(vararg buttons: FunctionalButton) {
        val buttonsList = buttons.asList()
            .onEach { it.style = it.style ?: defaultStyle }

        viberKeyboard.addRow(buttonsList)
    }

    @ScenarioDsl
    fun row(vararg buttons: String) {
        viberKeyboard.addRow(buttons.map { ReplyButton(text = it, style = defaultStyle) }.toList())
    }

    @ScenarioDsl
    fun row(rowDefaultStyle: ViberButton.Style = defaultStyle, builder: @ScenarioDsl RowBuilder.() -> Unit) {
        viberKeyboard.addRow(RowBuilder(rowDefaultStyle).apply(builder).buttons)
    }

    @ScenarioDsl
    fun reply(text: String, callbackData: String = text) {
        row(ReplyButton(text = text, callbackData = callbackData, style = defaultStyle))
    }

    @ScenarioDsl
    fun redirect(text: String, redirectUrl: String) {
        row(RedirectButton(text = text, redirectUrl = redirectUrl, style = defaultStyle))
    }

    @ScenarioDsl
    fun button(button: FunctionalButton) {
        button.style = button.style ?: defaultStyle
        row(button)
    }

    fun build(): ViberKeyboard {
        return viberKeyboard
    }
}

class RowBuilder(private val defaultStyle: ViberButton.Style) {

    internal val buttons: MutableList<FunctionalButton> = mutableListOf()

    @ScenarioDsl
    fun reply(text: String, callbackData: String = text) {
        buttons.add(ReplyButton(text = text, callbackData = callbackData, style = defaultStyle))
    }

    @ScenarioDsl
    fun redirect(text: String, redirectUrl: String) {
        buttons.add(RedirectButton(text = text, redirectUrl = redirectUrl, style = defaultStyle))
    }

    @ScenarioDsl
    fun button(button: FunctionalButton) {
        button.style = button.style ?: defaultStyle
        buttons.add(button)
    }
}
