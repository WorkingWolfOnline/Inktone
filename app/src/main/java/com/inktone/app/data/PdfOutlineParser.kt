package com.inktone.app.data

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.Inflater

/**
 * 极简 PDF 书签(目录/Outline)解析器。
 *
 * 仅读取 PDF 书签目录，提取每个条目的标题与页码(0-based)。
 * 支持 xref stream(/Prev 链)、对象流(ObjStm)、FlateDecode(PNG/TIFF Predictor)。
 * 不解析内容流与字体，解析一次即释放，内存占用可控。
 */
object PdfOutlineParser {

    data class PdfOutline(val title: String, val page: Int)

    fun outline(file: File): List<PdfOutline> = try {
        Parser(file.readBytes()).parse()
    } catch (e: Exception) {
        emptyList()
    }

    private sealed class P {
        object Null : P()
        class Name(val v: String) : P()
        class Str(val bytes: ByteArray) : P()
        class Num(val v: Double) : P()
        class Ref(val num: Int, val gen: Int) : P()
        class Arr(val v: List<P>) : P()
        class Dict(val v: Map<String, P>) : P()
        class Bool(val v: Boolean) : P()
    }

    private class XRefEntry(val type: Int, val a: Int, val b: Int) // type 1: a=offset,b=gen; type 2: a=objstm,b=index

    private class Parser(private val data: ByteArray) {
        private val objTable = HashMap<Int, XRefEntry>()
        private var rootRef: P.Ref? = null
        private val objCache = HashMap<Int, P>()
        private val objStmCache = HashMap<Int, ByteArray>()

        fun parse(): List<PdfOutline> {
            loadXRef()
            val root = rootRef?.let { resolve(it) } as? P.Dict ?: return emptyList()
            val outlines = resolve(root.v["Outlines"] as? P.Ref ?: return emptyList()) as? P.Dict ?: return emptyList()
            val first = outlines.v["First"] as? P.Ref ?: return emptyList()
            val pageIndex = buildPageIndex(root)
            val result = ArrayList<PdfOutline>()
            walkOutline(first, pageIndex, result)
            return result
        }

        // ---------- xref ----------
        private fun loadXRef() {
            val eof = data.lastIndexOf("%%EOF".toByteArray())
            val sx = data.lastIndexOf("startxref".toByteArray(), eof)
            if (sx < 0) return
            var offset = parseInt(data, sx + "startxref".length).first
            val visited = HashSet<Int>()
            while (offset >= 0 && offset < data.size && visited.add(offset)) {
                var i = skipWs(data, offset)
                offset = if (data.startsWith("xref".toByteArray(), i)) parseClassicXRef(i + 4)
                else parseXRefStream(i)
            }
        }

        private fun parseClassicXRef(start: Int): Int {
            var i = skipWs(data, start)
            var prev = -1
            while (true) {
                i = skipWs(data, i)
                if (data.startsWith("trailer".toByteArray(), i)) {
                    i += "trailer".length
                    val v = parseDict(data, i).first
                    val d = (v as? P.Dict)?.v
                    if (d != null) {
                        (d["Root"] as? P.Ref)?.let { rootRef = it }
                        prev = (d["Prev"] as? P.Num)?.v?.toInt() ?: -1
                    }
                    break
                }
                var r = parseInt(data, i); val startObj = r.first; i = r.second
                r = parseInt(data, i); val count = r.first; i = r.second
                for (k in 0 until count) {
                    r = parseInt(data, i); val off = r.first; i = r.second
                    r = parseInt(data, i); val gen = r.first; i = r.second
                    i = skipWs(data, i)
                    if (data.getOrNull(i)?.toInt()?.toChar() == 'n') objTable[startObj + k] = XRefEntry(1, off, gen)
                    i++
                }
            }
            return prev
        }

