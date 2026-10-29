package com.bugenzhao.mnga.ui.editor

import androidx.compose.foundation.background

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.KeyboardHide
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Minimize
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.TheaterComedy
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.extra.SuperBottomSheet
import top.yukonga.miuix.kmp.extra.SuperListPopup
import top.yukonga.miuix.kmp.extra.WindowDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.bugenzhao.mnga.logicCallAsync
import com.bugenzhao.mnga.ui.components.MiuixDropdownMenu
import com.bugenzhao.mnga.ui.components.MiuixMenuItem
import com.bugenzhao.mnga.model.ContentEditorModel
import com.bugenzhao.mnga.model.GenericPostModel
import com.bugenzhao.mnga.model.PlusFeature
import com.bugenzhao.mnga.model.PlusModel
import com.bugenzhao.mnga.model.ToastModel
import com.bugenzhao.mnga.protos.datamodel.PostAttachment
import com.bugenzhao.mnga.protos.service.UploadAttachmentResponse
import com.bugenzhao.mnga.util.L
import com.bugenzhao.mnga.util.errorLocalized
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** BBCode color palette, ported from `ContentCombiner.palette`. */
private val COLOR_PALETTE: List<Pair<String, Long>> = listOf(
    "skyblue" to 0x87CEEB, "royalblue" to 0x4169E1, "blue" to 0x0066BB,
    "darkblue" to 0x00008B, "orange" to 0xA06700, "orangered" to 0xFF4500,
    "crimson" to 0xDC143C, "red" to 0xDD0000, "firebrick" to 0xB22222,
    "darkred" to 0x8B0000, "green" to 0x3D9F0E, "limegreen" to 0x32CD32,
    "seagreen" to 0x2E8B57, "teal" to 0x008080, "deeppink" to 0xFF1493,
    "tomato" to 0xFF6347, "coral" to 0xFF7F50, "purple" to 0x800080,
    "indigo" to 0x4B0082, "burlywood" to 0xDEB887, "sandybrown" to 0xF4A460,
    "chocolate" to 0xD2691E, "sienna" to 0xA0522D, "silver" to 0x888888,
    "white" to 0xFFFFFF,
)

private val SMALL_SIZES = listOf("10%", "50%", "80%", "90%")
private val LARGE_SIZES = listOf("110%", "120%", "150%", "200%")

/** Editor sheet for every topic posting flow, ported from `GenericEditorView`. */
@Composable
fun PostEditorSheet(model: PostReplyModel, onDismiss: () -> Unit) {
    GenericEditorSheet(
        model = model,
        update = model::update,
        onAttachmentUploaded = model::addAttachment,
        onDismiss = onDismiss,
    )
}

/** Editor sheet for short messages, ported from `ShortMessageEditorView`. */
@Composable
fun ShortMessageEditorSheet(model: ShortMessageModel, onDismiss: () -> Unit) {
    GenericEditorSheet(
        model = model,
        update = model::update,
        onAttachmentUploaded = null,
        onDismiss = onDismiss,
    )
}

