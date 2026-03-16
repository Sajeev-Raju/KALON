package com.kalon.service;

import com.kalon.entity.Order;
import com.kalon.entity.OrderItem;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

@Service
@Slf4j
public class InvoiceService {

    @Value("${app.company.name:KALON}")
    private String companyName;

    @Value("${app.company.address:India}")
    private String companyAddress;

    @Value("${app.company.gstin:}")
    private String companyGstin;

    public byte[] generateInvoicePdf(Order order) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, baos);
            document.open();

            // Fonts
            Font titleFont = new Font(Font.HELVETICA, 22, Font.BOLD, new Color(45, 45, 45));
            Font headerFont = new Font(Font.HELVETICA, 11, Font.BOLD, new Color(45, 45, 45));
            Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(80, 80, 80));
            Font smallFont = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(120, 120, 120));

            // Title
            Paragraph title = new Paragraph(companyName, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph invoiceTitle = new Paragraph("TAX INVOICE", new Font(Font.HELVETICA, 14, Font.BOLD, new Color(100, 100, 100)));
            invoiceTitle.setAlignment(Element.ALIGN_CENTER);
            invoiceTitle.setSpacingAfter(20);
            document.add(invoiceTitle);

            // Company and Order info table
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(20);

            // Left: Company Info
            PdfPCell companyCell = new PdfPCell();
            companyCell.setBorder(Rectangle.NO_BORDER);
            companyCell.addElement(new Paragraph("From:", headerFont));
            companyCell.addElement(new Paragraph(companyName, normalFont));
            companyCell.addElement(new Paragraph(companyAddress, normalFont));
            if (companyGstin != null && !companyGstin.isEmpty()) {
                companyCell.addElement(new Paragraph("GSTIN: " + companyGstin, normalFont));
            }
            infoTable.addCell(companyCell);

            // Right: Order Info
            PdfPCell orderInfoCell = new PdfPCell();
            orderInfoCell.setBorder(Rectangle.NO_BORDER);
            orderInfoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            orderInfoCell.addElement(new Paragraph("Invoice #: " + order.getOrderNumber(), headerFont));
            orderInfoCell.addElement(new Paragraph("Date: " + order.getCreatedAt().toLocalDate(), normalFont));
            orderInfoCell.addElement(new Paragraph("Payment: " + order.getPaymentMethod(), normalFont));
            orderInfoCell.addElement(new Paragraph("Status: " + order.getPaymentStatus(), normalFont));
            infoTable.addCell(orderInfoCell);

            document.add(infoTable);

            // Shipping Address
            if (order.getShippingAddress() != null) {
                Paragraph shipTo = new Paragraph("Ship To:", headerFont);
                shipTo.setSpacingAfter(5);
                document.add(shipTo);

                var sa = order.getShippingAddress();
                document.add(new Paragraph(sa.getFullName(), normalFont));
                document.add(new Paragraph(sa.getAddressLine1(), normalFont));
                if (sa.getAddressLine2() != null && !sa.getAddressLine2().isEmpty()) {
                    document.add(new Paragraph(sa.getAddressLine2(), normalFont));
                }
                Paragraph cityLine = new Paragraph(sa.getCity() + ", " + sa.getState() + " " + sa.getPostalCode(), normalFont);
                cityLine.setSpacingAfter(15);
                document.add(cityLine);
            }

            // Items Table
            PdfPTable itemsTable = new PdfPTable(new float[]{4, 1.5f, 1, 1.5f, 2});
            itemsTable.setWidthPercentage(100);
            itemsTable.setSpacingAfter(15);

            // Header row
            Color headerBg = new Color(45, 45, 45);
            Font headerWhite = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
            String[] headers = {"Product", "Size/Color", "Qty", "Price", "Total"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerWhite));
                cell.setBackgroundColor(headerBg);
                cell.setPadding(8);
                itemsTable.addCell(cell);
            }

            // Item rows
            for (OrderItem item : order.getItems()) {
                itemsTable.addCell(createCell(item.getProductName(), normalFont));
                String variant = (item.getSize() != null ? item.getSize() : "") +
                        (item.getColor() != null ? " / " + item.getColor() : "");
                itemsTable.addCell(createCell(variant, normalFont));
                itemsTable.addCell(createCell(String.valueOf(item.getQuantity()), normalFont));
                itemsTable.addCell(createCell("\u20B9" + item.getPrice(), normalFont));
                itemsTable.addCell(createCell("\u20B9" + item.getTotalPrice(), normalFont));
            }

            document.add(itemsTable);

            // Totals
            PdfPTable totalsTable = new PdfPTable(2);
            totalsTable.setWidthPercentage(40);
            totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            addTotalRow(totalsTable, "Subtotal:", "\u20B9" + order.getSubtotal(), normalFont);
            addTotalRow(totalsTable, "Shipping:", "\u20B9" + order.getShippingCost(), normalFont);

            if (order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                addTotalRow(totalsTable, "Discount:", "-\u20B9" + order.getDiscountAmount(), normalFont);
            }
            if (order.getTaxAmount() != null && order.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
                addTotalRow(totalsTable, "GST (incl.):", "\u20B9" + order.getTaxAmount(), normalFont);
            }

            // Total row with bold
            PdfPCell totalLabel = new PdfPCell(new Phrase("Total:", headerFont));
            totalLabel.setBorder(Rectangle.TOP);
            totalLabel.setPadding(6);
            totalsTable.addCell(totalLabel);
            PdfPCell totalValue = new PdfPCell(new Phrase("\u20B9" + order.getTotalAmount(), headerFont));
            totalValue.setBorder(Rectangle.TOP);
            totalValue.setPadding(6);
            totalValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalsTable.addCell(totalValue);

            document.add(totalsTable);

            // Footer
            Paragraph footer = new Paragraph("Thank you for shopping with " + companyName + "!", smallFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(30);
            document.add(footer);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate invoice PDF for order {}: {}", order.getOrderNumber(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate invoice");
        }
    }

    private PdfPCell createCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6);
        cell.setBorderColor(new Color(200, 200, 200));
        return cell;
    }

    private void addTotalRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(4);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, font));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(4);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }
}