        private fun parseXRefStream(start: Int): Int {
            var i = parseInt(data, start).second // 跳过对象号
            i = parseInt(data, i).second // 跳过 gen
            i = skipWs(data, i)
            if (data.startsWith("obj".toByteArray(), i)) i += 3
            val streamPos = data.indexOf("stream".toByteArray(), i)
            val d = (parseDict(data, i).first as? P.Dict)?.v ?: return -1
            (d["Root"] as? P.Ref)?.let { rootRef = it }
            val raw = readStreamBody(d, streamPos)
            val w = (d["W"] as? P.Arr)?.v?.mapNotNull { (it as? P.Num)?.v?.toInt() } ?: listOf(1, 1, 1)
            val idxArr = (d["Index"] as? P.Arr)?.v?.mapNotNull { (it as? P.Num)?.v?.toInt() }
            val size = (d["Size"] as? P.Num)?.v?.toInt() ?: 0
            val idx = if (idxArr != null && idxArr.size >= 2) idxArr else listOf(0, size)
            var pos = 0
            var s = 0
            while (s + 1 < idx.size) {
                val startObj = idx[s]; val n = idx[s + 1]; s += 2
                for (k in 0 until n) {
                    val fields = ArrayList<Int>(w.size)
                    for (width in w) {
                        var vv = 0
                        for (t in 0 until width) { vv = (vv shl 8) or (raw[pos].toInt() and 0xFF); pos++ }
                        fields.add(vv)
                    }
                    val type = if (w[0] == 0) 1 else fields[0]
                    if (type == 1) objTable[startObj + k] = XRefEntry(1, fields[1], if (fields.size > 2) fields[2] else 0)
                    else if (type == 2) objTable[startObj + k] = XRefEntry(2, fields[1], if (fields.size > 2) fields[2] else 0)
                }
            }
            return (d["Prev"] as? P.Num)?.v?.toInt() ?: -1
        }

        // ---------- 对象解析 ----------
        private fun resolve(ref: P.Ref): P? {
            objCache[ref.num]?.let { return it }
            val e = objTable[ref.num] ?: return null
            val v = if (e.type == 1) readDirect(e.a) else readFromObjStm(e.a, e.b)
            if (v != null) objCache[ref.num] = v
            return v
        }

        private fun readDirect(offset: Int): P? {
            var i = parseInt(data, offset).second
            i = parseInt(data, i).second
            i = skipWs(data, i)
            if (data.startsWith("obj".toByteArray(), i)) i += 3
            return parseValue(data, i).first
        }

        private fun readFromObjStm(stmNum: Int, index: Int): P? {
            val raw = objStmBody(stmNum) ?: return null
            val d = objStmDict(stmNum) ?: return null
            val n = (d["N"] as? P.Num)?.v?.toInt() ?: 0
            val first = (d["First"] as? P.Num)?.v?.toInt() ?: 0
            var p = 0
            var targetOff = -1
            for (k in 0 until n) {
                var r = parseInt(raw, p); p = r.second // objnum
                r = parseInt(raw, p); val of = r.first; p = r.second // offset
                if (k == index) targetOff = of
            }
            if (targetOff < 0) return null
            return parseValue(raw, first + targetOff).first
        }

        private fun objStmBody(stmNum: Int): ByteArray? {
            objStmCache[stmNum]?.let { return it }
            if (!objTable.containsKey(stmNum)) return null
            val streamPos = objStmStreamPos(stmNum)
            val d = objStmDict(stmNum) ?: return null
            val raw = readStreamBody(d, streamPos)
            if (raw.isNotEmpty()) objStmCache[stmNum] = raw
            return raw
        }

        private fun objStmDict(stmNum: Int): Map<String, P>? {
            val e = objTable[stmNum] ?: return null
            var i = skipWs(data, e.a)
            i = parseInt(data, i).second
            i = parseInt(data, i).second
            i = skipWs(data, i)
            if (data.startsWith("obj".toByteArray(), i)) i += 3
            return (parseDict(data, i).first as? P.Dict)?.v
        }

        private fun objStmStreamPos(stmNum: Int): Int {
            val e = objTable[stmNum] ?: return -1
            var i = skipWs(data, e.a)
            i = parseInt(data, i).second
            i = parseInt(data, i).second
            i = skipWs(data, i)
            if (data.startsWith("obj".toByteArray(), i)) i += 3
            return data.indexOf("stream".toByteArray(), i)
        }