@Composable
private fun GenericEditorSheet(
    model: GenericPostModel,
    update: ((GenericPostModel.Context) -> GenericPostModel.Context) -> Unit,
    onAttachmentUploaded: ((PostAttachment) -> Unit)?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    val ctx by model.context.collectAsState()
    val isSending by model.isSending.collectAsState()
    val showEditor by model.showEditor.collectAsState()
    var showDiscardDialog by remember { mutableStateOf(false) }

    LaunchedEffect(showEditor) { if (!showEditor) onDismiss() }

    val editor = remember(ctx?.seed) { ContentEditorModel(ctx?.content ?: "") }
    val text by editor.text.collectAsState()
    val selection by editor.selection.collectAsState()
    val panel by editor.showing.collectAsState()
    val uploadingImage by editor.image.collectAsState()

    val sendToFocus = remember { FocusRequester() }
    val subjectFocus = remember { FocusRequester() }
    val contentFocus = remember { FocusRequester() }

    // Focus the first empty field, mirroring `setFocusOnAppear`. The target
    // field may compose a frame later than this effect, so guard the request.
    fun requestFocusSafely(requester: FocusRequester) {
        try {
            requester.requestFocus()
        } catch (e: IllegalStateException) {
            // Not attached yet; the user can focus the field manually.
        }
    }

    // v1.8.2 keyboard hardening: the sheet lives in the SuperBottomSheet
    // dialog window, and a focus request fired the moment the context
    // lands can race that window becoming the active one — focus lands
    // (or silently drops) but the IME never rises, which read on device
    // as "tap the reply bar and no keyboard appears". Retry the request
    // after a beat and explicitly ask the keyboard to show.
    val keyboardController =
        androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    LaunchedEffect(ctx?.seed) {
        val target = when {
            ctx?.to?.isEmpty() == true -> sendToFocus
            ctx?.subject?.isEmpty() == true -> subjectFocus
            ctx != null -> contentFocus
            else -> null
        } ?: return@LaunchedEffect
        requestFocusSafely(target)
        delay(250)
        requestFocusSafely(target)
        keyboardController?.show()
    }

    fun pushContent(newText: String) {
        update { it.copy(content = newText) }
    }

    // Only keystrokes go through the field's `onValueChange`; the sticker panel,
    // the BBCode toolbar and the image upload all mutate the editor's text
    // directly, so mirror every text change into the context. Without this a
    // sticker inserted last shows up in the field but never gets sent.
    LaunchedEffect(editor) {
        editor.text.collect { newText ->
            if (model.context.value?.content != newText) pushContent(newText)
        }
    }

    val canUpload =
        ctx?.task?.buildUploadAttachmentRequest(ByteArray(0)) != null

    val photoPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                scope.launch {
                    val bytes = withContext(Dispatchers.IO) { decodeImageBytes(context, uri) }
                    if (bytes != null) editor.image.value = bytes
                    else
                        ToastModel.editorAlert.show(
                            ToastModel.Message.Error("Image")
                        )
                }
            }
        }

    // Upload the picked image, then insert the BBCode reference.
    LaunchedEffect(uploadingImage) {
        val data = uploadingImage ?: return@LaunchedEffect
        val task = ctx?.task ?: run {
            editor.image.value = null
            return@LaunchedEffect
        }
        val request = task.buildUploadAttachmentRequest(data)
        if (request == null) {
            editor.image.value = null
            return@LaunchedEffect
        }
        logicCallAsync(request, UploadAttachmentResponse.parser()).fold(
            onSuccess = { response ->
                onAttachmentUploaded?.invoke(response.attachment)
                editor.insert("\n[img]./${response.attachment.url}[/img]\n")
                editor.image.value = null
            },
            onFailure = { e ->
                ToastModel.editorAlert.show(ToastModel.Message.Error(e.message ?: "error"))
                editor.image.value = null
            },
        )
    }

    val title = L.str(context, ctx?.task?.actionTitle?.takeIf { it.isNotEmpty() } ?: "Editor")

    // Miuix SuperBottomSheet replaces the Material ModalBottomSheet: it is
    // composed only while the editor is open (show = true), drag-down /
    // outside-tap / back all route to the same onDismissRequest as before,
    // and insideMargin is zeroed so the content keeps its own 20.dp padding.
    SuperBottomSheet(
        show = true,
        onDismissRequest = {
            model.editorDismissed()
            onDismiss()
        },
        insideMargin = DpSize(0.dp, 0.dp),
    ) {
        Column(
            Modifier.fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { showDiscardDialog = true }) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = L.str(context, "Discard"),
                        tint = MiuixTheme.colorScheme.error,
                    )
                }
                if (isSending) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Button(
                        onClick = { model.send() },
                        enabled = ctx != null,
                        colors = ButtonDefaults.buttonColorsPrimary(),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(L.str(context, "Send"))
                    }
                }
            }

            EditorAlertBanner()

            if (ctx == null) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val c = ctx!!

                if (c.to != null) {
                    Column {
                        var toState by
                            remember(c.seed) { mutableStateOf(TextFieldValue(c.to ?: "")) }
                        TextField(
                            value = toState,
                            onValueChange = {
                                toState = it
                                update { ctx0 -> ctx0.copy(to = it.text) }
                            },
                            label = L.str(context, "Send To"),
                            singleLine = true,
                            modifier =
                                Modifier.fillMaxWidth().focusRequester(sendToFocus),
                        )
                        Text(
                            L.str(context, "Separate multiple users with space."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }

                if (c.subject != null) {
                    var subjectState by
                        remember(c.seed) { mutableStateOf(TextFieldValue(c.subject ?: "")) }
                    TextField(
                        value = subjectState,
                        onValueChange = {
                            subjectState = it
                            update { ctx0 -> ctx0.copy(subject = it.text) }
                        },
                        label = L.str(context, "Subject"),
                        singleLine = true,
                        modifier =
                            Modifier.fillMaxWidth().focusRequester(subjectFocus),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    // Prepend a tag placeholder and select it.
                                    val inserted = "[...]"
                                    subjectState =
                                        TextFieldValue(
                                            inserted + subjectState.text,
                                            TextRange(1, inserted.length - 1),
                                        )
                                    update { ctx0 ->
                                        ctx0.copy(subject = subjectState.text)
                                    }
                                },
                            ) {
                                Icon(
                                    Icons.Outlined.Label,
                                    contentDescription = L.str(context, "Add Tag"),
                                )
                            }
                        },
                    )
                }

                TextField(
                    value = TextFieldValue(text, selection),
                    onValueChange = { value ->
                        editor.text.value = value.text
                        editor.selection.value = value.selection
                        if (value.text != text) pushContent(value.text)
                    },
                    label = L.str(context, "Content"),
                    minLines = 5,
                    maxLines = 12,
                    modifier =
                        Modifier.fillMaxWidth()
                            .heightIn(min = 150.dp)
                            .focusRequester(contentFocus)
                            .onFocusChanged {
                                if (it.isFocused && panel == ContentEditorModel.Panel.STICKER) {
                                    editor.showing.value = ContentEditorModel.Panel.NONE
                                }
                            },
                )

                if (c.anonymous != null) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Outlined.TheaterComedy,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.primary,
                        )
                        Text(
                            L.str(context, "Anonymous"),
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = c.anonymous == true,
                            onCheckedChange = { want ->
                                if (want && !PlusModel.checkPlus(PlusFeature.ANONYMOUS)) {
                                    return@Switch
                                }
                                update { ctx0 -> ctx0.copy(anonymous = want) }
                            },
                        )
                    }
                }

                EditorToolbar(
                    editor = editor,
                    canUpload = canUpload,
                    onPickImage = {
                        photoPicker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    onHideKeyboard = { focusManager.clearFocus() },
                )

                if (uploadingImage != null) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            L.str(context, "Upload Image"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                        LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 4.dp))
                    }
                }

                if (panel == ContentEditorModel.Panel.STICKER) {
                    StickerInputPanel(editor)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDiscardDialog) {
        WindowDialog(
            show = true,
            title = L.str(context, "Discard the draft?"),
            summary = L.str(context, "Save the draft by swiping down to dismiss the editor."),
            onDismissRequest = { showDiscardDialog = false },
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    text = L.str(context, "Cancel"),
                    onClick = { showDiscardDialog = false },
                )
                Spacer(Modifier.width(8.dp))
                TextButton(
                    text = L.str(context, "Discard"),
                    onClick = {
                        showDiscardDialog = false
                        model.discardCurrentContext()
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        textColor = MiuixTheme.colorScheme.error,
                    ),
                )
            }
        }
    }
}

