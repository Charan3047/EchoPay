package com.example.upiexpensetracker

import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Writes a small Excel-compatible Open XML workbook without exporting notification text. */
object TransactionSpreadsheetExporter {
    private val headers = listOf(
        "Transaction ID",
        "Date and time",
        "Amount (INR)",
        "Type",
        "Merchant",
        "Payment method",
        "Category",
        "Confidence",
        "Needs review"
    )

    fun write(output: OutputStream, transactions: List<TransactionEntity>) {
        val zip = ZipOutputStream(output)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
        try {
            zip.writeEntry("[Content_Types].xml", contentTypes)
            zip.writeEntry("_rels/.rels", packageRelationships)
            zip.writeEntry("xl/workbook.xml", workbook)
            zip.writeEntry("xl/_rels/workbook.xml.rels", workbookRelationships)
            zip.writeEntry("xl/styles.xml", styles)
            zip.writeEntry("xl/worksheets/sheet1.xml", sheet(transactions, dateFormat))
            zip.finish()
            zip.flush()
        } catch (exception: Exception) {
            runCatching { zip.close() }
            throw exception
        }
    }

    private fun sheet(
        transactions: List<TransactionEntity>,
        dateFormat: SimpleDateFormat
    ): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
        append("<sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>")
        append("<cols>")
        listOf(15, 21, 16, 15, 30, 18, 20, 14, 15).forEachIndexed { index, width ->
            append("<col min=\"${index + 1}\" max=\"${index + 1}\" width=\"$width\" customWidth=\"1\"/>")
        }
        append("</cols><sheetData>")
        append("<row r=\"1\">")
        headers.forEachIndexed { index, header -> append(stringCell(index, 1, header, style = 1)) }
        append("</row>")

        transactions.forEachIndexed { index, transaction ->
            val row = index + 2
            append("<row r=\"$row\">")
            append(numberCell(0, row, transaction.id.toString()))
            append(stringCell(1, row, dateFormat.format(Date(transaction.timestamp))))
            append(numberCell(2, row, transaction.amount.toString(), style = 2))
            append(stringCell(3, row, transaction.transactionType))
            append(stringCell(4, row, transaction.merchant.orEmpty()))
            append(stringCell(5, row, transaction.paymentMethod.orEmpty()))
            append(stringCell(6, row, transaction.category))
            append(numberCell(7, row, transaction.confidence.toString(), style = 3))
            append(booleanCell(8, row, transaction.needsReview))
            append("</row>")
        }
        append("</sheetData><autoFilter ref=\"A1:I${transactions.size + 1}\"/></worksheet>")
    }

    private fun stringCell(column: Int, row: Int, value: String, style: Int? = null): String {
        val styleAttribute = style?.let { " s=\"$it\"" }.orEmpty()
        val safeValue = value
            .filter { it == '\t' || it == '\n' || it == '\r' || it >= ' ' }
            .xmlEscape()
        return "<c r=\"${columnName(column)}$row\" t=\"inlineStr\"$styleAttribute><is><t xml:space=\"preserve\">$safeValue</t></is></c>"
    }

    private fun numberCell(column: Int, row: Int, value: String, style: Int? = null): String {
        val styleAttribute = style?.let { " s=\"$it\"" }.orEmpty()
        return "<c r=\"${columnName(column)}$row\"$styleAttribute><v>${value.xmlEscape()}</v></c>"
    }

    private fun booleanCell(column: Int, row: Int, value: Boolean): String =
        "<c r=\"${columnName(column)}$row\" t=\"b\"><v>${if (value) 1 else 0}</v></c>"

    private fun columnName(index: Int): String {
        var value = index + 1
        val name = StringBuilder()
        while (value > 0) {
            val remainder = (value - 1) % 26
            name.insert(0, ('A'.code + remainder).toChar())
            value = (value - 1) / 26
        }
        return name.toString()
    }

    private fun String.xmlEscape(): String = buildString(length) {
        for (character in this@xmlEscape) {
            append(
                when (character) {
                    '&' -> "&amp;"
                    '<' -> "&lt;"
                    '>' -> "&gt;"
                    '"' -> "&quot;"
                    '\'' -> "&apos;"
                    else -> character.toString()
                }
            )
        }
    }

    private fun ZipOutputStream.writeEntry(path: String, content: String) {
        putNextEntry(ZipEntry(path))
        write(content.toByteArray(StandardCharsets.UTF_8))
        closeEntry()
    }

    private val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
          <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
          <Default Extension="xml" ContentType="application/xml"/>
          <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
          <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
          <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
        </Types>""".trimIndent()

    private val packageRelationships = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
        </Relationships>""".trimIndent()

    private val workbook = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
          <sheets><sheet name="Transactions" sheetId="1" r:id="rId1"/></sheets>
        </workbook>""".trimIndent()

    private val workbookRelationships = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
          <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
        </Relationships>""".trimIndent()

    private val styles = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
          <numFmts count="2">
            <numFmt numFmtId="164" formatCode="&quot;₹&quot;#,##0.00"/>
            <numFmt numFmtId="165" formatCode="0.0%"/>
          </numFmts>
          <fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts>
          <fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills>
          <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
          <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
          <cellXfs count="4">
            <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
            <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
            <xf numFmtId="164" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
            <xf numFmtId="165" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
          </cellXfs>
          <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
        </styleSheet>""".trimIndent()
}