        private fun readStreamBody(d: Map<String, P>, streamPos: Int): ByteArray {
            var i = skipWs(data, streamPos + "stream".length)
            val length = (d["Length"] as? P.Num)?.v?.toInt()
            val raw = if (length != null && length >= 0 && i + length <= data.size) {
                data.copyOfRange(i, i + length)
            } else {
                val end = data.indexOf("endstream".toByteArray(), i)
                if (end < 0) ByteArray(0) else data.copyOfRange(i, end)
            }
            val filter = d["Filter"]
            return if (filter is P.Name && filter.v == "FlateDecode") inflate(raw, decodeParms(d)) else raw
        }

        private fun decodeParms(d: Map<String, P>): Map<String, Int> {
            val dd = ((d["DecodeParms"] ?: d["DP"]) as? P.Dict)?.v ?: return emptyMap()
            val r = HashMap<String, Int>()
            for (k in listOf("Predictor", "Colors", "BitsPerComponent", "Columns")) {
                (dd[k] as? P.Num)?.let { r[k] = it.v.toInt() }
            }
            return r
        }

        private fun inflate(raw: ByteArray, parms: Map<String, Int>): ByteArray {
            val out = ByteArrayOutputStream(raw.size)
            val inf = Inflater()
            try {
                inf.setInput(raw)
                val buf = ByteArray(8192)
                while (!inf.finished()) {
                    val n = inf.inflate(buf)
                    if (n == 0) break
                    out.write(buf, 0, n)
                }
            } catch (e: Exception) {
                // ignore
            } finally {
                inf.end()
            }
            var result = out.toByteArray()
            val predictor = parms["Predictor"] ?: 1
            if (predictor == 2) {
                val columns = (parms["Columns"] ?: 1) * (parms["Colors"] ?: 1)
                for (j in columns until result.size) {
                    result[j] = ((result[j].toInt() + result[j - columns].toInt()) and 0xFF).toByte()
                }
            } else if (predictor >= 10) {
                result = applyPngPredictor(result, (parms["Columns"] ?: 1) * (parms["Colors"] ?: 1))
            }
            return result
        }

        private fun applyPngPredictor(input: ByteArray, rowLen: Int): ByteArray {
            if (rowLen <= 0) return input
            val out = ByteArrayOutputStream(input.size)
            var pos = 0
            val prev = ByteArray(rowLen)
            while (pos < input.size) {
                val ft = input[pos].toInt() and 0xFF; pos++
                val row = ByteArray(rowLen)
                var r = 0
                while (r < rowLen && pos < input.size) { row[r] = input[pos]; r++; pos++ }
                when (ft) {
                    0 -> {}
                    1 -> for (j in 1 until rowLen) row[j] = ((row[j].toInt() + row[j - 1].toInt()) and 0xFF).toByte()
                    2 -> for (j in 0 until rowLen) row[j] = ((row[j].toInt() + prev[j].toInt()) and 0xFF).toByte()
                    3 -> for (j in 0 until rowLen) {
                        val left = if (j > 0) row[j - 1].toInt() and 0xFF else 0
                        row[j] = ((row[j].toInt() + ((left + (prev[j].toInt() and 0xFF)) shr 1)) and 0xFF).toByte()
                    }
                    4 -> for (j in 0 until rowLen) {
                        val aa = if (j > 0) row[j - 1].toInt() and 0xFF else 0
                        val bb = prev[j].toInt() and 0xFF
                        val cc = if (j > 0) prev[j - 1].toInt() and 0xFF else 0
                        val p = aa + bb - cc
                        val pa = kotlin.math.abs(p - aa); val pb = kotlin.math.abs(p - bb); val pc = kotlin.math.abs(p - cc)
                        val pr = if (pa <= pb && pa <= pc) aa else if (pb <= pc) bb else cc
                        row[j] = ((row[j].toInt() + pr) and 0xFF).toByte()
                    }
                }
                out.write(row, 0, rowLen)
                row.copyInto(prev)
            }
            return out.toByteArray()
        }

