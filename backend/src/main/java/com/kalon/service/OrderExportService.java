package com.kalon.service;

import com.kalon.entity.Order;
import com.kalon.entity.OrderItem;
import com.kalon.entity.ShippingAddress;
import com.kalon.repository.OrderRepository;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderExportService {

    private final OrderRepository orderRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter FILE_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Transactional(readOnly = true)
    public List<Order> getFilteredOrders(Order.OrderStatus status,
                                          LocalDateTime dateFrom,
                                          LocalDateTime dateTo,
                                          BigDecimal minAmount,
                                          BigDecimal maxAmount) {
        return orderRepository.findAllWithFilters(status, dateFrom, dateTo, minAmount, maxAmount);
    }

    public String generateFilename(String format, Order.OrderStatus status) {
        String datePart = LocalDateTime.now().format(FILE_DATE_FMT);
        String statusPart = (status != null) ? "_" + status.name() : "";
        return "orders_" + datePart + statusPart + "." + format;
    }

    // ==================== CSV ====================

    public byte[] generateCsv(List<Order> orders) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(baos);

        writer.println(String.join(",",
                "Order Number", "Date", "Customer Name", "Phone", "Status",
                "Payment Status", "Payment Method", "Items Count",
                "Subtotal", "Shipping", "Tax", "Discount", "Total Amount",
                "Address", "City", "State", "Postal Code", "Tracking Number"
        ));

        for (Order order : orders) {
            ShippingAddress sa = order.getShippingAddress();
            writer.println(String.join(",",
                    csvEscape(order.getOrderNumber()),
                    csvEscape(order.getCreatedAt() != null ? order.getCreatedAt().format(DATE_FMT) : ""),
                    csvEscape(sa != null ? sa.getFullName() : ""),
                    csvEscape(sa != null ? sa.getPhoneNumber() : ""),
                    csvEscape(order.getStatus().name()),
                    csvEscape(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : ""),
                    csvEscape(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : ""),
                    String.valueOf(order.getItems().size()),
                    order.getSubtotal().toPlainString(),
                    order.getShippingCost().toPlainString(),
                    order.getTaxAmount().toPlainString(),
                    order.getDiscountAmount().toPlainString(),
                    order.getTotalAmount().toPlainString(),
                    csvEscape(formatAddress(sa)),
                    csvEscape(sa != null ? sa.getCity() : ""),
                    csvEscape(sa != null ? sa.getState() : ""),
                    csvEscape(sa != null ? sa.getPostalCode() : ""),
                    csvEscape(order.getTrackingNumber() != null ? order.getTrackingNumber() : "")
            ));
        }

        writer.flush();
        writer.close();
        return baos.toByteArray();
    }

    // ==================== EXCEL ====================

    public byte[] generateExcel(List<Order> orders) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Styles
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("#,##0.00"));

            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(format.getFormat("yyyy-mm-dd hh:mm"));

            // --- Sheet 1: Orders Summary ---
            Sheet ordersSheet = workbook.createSheet("Orders");
            String[] orderHeaders = {
                    "Order Number", "Date", "Customer Name", "Phone",
                    "Status", "Payment Status", "Payment Method", "Items Count",
                    "Subtotal", "Shipping", "Tax", "Discount", "Total Amount",
                    "City", "State", "Tracking Number"
            };

            Row headerRow = ordersSheet.createRow(0);
            for (int i = 0; i < orderHeaders.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(orderHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (Order order : orders) {
                Row row = ordersSheet.createRow(rowNum++);
                ShippingAddress sa = order.getShippingAddress();
                int col = 0;

                row.createCell(col++).setCellValue(order.getOrderNumber());

                Cell dateCell = row.createCell(col++);
                if (order.getCreatedAt() != null) {
                    dateCell.setCellValue(java.sql.Timestamp.valueOf(order.getCreatedAt()));
                    dateCell.setCellStyle(dateStyle);
                }

                row.createCell(col++).setCellValue(sa != null ? sa.getFullName() : "");
                row.createCell(col++).setCellValue(sa != null ? sa.getPhoneNumber() : "");
                row.createCell(col++).setCellValue(order.getStatus().name());
                row.createCell(col++).setCellValue(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : "");
                row.createCell(col++).setCellValue(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : "");
                row.createCell(col++).setCellValue(order.getItems().size());

                setCurrencyCell(row, col++, order.getSubtotal(), currencyStyle);
                setCurrencyCell(row, col++, order.getShippingCost(), currencyStyle);
                setCurrencyCell(row, col++, order.getTaxAmount(), currencyStyle);
                setCurrencyCell(row, col++, order.getDiscountAmount(), currencyStyle);
                setCurrencyCell(row, col++, order.getTotalAmount(), currencyStyle);

                row.createCell(col++).setCellValue(sa != null ? sa.getCity() : "");
                row.createCell(col++).setCellValue(sa != null ? sa.getState() : "");
                row.createCell(col++).setCellValue(order.getTrackingNumber() != null ? order.getTrackingNumber() : "");
            }

            for (int i = 0; i < orderHeaders.length; i++) {
                ordersSheet.autoSizeColumn(i);
            }

            // --- Sheet 2: Order Items Detail ---
            Sheet itemsSheet = workbook.createSheet("Order Items");
            String[] itemHeaders = {
                    "Order Number", "Product Name", "Size", "Color",
                    "Quantity", "Unit Price", "Item Total"
            };

            Row itemHeaderRow = itemsSheet.createRow(0);
            for (int i = 0; i < itemHeaders.length; i++) {
                Cell cell = itemHeaderRow.createCell(i);
                cell.setCellValue(itemHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            int itemRowNum = 1;
            for (Order order : orders) {
                for (OrderItem item : order.getItems()) {
                    Row row = itemsSheet.createRow(itemRowNum++);
                    int col = 0;
                    row.createCell(col++).setCellValue(order.getOrderNumber());
                    row.createCell(col++).setCellValue(item.getProductName());
                    row.createCell(col++).setCellValue(item.getSize() != null ? item.getSize() : "");
                    row.createCell(col++).setCellValue(item.getColor() != null ? item.getColor() : "");
                    row.createCell(col++).setCellValue(item.getQuantity());
                    setCurrencyCell(row, col++, item.getPrice(), currencyStyle);
                    setCurrencyCell(row, col++, item.getTotalPrice(), currencyStyle);
                }
            }

            for (int i = 0; i < itemHeaders.length; i++) {
                itemsSheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    // ==================== PDF ====================

    public byte[] generatePdf(List<Order> orders) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Title
            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(
                    com.lowagie.text.Font.HELVETICA, 18, com.lowagie.text.Font.BOLD,
                    new Color(102, 126, 234));
            Paragraph title = new Paragraph("KALON - Orders Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(5);
            document.add(title);

            // Meta info
            com.lowagie.text.Font metaFont = new com.lowagie.text.Font(
                    com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.NORMAL, Color.GRAY);
            Paragraph meta = new Paragraph(
                    "Generated: " + LocalDateTime.now().format(DATE_FMT) +
                            "  |  Total Orders: " + orders.size(), metaFont);
            meta.setAlignment(Element.ALIGN_CENTER);
            meta.setSpacingAfter(15);
            document.add(meta);

            // Table
            PdfPTable table = new PdfPTable(10);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{12, 10, 14, 8, 9, 9, 8, 8, 10, 12});

            String[] headers = {
                    "Order #", "Date", "Customer", "Items",
                    "Status", "Payment", "Subtotal", "Shipping",
                    "Total", "City"
            };

            com.lowagie.text.Font pdfHeaderFont = new com.lowagie.text.Font(
                    com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.BOLD, Color.WHITE);
            Color headerBg = new Color(102, 126, 234);

            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, pdfHeaderFont));
                cell.setBackgroundColor(headerBg);
                cell.setPadding(6);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            com.lowagie.text.Font cellFont = new com.lowagie.text.Font(
                    com.lowagie.text.Font.HELVETICA, 8, com.lowagie.text.Font.NORMAL);
            Color altBg = new Color(245, 247, 255);

            for (int i = 0; i < orders.size(); i++) {
                Order order = orders.get(i);
                ShippingAddress sa = order.getShippingAddress();
                Color rowBg = (i % 2 == 0) ? Color.WHITE : altBg;

                addPdfCell(table, order.getOrderNumber(), cellFont, rowBg);
                addPdfCell(table, order.getCreatedAt() != null ?
                        order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : "", cellFont, rowBg);
                addPdfCell(table, sa != null ? sa.getFullName() : "", cellFont, rowBg);
                addPdfCell(table, String.valueOf(order.getItems().size()), cellFont, rowBg);
                addPdfCell(table, order.getStatus().name(), cellFont, rowBg);
                addPdfCell(table, order.getPaymentStatus() != null ? order.getPaymentStatus().name() : "", cellFont, rowBg);
                addPdfCell(table, "Rs." + order.getSubtotal().toPlainString(), cellFont, rowBg);
                addPdfCell(table, "Rs." + order.getShippingCost().toPlainString(), cellFont, rowBg);
                addPdfCell(table, "Rs." + order.getTotalAmount().toPlainString(), cellFont, rowBg);
                addPdfCell(table, sa != null ? sa.getCity() : "", cellFont, rowBg);
            }

            document.add(table);

            // Footer with grand total
            Paragraph footer = new Paragraph();
            footer.setSpacingBefore(15);
            com.lowagie.text.Font footerFont = new com.lowagie.text.Font(
                    com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.BOLD);
            BigDecimal grandTotal = orders.stream()
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            footer.add(new Chunk("Grand Total: Rs." + grandTotal.toPlainString(), footerFont));
            footer.setAlignment(Element.ALIGN_RIGHT);
            document.add(footer);

        } catch (DocumentException e) {
            throw new IOException("Failed to generate PDF", e);
        } finally {
            document.close();
        }

        return baos.toByteArray();
    }

    // ==================== Helpers ====================

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String formatAddress(ShippingAddress sa) {
        if (sa == null) return "";
        StringBuilder sb = new StringBuilder();
        if (sa.getAddressLine1() != null) sb.append(sa.getAddressLine1());
        if (sa.getAddressLine2() != null && !sa.getAddressLine2().isEmpty()) {
            sb.append(", ").append(sa.getAddressLine2());
        }
        return sb.toString();
    }

    private void setCurrencyCell(Row row, int col, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
            cell.setCellStyle(style);
        }
    }

    private void addPdfCell(PdfPTable table, String text, com.lowagie.text.Font font, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setBackgroundColor(bgColor);
        cell.setPadding(5);
        cell.setBorderColor(new Color(220, 220, 220));
        table.addCell(cell);
    }
}
