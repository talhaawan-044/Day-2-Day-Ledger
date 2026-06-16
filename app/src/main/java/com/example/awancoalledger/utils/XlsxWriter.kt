package com.example.awancoalledger.utils

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Pure Kotlin .xlsx writer using raw OOXML + ZipOutputStream.
 * No Apache POI — zero crash risk on Android.
 * Replicates the TypeScript ExcelJS export style:
 *   Row 1: Title       – black bg, white bold Courier New 16
 *   Row 2: Subtitle    – neon-green bg (#C6F822), black bold Courier New 12
 *   Row 3: Spacer
 *   Row 4: Headers     – black bg, white bold Courier New 11, centered
 *   Rows…: Data        – alternating white / #F4F4F0, thin black borders
 *   Summary rows       – right-aligned bold; BALANCE row = black label + green value
 */
object XlsxWriter {

    const val TYPE_NORMAL = 0
    const val TYPE_GREEN = 1
    const val TYPE_RED = 2

    // ── Style indices (must match cellXfs order in styles()) ──────────────────
    private const val S_DEFAULT   = 0
    private const val S_TITLE     = 1  // black fill, white bold 16, center
    private const val S_SUBTITLE  = 2  // green fill, black bold 12, center
    private const val S_HEADER    = 3  // black fill, white bold 11, center
    private const val S_DATA_ODD  = 4  // white fill, Courier 11, left, border
    private const val S_DATA_EVEN = 5  // #F4F4F0 fill, Courier 11, left, border
    private const val S_SUMMARY   = 6  // no fill, bold 12, right
    private const val S_BAL_LABEL = 7  // black fill, white bold 12, right, border
    private const val S_BAL_VALUE = 8  // green fill, black bold 12, right, border
    private const val S_DATA_ODD_GREEN  = 9
    private const val S_DATA_ODD_RED    = 10
    private const val S_DATA_EVEN_GREEN = 11
    private const val S_DATA_EVEN_RED   = 12

    fun create(
        title: String,
        subtitle: String,
        headers: List<String>,
        dataRows: List<List<Pair<String, Int>>>,
        summary: List<Pair<String, String>>   // label → value; rows with "BALANCE" get highlight
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        val zip  = ZipOutputStream(baos)
        fun add(name: String, content: String) {
            zip.putNextEntry(ZipEntry(name))
            zip.write(content.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        val colCount = headers.size
        add("[Content_Types].xml",          contentTypes())
        add("_rels/.rels",                  rels())
        add("xl/workbook.xml",              workbook())
        add("xl/_rels/workbook.xml.rels",   workbookRels())
        add("xl/styles.xml",               styles())
        add("xl/worksheets/sheet1.xml",    sheet(title, subtitle, headers, dataRows, summary, colCount))
        zip.finish()
        return baos.toByteArray()
    }

    // ── Column letter helper (0-indexed → "A", "B", … "AA") ──────────────────
    private fun col(index: Int): String {
        var n = index + 1; var r = ""
        while (n > 0) { r = ('A' + (n - 1) % 26) + r; n = (n - 1) / 26 }
        return r
    }

    private fun esc(s: String) = s
        .replace("&", "&amp;").replace("<", "&lt;")
        .replace(">", "&gt;").replace("\"", "&quot;")

    private fun cell(ref: String, value: String, s: Int) =
        if (value.isBlank()) "<c r=\"$ref\" s=\"$s\"/>"
        else "<c r=\"$ref\" s=\"$s\" t=\"inlineStr\"><is><t>${esc(value)}</t></is></c>"

    // ── Worksheet XML ─────────────────────────────────────────────────────────
    private fun sheet(
        title: String, subtitle: String,
        headers: List<String>, dataRows: List<List<Pair<String, Int>>>,
        summary: List<Pair<String, String>>, colCount: Int
    ): String {
        val sb  = StringBuilder()
        val end = col(colCount - 1)
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n")
        // Column widths
        sb.append("<cols>\n")
        for (i in 0 until colCount) {
            val w = if (i == 1) 42.0 else 20.0
            sb.append("<col min=\"${i+1}\" max=\"${i+1}\" width=\"$w\" customWidth=\"1\"/>\n")
        }
        sb.append("</cols>\n<sheetData>\n")
        var row = 1
        // Row 1 – Title
        sb.append("<row r=\"$row\" ht=\"30\" customHeight=\"1\">\n")
        sb.append(cell("A$row", title, S_TITLE))
        for (i in 1 until colCount) sb.append("<c r=\"${col(i)}$row\" s=\"$S_TITLE\"/>")
        sb.append("\n</row>\n"); row++
        // Row 2 – Subtitle
        sb.append("<row r=\"$row\" ht=\"25\" customHeight=\"1\">\n")
        sb.append(cell("A$row", subtitle, S_SUBTITLE))
        for (i in 1 until colCount) sb.append("<c r=\"${col(i)}$row\" s=\"$S_SUBTITLE\"/>")
        sb.append("\n</row>\n"); row++
        // Row 3 – Spacer
        sb.append("<row r=\"$row\"><c r=\"A$row\" s=\"$S_DEFAULT\"/></row>\n"); row++
        // Row 4 – Headers
        sb.append("<row r=\"$row\" ht=\"25\" customHeight=\"1\">\n")
        for (i in headers.indices) sb.append(cell("${col(i)}$row", headers[i], S_HEADER))
        sb.append("\n</row>\n"); row++
        // Data rows
        dataRows.forEachIndexed { idx, rowData ->
            val isOdd = idx % 2 == 0
            sb.append("<row r=\"$row\" ht=\"18\" customHeight=\"1\">\n")
            for (i in 0 until colCount) {
                val pair = rowData.getOrElse(i) { Pair("", TYPE_NORMAL) }
                val s = when (pair.second) {
                    TYPE_GREEN -> if (isOdd) S_DATA_ODD_GREEN else S_DATA_EVEN_GREEN
                    TYPE_RED -> if (isOdd) S_DATA_ODD_RED else S_DATA_EVEN_RED
                    else -> if (isOdd) S_DATA_ODD else S_DATA_EVEN
                }
                sb.append(cell("${col(i)}$row", pair.first, s))
            }
            sb.append("\n</row>\n"); row++
        }
        // Spacer before summary
        sb.append("<row r=\"$row\"><c r=\"A$row\" s=\"$S_DEFAULT\"/></row>\n"); row++
        // Summary rows
        summary.forEach { (label, value) ->
            val isBalance = label.contains("BALANCE", ignoreCase = true)
            val ls = if (isBalance) S_BAL_LABEL else S_SUMMARY
            val vs = if (isBalance) S_BAL_VALUE else S_SUMMARY
            sb.append("<row r=\"$row\" ht=\"20\" customHeight=\"1\">\n")
            for (i in 0 until colCount - 2) sb.append("<c r=\"${col(i)}$row\" s=\"$S_DEFAULT\"/>")
            sb.append(cell("${col(colCount - 2)}$row", label, ls))
            sb.append(cell("${col(colCount - 1)}$row", value, vs))
            sb.append("\n</row>\n"); row++
        }
        sb.append("</sheetData>\n")
        // Merged title + subtitle rows
        sb.append("<mergeCells count=\"2\"><mergeCell ref=\"A1:${end}1\"/><mergeCell ref=\"A2:${end}2\"/></mergeCells>\n")
        sb.append("</worksheet>")
        return sb.toString()
    }

    // ── Static XML parts ──────────────────────────────────────────────────────
    private fun contentTypes() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml"  ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml"          ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml"            ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

    private fun rels() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun workbook() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets><sheet name="Ledger" sheetId="1" r:id="rId1"/></sheets>
</workbook>"""

    private fun workbookRels() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles"   Target="styles.xml"/>
</Relationships>"""

    private fun styles() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="9">
    <font><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="16"/><name val="Courier New"/><color rgb="FFFFFFFF"/></font>
    <font><b/><sz val="12"/><name val="Courier New"/><color rgb="FF000000"/></font>
    <font><b/><sz val="11"/><name val="Courier New"/><color rgb="FFFFFFFF"/></font>
    <font><sz val="11"/><name val="Courier New"/><color rgb="FF000000"/></font>
    <font><b/><sz val="12"/><name val="Courier New"/><color rgb="FF000000"/></font>
    <font><b/><sz val="12"/><name val="Courier New"/><color rgb="FFFFFFFF"/></font>
    <font><b/><sz val="11"/><name val="Courier New"/><color rgb="FF059669"/></font>
    <font><b/><sz val="11"/><name val="Courier New"/><color rgb="FFDC2626"/></font>
  </fonts>
  <fills count="5">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF000000"/><bgColor indexed="64"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFC6F822"/><bgColor indexed="64"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFF4F4F0"/><bgColor indexed="64"/></patternFill></fill>
  </fills>
  <borders count="2">
    <border><left/><right/><top/><bottom/><diagonal/></border>
    <border>
      <left style="thin"><color rgb="FF000000"/></left>
      <right style="thin"><color rgb="FF000000"/></right>
      <top style="thin"><color rgb="FF000000"/></top>
      <bottom style="thin"><color rgb="FF000000"/></bottom>
      <diagonal/>
    </border>
  </borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  <cellXfs count="13">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="middle"/></xf>
    <xf numFmtId="0" fontId="2" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="middle"/></xf>
    <xf numFmtId="0" fontId="3" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="middle"/></xf>
    <xf numFmtId="0" fontId="4" fillId="0" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="left" vertical="middle"/></xf>
    <xf numFmtId="0" fontId="4" fillId="4" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="left" vertical="middle"/></xf>
    <xf numFmtId="0" fontId="5" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1"><alignment horizontal="right" vertical="middle"/></xf>
    <xf numFmtId="0" fontId="6" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="middle"/></xf>
    <xf numFmtId="0" fontId="5" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="middle"/></xf>
    <xf numFmtId="0" fontId="7" fillId="0" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="left" vertical="middle"/></xf>
    <xf numFmtId="0" fontId="8" fillId="0" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="left" vertical="middle"/></xf>
    <xf numFmtId="0" fontId="7" fillId="4" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="left" vertical="middle"/></xf>
    <xf numFmtId="0" fontId="8" fillId="4" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="left" vertical="middle"/></xf>
  </cellXfs>
  <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
</styleSheet>"""
}