        // ---------- 页面树 ----------
        private fun buildPageIndex(root: P.Dict): Map<Int, Int> {
            val pagesRef = root.v["Pages"] as? P.Ref ?: return emptyMap()
            val refs = ArrayList<Int>()
            collectPageRefs(pagesRef, refs)
            val map = HashMap<Int, Int>(refs.size * 2)
            refs.forEachIndexed { idx, num -> map[num] = idx }
            return map
        }

        private fun collectPageRefs(ref: P.Ref, acc: MutableList<Int>) {
            val node = resolve(ref) as? P.Dict ?: return
            val kids = node.v["Kids"] as? P.Arr
            if (kids != null) {
                for (k in kids.v) {
                    if (k is P.Ref) {
                        val child = resolve(k) as? P.Dict
                        if (child != null && child.v.containsKey("Kids")) collectPageRefs(k, acc)
                        else acc.add(k.num)
                    }
                }
            }
        }

        // ---------- 目录遍历 ----------
        private fun walkOutline(ref: P.Ref, pageIndex: Map<Int, Int>, out: MutableList<PdfOutline>) {
            var item: P.Ref? = ref
            var guard = 0
            while (item != null && guard < 5000) {
                guard++
                val d = resolve(item) as? P.Dict ?: break
                val title = decodePdfString((d.v["Title"] as? P.Str)?.bytes ?: ByteArray(0)).trim()
                val page = destPage(d.v, pageIndex)
                if (title.isNotEmpty() && page >= 0) out.add(PdfOutline(title, page))
                (d.v["First"] as? P.Ref)?.let { walkOutline(it, pageIndex, out) }
                item = d.v["Next"] as? P.Ref
            }
        }

        private fun destPage(d: Map<String, P>, pageIndex: Map<Int, Int>): Int {
            var dest = d["Dest"]
            if (dest !is P.Arr) {
                val a = resolve(d["A"] as? P.Ref ?: return -1) as? P.Dict ?: return -1
                dest = a.v["D"]
            }
            val arr = dest as? P.Arr ?: return -1
            return when (val first = arr.v.getOrNull(0)) {
                is P.Ref -> pageIndex[first.num] ?: -1
                is P.Num -> first.v.toInt()
                else -> -1
            }
        }

        private fun decodePdfString(bytes: ByteArray): String {
            if (bytes.isEmpty()) return ""
            if (bytes.size >= 2 && (bytes[0].toInt() and 0xFF) == 0xFE && (bytes[1].toInt() and 0xFF) == 0xFF) {
                return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
            }
            if (bytes.size >= 2 && (bytes[0].toInt() and 0xFF) == 0xFF && (bytes[1].toInt() and 0xFF) == 0xFE) {
                return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
            }
            return String(bytes, Charsets.ISO_8859_1)
        }

        // ---------- tokenizer ----------
        private fun skipWs(buf: ByteArray, i0: Int): Int {
            var i = i0
            while (i < buf.size && isWs(buf[i])) i++
            return i
        }

        private fun isWs(c: Byte): Boolean = c == ' '.code.toByte() || c == '\n'.code.toByte() ||
            c == '\r'.code.toByte() || c == '\t'.code.toByte() || c == 0.toByte()

        private fun isDelim(c: Byte): Boolean = isWs(c) || c == '/'.code.toByte() || c == '('.code.toByte() ||
            c == ')'.code.toByte() || c == '<'.code.toByte() || c == '>'.code.toByte() ||
            c == '['.code.toByte() || c == ']'.code.toByte() || c == '{'.code.toByte() ||
            c == '}'.code.toByte() || c == '%'.code.toByte()