/**
 * In-sheet editor alert surface: the models route errors to the
 * `editorAlert` channel, rendered here for ~3s without tap-to-dismiss.
 */
@Composable
private fun EditorAlertBanner() {
    val context = LocalContext.current
    val message by ToastModel.editorAlert.message.collectAsState()
    val error = message as? ToastModel.Message.Error

    if (error != null) {
        LaunchedEffect(error.id) {
            delay(3000)
            ToastModel.editorAlert.dismiss()
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MiuixTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                context.errorLocalized(error.error),
                style = MaterialTheme.typography.bodySmall,
                color = MiuixTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** BBCode insertion toolbar, ported from `ContentTextEditorView`. */
@Composable
private fun EditorToolbar(
    editor: ContentEditorModel,
    canUpload: Boolean,
    onPickImage: () -> Unit,
    onHideKeyboard: () -> Unit,
) {
    val context = LocalContext.current
    var colorMenu by remember { mutableStateOf(false) }
    var sizeMenu by remember { mutableStateOf(false) }

    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = {
                editor.showing.value =
                    if (editor.showing.value == ContentEditorModel.Panel.STICKER)
                        ContentEditorModel.Panel.NONE
                    else ContentEditorModel.Panel.STICKER
                onHideKeyboard()
            },
        ) {
            Icon(Icons.Outlined.EmojiEmotions, contentDescription = null)
        }
        if (canUpload) {
            IconButton(onClick = onPickImage) {
                Icon(Icons.Outlined.Image, contentDescription = L.str(context, "Image"))
            }
        }
        IconButton(onClick = { editor.appendBold() }) {
            Icon(Icons.Outlined.FormatBold, contentDescription = "bold")
        }
        IconButton(onClick = { editor.appendDel() }) {
            Icon(Icons.Outlined.FormatStrikethrough, contentDescription = "del")
        }
        Box {
            IconButton(onClick = { colorMenu = true }) {
                Icon(Icons.Outlined.Palette, contentDescription = "color")
            }
            // Miuix menu (phase 3): the color swatch rides in the
            // MiuixMenuItem leading slot - Miuix's own DropdownImpl
            // rows cannot carry one (see ui/components/MiuixMenu.kt).
            MiuixDropdownMenu(
                expanded = colorMenu,
                onDismissRequest = { colorMenu = false },
                alignment = PopupPositionProvider.Align.Start,
            ) {
                COLOR_PALETTE.forEach { (name, hex) ->
                    MiuixMenuItem(
                        text = name,
                        leadingContent = {
                            Box(
                                Modifier.size(14.dp)
                                    .background(Color(0xFF000000 or hex))
                            )
                        },
                        onClick = {
                            editor.appendColor(name)
                            colorMenu = false
                        },
                    )
                }
            }
        }
        Box {
            IconButton(onClick = { sizeMenu = true }) {
                Icon(Icons.Outlined.FormatSize, contentDescription = "size")
            }
            // Flat text-only menu: expressed with Miuix SuperListPopup +
            // DropdownImpl (the same combo SuperDropdown uses internally).
            SuperListPopup(
                show = sizeMenu,
                onDismissRequest = { sizeMenu = false },
            ) {
                ListPopupColumn {
                    val sizes = SMALL_SIZES + LARGE_SIZES
                    sizes.forEachIndexed { index, size ->
                        DropdownImpl(
                            text = size,
                            optionSize = sizes.size,
                            isSelected = false,
                            index = index,
                            onSelectedIndexChange = { selected ->
                                editor.appendSize(sizes[selected])
                                sizeMenu = false
                            },
                        )
                    }
                }
            }
        }
        IconButton(
            onClick = { editor.appendCollapsed(L.str(context, "Collapsed Content")) },
        ) {
            Icon(Icons.Outlined.UnfoldMore, contentDescription = "collapse")
        }
        IconButton(onClick = { editor.appendAt() }) {
            Icon(Icons.Outlined.AlternateEmail, contentDescription = "at")
        }
        IconButton(onClick = { editor.appendQuoted() }) {
            Icon(Icons.Outlined.FormatQuote, contentDescription = "quote")
        }
        IconButton(onClick = { editor.appendDice() }) {
            Icon(Icons.Outlined.Casino, contentDescription = "dice")
        }
        IconButton(onClick = { editor.insertSeparator() }) {
            Icon(Icons.Outlined.Minimize, contentDescription = "separator")
        }
        IconButton(onClick = { editor.appendHeader() }) {
            Icon(Icons.Outlined.Title, contentDescription = "header")
        }
        IconButton(onClick = onHideKeyboard) {
            Icon(Icons.Outlined.KeyboardHide, contentDescription = null)
        }
    }
}

/**
 * Read the picked image, downscale it to at most [maxDimension] and JPEG
 * encode it (quality 80), mirroring the iOS `ImagePicker` encoding.
 */
private fun decodeImageBytes(
    context: android.content.Context,
    uri: Uri,
    maxDimension: Int = 2048,
): ByteArray? =
    try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        var sample = 1
        while (
            bounds.outWidth / (sample * 2) >= maxDimension ||
                bounds.outHeight / (sample * 2) >= maxDimension
        )
            sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap =
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return null
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        bitmap.recycle()
        out.toByteArray()
    } catch (e: Exception) {
        null
    }
