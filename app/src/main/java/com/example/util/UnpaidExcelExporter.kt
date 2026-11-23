package com.example.util

import com.example.data.PaymentStatus
import com.example.data.SaleWithPayments
import com.example.ui.CustomerDebtSummary
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object UnpaidExcelExporter {

    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    fun buildUnpaidWorkbookBytes(
        customerDebts: List<CustomerDebtSummary>,
        unpaidSales: List<SaleWithPayments>,
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            writeZipEntry(zos, "[Content_Types].xml", contentTypesXml())
            writeZipEntry(zos, "_rels/.rels", rootRelsXml())
            writeZipEntry(zos, "xl/workbook.xml", workbookXml())
            writeZipEntry(zos, "xl/_rels/workbook.xml.rels", workbookRelsXml())
            writeZipEntry(zos, "xl/styles.xml", stylesXml())
            writeZipEntry(zos, "xl/worksheets/sheet1.xml", buildCustomersSheetXml(customerDebts))
            writeZipEntry(zos, "xl/worksheets/sheet2.xml", buildInvoicesSheetXml(unpaidSales))
        }
        return baos.toByteArray()
    }

    private fun writeZipEntry(zos: ZipOutputStream, name: String, content: String) {
        zos.putNextEntry(ZipEntry(name))
        zos.write(content.toByteArray(Charsets.UTF_8))
        zos.closeEntry()
    }

    private fun buildCustomersSheetXml(customerDebts: List<CustomerDebtSummary>): String {
        val sb = StringBuilder(4096)
        sb.append(SHEET_HEADER_PREFIX)
        sb.append(
            """
            <cols>
              <col min="1" max="1" width="8" customWidth="1"/>
              <col min="2" max="2" width="26" customWidth="1"/>
              <col min="3" max="3" width="16" customWidth="1"/>
              <col min="4" max="4" width="18" customWidth="1"/>
              <col min="5" max="5" width="16" customWidth="1"/>
              <col min="6" max="6" width="18" customWidth="1"/>
              <col min="7" max="7" width="20" customWidth="1"/>
            </cols>
            <sheetData>
            """.trimIndent()
        )

        var rowNum = 1
        sb.append(
            buildRowXml(
                rowNum++,
                listOf(
                    CellSpec.Str("#", style = 1),
                    CellSpec.Str("اسم العميل", style = 1),
                    CellSpec.Str("الفواتير المفتوحة", style = 1),
                    CellSpec.Str("إجمالي الفواتير (₪)", style = 1),
                    CellSpec.Str("المسدّد (₪)", style = 1),
                    CellSpec.Str("المتبقي (₪)", style = 1),
                    CellSpec.Str("أقدم فاتورة", style = 1),
                ),
            )
        )

        customerDebts.forEachIndexed { idx, customer ->
            sb.append(
                buildRowXml(
                    rowNum++,
                    listOf(
                        CellSpec.Num((idx + 1).toDouble(), style = 2),
                        CellSpec.Str(customer.customerName, style = 2),
                        CellSpec.Num(customer.openSalesCount.toDouble(), style = 2),
                        CellSpec.Num(customer.totalAmount, style = 3),
                        CellSpec.Num(customer.totalPaid, style = 3),
                        CellSpec.Num(customer.totalRemaining, style = 3),
                        CellSpec.Str(dateTimeFormat.format(Date(customer.oldestSaleMillis)), style = 2),
                    ),
                )
            )
        }

        // Totals Row
        sb.append(
            buildRowXml(
                rowNum,
                listOf(
                    CellSpec.Str("", style = 4),
                    CellSpec.Str("الإجمالي الكلي", style = 4),
                    CellSpec.Num(customerDebts.sumOf { it.openSalesCount }.toDouble(), style = 4),
                    CellSpec.Num(customerDebts.sumOf { it.totalAmount }, style = 5),
                    CellSpec.Num(customerDebts.sumOf { it.totalPaid }, style = 5),
                    CellSpec.Num(customerDebts.sumOf { it.totalRemaining }, style = 5),
                    CellSpec.Str("", style = 4),
                ),
            )
        )

        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private fun buildInvoicesSheetXml(unpaidSales: List<SaleWithPayments>): String {
        val sb = StringBuilder(4096)
        sb.append(SHEET_HEADER_PREFIX)
        sb.append(
            """
            <cols>
              <col min="1" max="1" width="10" customWidth="1"/>
              <col min="2" max="2" width="24" customWidth="1"/>
              <col min="3" max="3" width="20" customWidth="1"/>
              <col min="4" max="4" width="16" customWidth="1"/>
              <col min="5" max="5" width="16" customWidth="1"/>
              <col min="6" max="6" width="16" customWidth="1"/>
              <col min="7" max="7" width="14" customWidth="1"/>
              <col min="8" max="8" width="26" customWidth="1"/>
            </cols>
            <sheetData>
            """.trimIndent()
        )

        var rowNum = 1
        sb.append(
            buildRowXml(
                rowNum++,
                listOf(
                    CellSpec.Str("رقم", style = 1),
                    CellSpec.Str("اسم العميل", style = 1),
                    CellSpec.Str("التاريخ والوقت", style = 1),
                    CellSpec.Str("المبلغ الإجمالي (₪)", style = 1),
                    CellSpec.Str("المسدّد (₪)", style = 1),
                    CellSpec.Str("المتبقي (₪)", style = 1),
                    CellSpec.Str("الحالة", style = 1),
                    CellSpec.Str("ملاحظة", style = 1),
                ),
            )
        )

        unpaidSales.forEach { item ->
            val statusText = when (item.status) {
                PaymentStatus.PAID -> "مسدد"
                PaymentStatus.PARTIAL -> "دفعة جزئية"
                PaymentStatus.UNPAID -> "غير مسدد"
            }
            sb.append(
                buildRowXml(
                    rowNum++,
                    listOf(
                        CellSpec.Num(item.sale.id.toDouble(), style = 2),
                        CellSpec.Str(item.sale.customerName, style = 2),
                        CellSpec.Str(dateTimeFormat.format(Date(item.sale.createdAtMillis)), style = 2),
                        CellSpec.Num(item.sale.totalAmount, style = 3),
                        CellSpec.Num(item.paidAmount, style = 3),
                        CellSpec.Num(item.remainingAmount, style = 3),
                        CellSpec.Str(statusText, style = 2),
                        CellSpec.Str(item.sale.note, style = 2),
                    ),
                )
            )
        }

        sb.append(
            buildRowXml(
                rowNum,
                listOf(
                    CellSpec.Str("", style = 4),
                    CellSpec.Str("الإجمالي الكلي", style = 4),
                    CellSpec.Str("${unpaidSales.size} فاتورة", style = 4),
                    CellSpec.Num(unpaidSales.sumOf { it.sale.totalAmount }, style = 5),
                    CellSpec.Num(unpaidSales.sumOf { it.paidAmount }, style = 5),
                    CellSpec.Num(unpaidSales.sumOf { it.remainingAmount }, style = 5),
                    CellSpec.Str("", style = 4),
                    CellSpec.Str("", style = 4),
                ),
            )
        )

        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private sealed class CellSpec(val style: Int) {
        class Str(val value: String, style: Int) : CellSpec(style)
        class Num(val value: Double, style: Int) : CellSpec(style)
    }

    private fun buildRowXml(rowNum: Int, cells: List<CellSpec>): String {
        val sb = StringBuilder(256)
        sb.append("<row r=\"").append(rowNum).append("\">")
        cells.forEachIndexed { colIdx, cell ->
            val ref = "${colLetter(colIdx)}$rowNum"
            when (cell) {
                is CellSpec.Str -> {
                    sb.append("<c r=\"").append(ref).append("\" t=\"inlineStr\" s=\"").append(cell.style)
                        .append("\"><is><t>").append(escapeXml(cell.value)).append("</t></is></c>")
                }
                is CellSpec.Num -> {
                    val safeNum = if (cell.value.isFinite()) cell.value else 0.0
                    sb.append("<c r=\"").append(ref).append("\" s=\"").append(cell.style)
                        .append("\"><v>").append(safeNum).append("</v></c>")
                }
            }
        }
        sb.append("</row>")
        return sb.toString()
    }

    private fun colLetter(index: Int): String = ('A' + index).toString()

    private fun escapeXml(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            when (ch) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&apos;")
                else -> {
                    // Strip illegal XML 1.0 control characters
                    if (ch == '\t' || ch == '\n' || ch == '\r' || ch.code >= 0x20) {
                        sb.append(ch)
                    }
                }
            }
        }
        return sb.toString()
    }

    private const val SHEET_HEADER_PREFIX =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<sheetViews><sheetView workbookViewId="0" rightToLeft="1"/></sheetViews>"""

    private fun contentTypesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""

    private fun rootRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun workbookXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="ملخص العملاء" sheetId="1" r:id="rId1"/>
    <sheet name="تفاصيل الفواتير" sheetId="2" r:id="rId2"/>
  </sheets>
</workbook>"""

    private fun workbookRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    // Styles:
    // 0: default
    // 1: header (bold white on #0F766E)
    // 2: body text/int
    // 3: body currency (#,##0.00)
    // 4: total label (bold on #CCFBF1)
    // 5: total currency (bold #,##0.00 on #CCFBF1)
    private fun stylesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <numFmts count="1">
    <numFmt numFmtId="164" formatCode="#,##0.00"/>
  </numFmts>
  <fonts count="3">
    <font><sz val="11"/><name val="Arial"/></font>
    <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Arial"/></font>
    <font><b/><sz val="11"/><color rgb="FF064E3B"/><name val="Arial"/></font>
  </fonts>
  <fills count="4">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF0F766E"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFCCFBF1"/></patternFill></fill>
  </fills>
  <borders count="2">
    <border/>
    <border>
      <left style="thin"><color rgb="FFCBD5E1"/></left>
      <right style="thin"><color rgb="FFCBD5E1"/></right>
      <top style="thin"><color rgb="FFCBD5E1"/></top>
      <bottom style="thin"><color rgb="FFCBD5E1"/></bottom>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="6">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>
    <xf numFmtId="164" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>
    <xf numFmtId="0" fontId="2" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>
    <xf numFmtId="164" fontId="2" fillId="3" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>
  </cellXfs>
</styleSheet>"""
}