        private fun parseInt(buf: ByteArray, i0: Int): Pair<Int, Int> {
            var i = skipWs(buf, i0)
            var neg = false
            if (i < buf.size && (buf[i] == '-'.code.toByte() || buf[i] == '+'.code.toByte())) {
                neg = buf[i] == '-'.code.toByte(); i++
            }
            var v = 0L
            while (i < buf.size && buf[i] in '0'.code.toByte()..'9'.code.toByte()) {
                v = v * 10 + (buf[i] - '0'.code.toByte()); i++
            }
            return (if (neg) -v else v).toInt() to i
        }

        private fun parseValue(buf: ByteArray, i0: Int): Pair<P?, Int> {
            var i = skipWs(buf, i0)
            if (i >= buf.size) return null to i
            val c = buf[i]
            return when {
                c == '/'.code.toByte() -> {
                    var k = i + 1
                    while (k < buf.size && !isDelim(buf[k])) k++
                    P.Name(String(buf, i + 1, k - i - 1, Charsets.ISO_8859_1)) to k
                }
                c == '('.code.toByte() -> parseLiteralString(buf, i)
                c == '<'.code.toByte() -> {
                    if (i + 1 < buf.size && buf[i + 1] == '<'.code.toByte()) parseDict(buf, i)
                    else {
                        var k = indexOfByte(buf, '>'.code, i)
                        if (k < 0) k = buf.size
                        val hex = String(buf, i + 1, k - i - 1, Charsets.ISO_8859_1).trim()
                        P.Str(hexToBytes(hex)) to (k + 1)
                    }
                }
                c == '['.code.toByte() -> {
                    val items = ArrayList<P>()
                    var p = skipWs(buf, i + 1)
                    while (p < buf.size && buf[p] != ']'.code.toByte()) {
                        val r = parseValue(buf, p)
                        r.first?.let { items.add(it) }
                        p = skipWs(buf, r.second)
                    }
                    P.Arr(items) to (p + 1)
                }
                c == ']'.code.toByte() || c == '>'.code.toByte() -> null to i
                c in '0'.code.toByte()..'9'.code.toByte() || c == '-'.code.toByte() ||
                    c == '+'.code.toByte() || c == '.'.code.toByte() -> parseNumber(buf, i)
                startsWith(buf, "true".toByteArray(), i) -> P.Bool(true) to (i + 4)
                startsWith(buf, "false".toByteArray(), i) -> P.Bool(false) to (i + 5)
                startsWith(buf, "null".toByteArray(), i) -> P.Null to (i + 4)
                else -> {
                    var k = i
                    while (k < buf.size && !isDelim(buf[k])) k++
                    null to k
                }
            }
        }

        private fun parseLiteralString(buf: ByteArray, i0: Int): Pair<P, Int> {
            var j = i0 + 1
            val out = ByteArrayOutputStream()
            var depth = 1
            while (j < buf.size && depth > 0) {
                val c = buf[j]
                when {
                    c == '\\'.code.toByte() -> {
                        if (j + 1 >= buf.size) break
                        val n = buf[j + 1]
                        when (n) {
                            'n'.code.toByte() -> { out.write('\n'.code); j += 2 }
                            'r'.code.toByte() -> { out.write('\r'.code); j += 2 }
                            't'.code.toByte() -> { out.write('\t'.code); j += 2 }
                            'b'.code.toByte() -> { out.write(8); j += 2 }
                            'f'.code.toByte() -> { out.write(12); j += 2 }
                            '('.code.toByte(), ')'.code.toByte(), '\\'.code.toByte() -> { out.write(n.toInt()); j += 2 }
                            else -> {
                                var v = 0; var cnt = 0
                                while (j + 1 < buf.size && cnt < 3 && buf[j + 1] in '0'.code.toByte()..'7'.code.toByte()) {
                                    v = v * 8 + (buf[j + 1] - '0'.code.toByte()); j++; cnt++
                                }
                                out.write(v); j++
                            }
                        }
                    }
                    c == '('.code.toByte() -> { out.write(c.toInt()); depth++; j++ }
                    c == ')'.code.toByte() -> { depth--; if (depth > 0) out.write(c.toInt()); j++ }
                    else -> { out.write(c.toInt()); j++ }
                }
            }
            return P.Str(out.toByteArray()) to j
        }

