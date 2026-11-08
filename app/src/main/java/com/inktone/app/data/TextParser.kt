package com.inktone.app.data

import android.os.ParcelFileDescriptor
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

/** 一个排版块：level 0 为正文，1..6 为标题层级。 */
data class Block(val level: Int, val text: String, val firstLineIndent: Boolean)

/** 目录条目：startIndex 为扁平字符序列中的起始下标，level 为标题层级。 */
data class Chapter(val title: String, val startIndex: Int, val level: Int = 1)

/**
 * 结构化解析结果。
 * [blockStarts] 记录每个块首字符在扁平序列中的起始下标，[total] 为全部字符数。
 */
class ReaderDocument(
    val blocks: List<Block>,
    val chapters: List<Chapter>,
    val total: Int,
    val blockStarts: IntArray
)

object TextParser {

    fun parseTxt(text: String): ReaderDocument {
        val blocks = mutableListOf<Block>()
        text.replace("\r", "").split('\n').forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEach
            val level = chapterHeadingLevel(line)
            blocks.add(Block(level, line, firstLineIndent = level == 0))
        }
        return buildDocument(blocks)
    }

    fun parseEpub(file: File): ReaderDocument = try {
        ZipFile(file).use { zip ->
            val opfPath = findOpfPath(zip)
            if (opfPath == null) {
                buildDocument(htmlBlocks(readAllXhtml(zip)))
            } else {
                parseEpubWithToc(zip, opfPath)
            }
        }
    } catch (e: Exception) {
        buildDocument(emptyList())
    }

    fun parseDocx(file: File): ReaderDocument = try {
        ZipFile(file).use { zip -> buildDocument(parseDocxBlocks(zip)) }
    } catch (e: Exception) {
        buildDocument(emptyList())
    }

    fun pdfPageCount(file: File): Int = try {
        android.graphics.pdf.PdfRenderer(
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        ).use { it.pageCount }
    } catch (e: Exception) {
        0
    }

    fun buildDocument(blocks: List<Block>): ReaderDocument {
        val chapters = mutableListOf<Chapter>()
        var index = 0
        val starts = IntArray(blocks.size)
        blocks.forEachIndexed { i, b ->
            starts[i] = index
            if (b.level > 0 && b.text.isNotBlank()) {
                chapters.add(Chapter(b.text.trim(), index, b.level))
            }
            index += b.text.length
        }
        return ReaderDocument(blocks, chapters, index, starts)
    }

    private fun buildDocument(blocks: List<Block>, chapters: List<Chapter>): ReaderDocument {
        var index = 0
        val starts = IntArray(blocks.size)
        blocks.forEachIndexed { i, b ->
            starts[i] = index
            index += b.text.length
        }
        return ReaderDocument(blocks, chapters, index, starts)
    }

    // ---------- DOCX ----------

    private fun parseDocxBlocks(zip: ZipFile): List<Block> {
        val levels = docxStyleLevels(zip)
        val entry = zip.getEntry("word/document.xml") ?: return emptyList()
        val xml = zip.getInputStream(entry).readBytes().decodeToString()
        val doc = parseXml(xml) ?: return emptyList()
        val blocks = mutableListOf<Block>()
        descendants(doc.documentElement).filter { it.localName == "p" }.forEach { p ->
            val pStyle = descendants(p).firstOrNull { it.localName == "pStyle" }?.getAttribute("w:val")
            val level = levels[pStyle] ?: 0
            val text = descendants(p).filter { it.localName == "t" }.joinToString("") { it.textContent }.trim()
            if (text.isNotBlank()) {
                blocks.add(Block(level, text, firstLineIndent = level == 0))
            }
        }
        return blocks
    }

    private fun docxStyleLevels(zip: ZipFile): Map<String, Int> {
        val entry = zip.getEntry("word/styles.xml") ?: return emptyMap()
        val xml = zip.getInputStream(entry).readBytes().decodeToString()
        val doc = parseXml(xml) ?: return emptyMap()
        val map = mutableMapOf<String, Int>()
        descendants(doc.documentElement).filter { it.localName == "style" }.forEach { style ->
            val id = style.getAttribute("w:styleId")
            if (id.isEmpty()) return@forEach
            val name = descendants(style).firstOrNull { it.localName == "name" }?.getAttribute("w:val") ?: ""
            val outlineLvl = descendants(style).firstOrNull { it.localName == "outlineLvl" }
                ?.getAttribute("w:val")?.toIntOrNull()
            val level = headingLevelFromName(name) ?: (outlineLvl?.let { it + 1 } ?: 0)
            if (level > 0) map[id] = level
        }
        return map
    }

    private fun headingLevelFromName(name: String): Int? {
        val n = name.trim()
        Regex("""^heading\s*(\d+)""", RegexOption.IGNORE_CASE).find(n)?.let {
            return it.groupValues[1].toInt().coerceIn(1, 6)
        }
        Regex("""^标题\s*(\d+)""").find(n)?.let {
            return it.groupValues[1].toInt().coerceIn(1, 6)
        }
        Regex("""^标题\s*([一二三四五六七八九十])""").find(n)?.let {
            return "一二三四五六七八九十".indexOf(it.groupValues[1]) + 1
        }
        return null
    }

    // ---------- EPUB ----------

    private fun findOpfPath(zip: ZipFile): String? {
        val entry = zip.getEntry("META-INF/container.xml") ?: return null
        val xml = zip.getInputStream(entry).readBytes().decodeToString()
        val doc = parseXml(xml) ?: return null
        return descendants(doc.documentElement)
            .firstOrNull { it.localName == "rootfile" }
            ?.getAttribute("full-path")
            ?.takeIf { it.isNotEmpty() }
    }

    private fun readAllXhtml(zip: ZipFile): String {
        val sb = StringBuilder()
        val entries = zip.entries()
        while (entries.hasMoreElements()) {
            val e = entries.nextElement()
            if (e.isDirectory) continue
            val name = e.name.lowercase()
            if (name.endsWith(".xhtml") || name.endsWith(".html") || name.endsWith(".htm")) {
                sb.append(zip.getInputStream(e).readBytes().decodeToString()).append("\n")
            }
        }
        return sb.toString()
    }

    private fun htmlBlocks(html: String): List<Block> {
        var s = html
        s = Regex("""(?is)<(script|style)[^>]*>.*?</\1>""").replace(s, " ")
        s = Regex("""(?i)<br\s*/?>""").replace(s, "\n")
        s = Regex("""(?is)<h([1-6])\b[^>]*>(.*?)</h\1>""").replace(s) { m ->
            "\u0001${m.groupValues[1]}\u0001${m.groupValues[2]}\u0002"
        }
        s = Regex("""(?i)</?(p|div|li|blockquote|tr|section|article|td|ul|ol)[^>]*>""").replace(s, "\n")
        s = Regex("""<[^>]+>""").replace(s, "")
        s = decodeHtmlEntities(s)

        val blocks = mutableListOf<Block>()
        var i = 0
        while (i < s.length) {
            val start = s.indexOf('\u0001', i)
            if (start < 0) {
                appendBodyBlocks(blocks, s.substring(i))
                break
            }
            appendBodyBlocks(blocks, s.substring(i, start))
            val levelEnd = s.indexOf('\u0001', start + 1)
            if (levelEnd < 0) {
                i = start + 1
                continue
            }
            val level = s.substring(start + 1, levelEnd).toIntOrNull()?.coerceIn(1, 6) ?: 1
            val contentEnd = s.indexOf('\u0002', levelEnd + 1)
            val heading = s.substring(levelEnd + 1, if (contentEnd >= 0) contentEnd else s.length).trim()
            if (heading.isNotEmpty()) blocks.add(Block(level, heading, firstLineIndent = false))
            i = if (contentEnd >= 0) contentEnd + 1 else s.length
        }
        if (blocks.isEmpty()) {
            val t = s.trim()
            if (t.isNotEmpty()) blocks.add(Block(0, t, true))
        }
        return blocks
    }

    private fun appendBodyBlocks(blocks: MutableList<Block>, text: String) {
        text.split('\n').forEach { line ->
            val t = line.trim()
            if (t.isNotEmpty()) blocks.add(Block(0, t, firstLineIndent = true))
        }
    }

    private fun decodeHtmlEntities(input: String): String {
        var t = input
        t = t.replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&mdash;", "—")
            .replace("&ndash;", "–")
            .replace("&hellip;", "…")
            .replace("&ldquo;", "“")
            .replace("&rdquo;", "”")
            .replace("&lsquo;", "‘")
            .replace("&rsquo;", "’")
            .replace("&middot;", "·")
        t = Regex("""&#(\d+);""").replace(t) { m ->
            m.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: m.value
        }
        t = Regex("""&#x([0-9a-fA-F]+);""").replace(t) { m ->
            m.groupValues[1].toIntOrNull(16)?.toChar()?.toString() ?: m.value
        }
        return t
    }

    // ---------- EPUB 目录(基于 NCX/NAV) ----------

    private fun parseEpubWithToc(zip: ZipFile, opfPath: String): ReaderDocument {
        val base = opfPath.substringBeforeLast('/', "").let { if (it.isEmpty()) "" else "$it/" }
        val opfEntry = zip.getEntry(opfPath)
        val doc = if (opfEntry == null) null else parseXml(zip.getInputStream(opfEntry).readBytes().decodeToString())
        val manifest = LinkedHashMap<String, String>()
        val spine = ArrayList<String>()
        var ncxHref: String? = null
        var navHref: String? = null
        if (doc != null) {
            val spineToc = descendants(doc.documentElement).firstOrNull { it.localName == "spine" }?.getAttribute("toc")
            descendants(doc.documentElement).forEach { el ->
                when (el.localName) {
                    "item" -> {
                        val id = el.getAttribute("id")
                        val href = el.getAttribute("href")
                        val props = el.getAttribute("properties")
                        if (id.isNotEmpty() && href.isNotEmpty()) {
                            manifest[id] = href
                            if (spineToc != null && id == spineToc) ncxHref = href
                            if (props.contains("nav")) navHref = href
                        }
                    }
                    "itemref" -> {
                        val idref = el.getAttribute("idref")
                        if (idref.isNotEmpty()) spine.add(idref)
                    }
                }
            }
        }

        val tocEntries: List<Triple<String, String, Int>> = when {
            navHref != null -> parseNavToc(zip, normalizePath(base + navHref!!))
            ncxHref != null -> parseNcxToc(zip, normalizePath(base + ncxHref!!))
            else -> emptyList()
        }

        val allBlocks = ArrayList<Block>()
        val anchorOffsets = HashMap<String, Int>()
        var offset = 0
        for (idref in spine) {
            val href = manifest[idref] ?: continue
            val path = normalizePath(base + href)
            val entry = zip.getEntry(path) ?: continue
            val html = zip.getInputStream(entry).readBytes().decodeToString()
            val parsed = parseHtmlWithAnchors(html)
            val fileBase = offset
            for ((frag, local) in parsed.second) anchorOffsets["$path#$frag"] = fileBase + local
            anchorOffsets[path] = fileBase
            allBlocks.addAll(parsed.first)
            offset += parsed.first.sumOf { it.text.length }
        }
        if (allBlocks.isEmpty()) return buildDocument(emptyList())

        val chapters = ArrayList<Chapter>()
        for ((title, href, depth) in tocEntries) {
            val (filePart, fragPart) = splitHref(href)
            if (filePart.isEmpty()) continue
            val fullPath = normalizePath(base + filePart)
            val idx = if (fragPart != null) anchorOffsets["$fullPath#$fragPart"] else anchorOffsets[fullPath]
            if (idx != null) chapters.add(Chapter(title.trim(), idx, (depth + 1).coerceIn(1, 6)))
        }

        val finalChapters = if (chapters.isEmpty()) buildDocument(allBlocks).chapters else chapters
        return buildDocument(allBlocks, finalChapters)
    }

    private fun parseNcxToc(zip: ZipFile, ncxPath: String): List<Triple<String, String, Int>> {
        val entry = zip.getEntry(ncxPath) ?: return emptyList()
        val doc = parseXml(zip.getInputStream(entry).readBytes().decodeToString()) ?: return emptyList()
        val navMap = descendants(doc.documentElement).firstOrNull { it.localName == "navMap" } ?: return emptyList()
        val result = ArrayList<Triple<String, String, Int>>()
        fun walk(node: Element, depth: Int) {
            for (i in 0 until node.childNodes.length) {
                val child = node.childNodes.item(i)
                if (child is Element && child.localName == "navPoint") {
                    val label = descendants(child).firstOrNull { it.localName == "text" }?.textContent?.trim() ?: ""
                    val src = descendants(child).firstOrNull { it.localName == "content" }?.getAttribute("src") ?: ""
                    if (label.isNotEmpty() && src.isNotEmpty()) result.add(Triple(label, src, depth))
                    walk(child, depth + 1)
                }
            }
        }
        walk(navMap, 0)
        return result
    }

    private fun parseNavToc(zip: ZipFile, navPath: String): List<Triple<String, String, Int>> {
        val entry = zip.getEntry(navPath) ?: return emptyList()
        val doc = parseXml(zip.getInputStream(entry).readBytes().decodeToString()) ?: return emptyList()
        val nav = descendants(doc.documentElement).firstOrNull {
            it.localName == "nav" && (it.getAttribute("epub:type") == "toc" || it.getAttribute("type") == "toc")
        } ?: return emptyList()
        val ol = descendants(nav).firstOrNull { it.localName == "ol" } ?: return emptyList()
        val result = ArrayList<Triple<String, String, Int>>()
        fun walkOl(node: Element, depth: Int) {
            for (i in 0 until node.childNodes.length) {
                val li = node.childNodes.item(i) as? Element ?: continue
                if (li.localName != "li") continue
                val a = descendants(li).firstOrNull { it.localName == "a" }
                val span = descendants(li).firstOrNull { it.localName == "span" }
                val title = (span?.textContent ?: a?.textContent)?.trim() ?: ""
                val href = a?.getAttribute("href") ?: ""
                if (title.isNotEmpty() && href.isNotEmpty()) result.add(Triple(title, href, depth))
                descendants(li).firstOrNull { it.localName == "ol" }?.let { walkOl(it, depth + 1) }
            }
        }
        walkOl(ol, 0)
        return result
    }

    private fun splitHref(href: String): Pair<String, String?> {
        val idx = href.indexOf('#')
        return if (idx >= 0) href.substring(0, idx) to href.substring(idx + 1) else href to null
    }

    /** 解析 HTML 文本为块，并返回每个锚点 id 对应的局部字符偏移。 */
    private fun parseHtmlWithAnchors(html: String): Pair<List<Block>, Map<String, Int>> {
        var s = html
        s = Regex("""(?is)<(script|style)[^>]*>.*?</\1>""").replace(s, " ")
        s = Regex("""(?i)<br\s*/?>""").replace(s, "\n")
        s = Regex("""(?is)<h([1-6])\b[^>]*>(.*?)</h\1>""").replace(s) { m ->
            "\u0001${m.groupValues[1]}\u0001${m.groupValues[2]}\u0002"
        }
        s = Regex("""(?i)</?(p|div|li|blockquote|tr|section|article|td|ul|ol)[^>]*>""").replace(s, "\n")

        val blocks = ArrayList<Block>()
        val anchors = HashMap<String, Int>()
        var offset = 0
        val line = StringBuilder()
        var i = 0

        fun flush() {
            val t = decodeHtmlEntities(line.toString()).trim()
            if (t.isNotEmpty()) {
                blocks.add(Block(0, t, firstLineIndent = true))
                offset += t.length
            }
            line.setLength(0)
        }

        while (i < s.length) {
            val c = s[i]
            when {
                c == '<' -> {
                    val end = s.indexOf('>', i)
                    if (end < 0) { line.append(s.substring(i)); break }
                    val tag = s.substring(i + 1, end)
                    Regex("""(?i)\bid\s*=\s*["']([^"']+)["']""").find(tag)?.let { m ->
                        anchors[m.groupValues[1]] = offset + line.length
                    }
                    i = end + 1
                }
                c == '\u0001' -> {
                    flush()
                    val levelEnd = s.indexOf('\u0001', i + 1)
                    if (levelEnd < 0) { i++; continue }
                    val level = s.substring(i + 1, levelEnd).toIntOrNull()?.coerceIn(1, 6) ?: 1
                    val contentEnd = s.indexOf('\u0002', levelEnd + 1)
                    val heading = s.substring(levelEnd + 1, if (contentEnd >= 0) contentEnd else s.length)
                    val t = decodeHtmlEntities(heading).trim()
                    if (t.isNotEmpty()) {
                        blocks.add(Block(level, t, firstLineIndent = false))
                        offset += t.length
                    }
                    i = if (contentEnd >= 0) contentEnd + 1 else s.length
                }
                c == '\n' -> { flush(); i++ }
                else -> { line.append(c); i++ }
            }
        }
        flush()
        return blocks to anchors
    }

    // ---------- TXT ----------

    private fun chapterHeadingLevel(line: String): Int {
        if (line.isEmpty() || line.length > 60) return 0
        if (Regex("""^第\s*[0-9一二三四五六七八九十百千零两]+\s*[章回卷部篇集]""").containsMatchIn(line)) return 1
        if (Regex("""^第\s*[0-9一二三四五六七八九十百千零两]+\s*[节]""").containsMatchIn(line)) return 2
        if (Regex("""^(Chapter|CHAPTER)\s+\d+""").containsMatchIn(line)) return 1
        if (Regex("""^[一二三四五六七八九十百千零]+[、.．]\s*\S""").containsMatchIn(line)) return 2
        if (Regex("""^\d{1,3}[、.．]\s*\S""").containsMatchIn(line)) return 3
        return 0
    }

    // ---------- 通用 ----------

    private fun normalizePath(path: String): String {
        val parts = mutableListOf<String>()
        path.split('/').forEach { seg ->
            when (seg) {
                "", "." -> {}
                ".." -> if (parts.isNotEmpty()) parts.removeAt(parts.size - 1)
                else -> parts.add(seg)
            }
        }
        return parts.joinToString("/")
    }

    private fun parseXml(text: String): Document? = try {
        val f = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        f.newDocumentBuilder().parse(ByteArrayInputStream(text.toByteArray(Charsets.UTF_8)))
    } catch (e: Exception) {
        null
    }

    private fun descendants(node: Node): List<Element> {
        val out = mutableListOf<Element>()
        fun walk(n: Node) {
            if (n is Element) {
                out.add(n)
                for (i in 0 until n.childNodes.length) walk(n.childNodes.item(i))
            }
        }
        walk(node)
        return out
    }
}
