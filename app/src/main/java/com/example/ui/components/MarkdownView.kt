package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TasklyRed

/**
 * Renders Markdown formatted text in Jetpack Compose.
 * Supports headings (#, ##, ###), bullet lists (*, -), numbered lists (1.),
 * code blocks (```...```), blockquotes (>), inline bold (**), italic (*), and inline code (`).
 */
@Composable
fun MarkdownView(
    markdownText: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    codeBackgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    accentColor: Color = TasklyRed
) {
    val blocks = parseMarkdownBlocks(markdownText)

    Column(modifier = modifier) {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    val (style, size) = when (block.level) {
                        1 -> MaterialTheme.typography.titleLarge to 20.sp
                        2 -> MaterialTheme.typography.titleMedium to 17.sp
                        else -> MaterialTheme.typography.titleSmall to 15.sp
                    }
                    Text(
                        text = buildInlineMarkdown(block.text, textColor, codeBackgroundColor),
                        style = style.copy(fontWeight = FontWeight.Bold, fontSize = size),
                        color = textColor,
                        modifier = Modifier.padding(top = if (index > 0) 8.dp else 0.dp, bottom = 4.dp)
                    )
                }

                is MarkdownBlock.BulletItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp, end = 8.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Text(
                            text = buildInlineMarkdown(block.text, textColor, codeBackgroundColor),
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MarkdownBlock.NumberedItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${block.number}.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = accentColor,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = buildInlineMarkdown(block.text, textColor, codeBackgroundColor),
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MarkdownBlock.CodeBlock -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(codeBackgroundColor)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = block.code,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = textColor
                        )
                    }
                }

                is MarkdownBlock.Blockquote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(22.dp)
                                .background(accentColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = buildInlineMarkdown(block.text, textColor, codeBackgroundColor),
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            color = textColor.copy(alpha = 0.85f),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MarkdownBlock.Paragraph -> {
                    if (block.text.isNotBlank()) {
                        Text(
                            text = buildInlineMarkdown(block.text, textColor, codeBackgroundColor),
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                            color = textColor,
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

private sealed class MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock()
    data class BulletItem(val text: String) : MarkdownBlock()
    data class NumberedItem(val number: String, val text: String) : MarkdownBlock()
    data class CodeBlock(val language: String?, val code: String) : MarkdownBlock()
    data class Blockquote(val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
}

private fun parseMarkdownBlocks(input: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = input.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        // Code block start
        if (trimmed.startsWith("```")) {
            val language = trimmed.removePrefix("```").trim().ifEmpty { null }
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            blocks.add(MarkdownBlock.CodeBlock(language, codeLines.joinToString("\n")))
            i++
            continue
        }

        // Headings
        if (trimmed.startsWith("#")) {
            var level = 0
            while (level < trimmed.length && trimmed[level] == '#') {
                level++
            }
            val text = trimmed.substring(level).trim()
            blocks.add(MarkdownBlock.Heading(level.coerceIn(1, 3), text))
            i++
            continue
        }

        // Bullet lists
        if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ")) {
            val text = trimmed.substring(2).trim()
            blocks.add(MarkdownBlock.BulletItem(text))
            i++
            continue
        }

        // Numbered list: e.g. "1. " or "2. "
        val numMatch = Regex("""^(\d+)\.\s+(.*)""").find(trimmed)
        if (numMatch != null) {
            val num = numMatch.groupValues[1]
            val text = numMatch.groupValues[2]
            blocks.add(MarkdownBlock.NumberedItem(num, text))
            i++
            continue
        }

        // Blockquotes: e.g. "> "
        if (trimmed.startsWith(">")) {
            val text = trimmed.removePrefix(">").trim()
            blocks.add(MarkdownBlock.Blockquote(text))
            i++
            continue
        }

        // Paragraph
        if (trimmed.isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(trimmed))
        }
        i++
    }

    return blocks
}

/**
 * Parses inline markdown: **bold**, *italic*, `code`, and ~~strikethrough~~.
 */
private fun buildInlineMarkdown(
    text: String,
    defaultColor: Color,
    codeBgColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val length = text.length

        while (cursor < length) {
            // Check bold: **text**
            if (cursor + 1 < length && text[cursor] == '*' && text[cursor + 1] == '*') {
                val end = text.indexOf("**", cursor + 2)
                if (end != -1) {
                    val boldText = text.substring(cursor + 2, end)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(boldText)
                    pop()
                    cursor = end + 2
                    continue
                }
            }

            // Check inline code: `code`
            if (text[cursor] == '`') {
                val end = text.indexOf('`', cursor + 1)
                if (end != -1) {
                    val codeSnippet = text.substring(cursor + 1, end)
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = codeBgColor,
                            fontSize = 13.sp
                        )
                    )
                    append(" $codeSnippet ")
                    pop()
                    cursor = end + 1
                    continue
                }
            }

            // Check strikethrough: ~~text~~
            if (cursor + 1 < length && text[cursor] == '~' && text[cursor + 1] == '~') {
                val end = text.indexOf("~~", cursor + 2)
                if (end != -1) {
                    val strikeText = text.substring(cursor + 2, end)
                    pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                    append(strikeText)
                    pop()
                    cursor = end + 2
                    continue
                }
            }

            // Check italic: *text* (single asterisk)
            if (text[cursor] == '*') {
                val end = text.indexOf('*', cursor + 1)
                if (end != -1 && end > cursor + 1) {
                    val italicText = text.substring(cursor + 1, end)
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(italicText)
                    pop()
                    cursor = end + 1
                    continue
                }
            }

            // Normal character
            append(text[cursor])
            cursor++
        }
    }
}