        private fun parseDict(buf: ByteArray, i0: Int): Pair<P, Int> {
            var i = skipWs(buf, i0)
            if (i + 1 < buf.size && buf[i] == '<'.code.toByte() && buf[i + 1] == '<'.code.toByte()) i += 2
            val map = HashMap<String, P>()
            while (true) {
                i = skipWs(buf, i)
                if (i + 1 < buf.size && buf[i] == '>'.code.toByte() && buf[i + 1] == '>'.code.toByte()) {
                    return P.Dict(map) to (i + 2)
                }
                if (i >= buf.size) return P.Dict(map) to i
                val k = parseValue(buf, i)
                val v = parseValue(buf, k.second)
                if (k.first is P.Name) map[(k.first as P.Name).v] = v.first ?: P.Null
                i = v.second
            }
        }

        private fun parseNumber(buf: ByteArray, i0: Int): Pair<P, Int> {
            var j = i0
            if (j < buf.size && (buf[j] == '-'.code.toByte() || buf[j] == '+'.code.toByte())) j++
            while (j < buf.size && (buf[j] in '0'.code.toByte()..'9'.code.toByte() || buf[j] == '.'.code.toByte())) j++
            val num = String(buf, i0, j - i0, Charsets.ISO_8859_1).toDoubleOrNull() ?: 0.0
            var k = skipWs(buf, j)
            if (k < buf.size && buf[k] in '0'.code.toByte()..'9'.code.toByte()) {
                var m = k
                while (m < buf.size && buf[m] in '0'.code.toByte()..'9'.code.toByte()) m++
                val gen = String(buf, k, m - k, Charsets.ISO_8859_1).toIntOrNull()
                val r = skipWs(buf, m)
                if (gen != null && r < buf.size && buf[r] == 'R'.code.toByte()) {
                    return P.Ref(num.toInt(), gen) to (r + 1)
                }
            }
            return P.Num(num) to j
        }

        private fun indexOfByte(buf: ByteArray, target: Int, start: Int): Int {
            for (i in start until buf.size) if (buf[i].toInt() and 0xFF == target) return i
            return -1
        }

        private fun startsWith(buf: ByteArray, target: ByteArray, start: Int): Boolean {
            if (start < 0 || start + target.size > buf.size) return false
            for (j in target.indices) if (buf[start + j] != target[j]) return false
            return true
        }

        private fun hexToBytes(hex: String): ByteArray {
            val out = ByteArrayOutputStream(hex.length / 2 + 1)
            var i = 0
            while (i + 1 < hex.length) {
                out.write(hex.substring(i, i + 2).toIntOrNull(16) ?: 0)
                i += 2
            }
            if (i < hex.length) (hex.substring(i) + "0").toIntOrNull(16)?.let { out.write(it) }
            return out.toByteArray()
        }
    }

    private fun ByteArray.indexOf(target: ByteArray, start: Int): Int {
        if (target.isEmpty() || start < 0) return -1
        outer@ for (i in start..size - target.size) {
            for (j in target.indices) if (this[i + j] != target[j]) continue@outer
            return i
        }
        return -1
    }

    private fun ByteArray.startsWith(target: ByteArray, start: Int): Boolean {
        if (start < 0 || start + target.size > size) return false
        for (j in target.indices) if (this[start + j] != target[j]) return false
        return true
    }

    private fun ByteArray.lastIndexOf(target: ByteArray): Int {
        if (target.isEmpty()) return -1
        for (i in size - target.size downTo 0) {
            var ok = true
            for (j in target.indices) if (this[i + j] != target[j]) { ok = false; break }
            if (ok) return i
        }
        return -1
    }

    private fun ByteArray.lastIndexOf(target: ByteArray, fromIndex: Int): Int {
        if (target.isEmpty()) return -1
        var start = minOf(fromIndex, size - target.size)
        for (i in start downTo 0) {
            var ok = true
            for (j in target.indices) if (this[i + j] != target[j]) { ok = false; break }
            if (ok) return i
        }
        return -1
    }
}
