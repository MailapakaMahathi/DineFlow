
package com.dineflow;

import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

public class InvoicePdfGenerator {

    public static void generateInvoice(
            String filePath,
            int orderId,
            String customerName,
            List<CartLine> cart,
            double subtotal,
            double tax,
            double discount,
            double grandTotal,
            String paymentMethod) {

        Document document = new Document();

        try {

            PdfWriter.getInstance(
                    document,
                    new FileOutputStream(filePath)
            );

            document.open();

            // Fonts
            Font titleFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD, 20
            );

            Font headingFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD, 12
            );

            Font normalFont = FontFactory.getFont(
                    FontFactory.HELVETICA, 10
            );

            // Restaurant heading
            Paragraph title = new Paragraph(
                    "DINEFLOW RESTAURANT",
                    titleFont
            );

            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph(
                    "Customer Invoice",
                    headingFont
            );

            subtitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitle);

            document.add(new Paragraph(" "));

            // Invoice details
            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern(
                            "dd-MM-yyyy HH:mm:ss"
                    );

            document.add(new Paragraph(
                    "Invoice Number: " + orderId,
                    normalFont
            ));

            document.add(new Paragraph(
                    "Customer Name: " + customerName,
                    normalFont
            ));

            document.add(new Paragraph(
                    "Date: " + LocalDateTime.now().format(formatter),
                    normalFont
            ));

            document.add(new Paragraph(
                    "Payment Method: " + paymentMethod,
                    normalFont
            ));

            document.add(new Paragraph(" "));

            // Food items table
            PdfPTable table = new PdfPTable(4);

            table.setWidthPercentage(100);

            table.setWidths(new float[]{
                    3f, 1.5f, 1.5f, 2f
            });

            addCell(table, "Food Item", headingFont);
            addCell(table, "Quantity", headingFont);
            addCell(table, "Price", headingFont);
            addCell(table, "Total", headingFont);

            // Add cart items
            for (CartLine line : cart) {

                addCell(
                        table,
                        line.getMenuItem().getItemName(),
                        normalFont
                );

                addCell(
                        table,
                        String.valueOf(line.getQuantity()),
                        normalFont
                );

                addCell(
                        table,
                        String.format("%.2f",
                                line.getMenuItem().getPrice()),
                        normalFont
                );

                addCell(
                        table,
                        String.format("%.2f",
                                line.getLineTotal()),
                        normalFont
                );
            }

            document.add(table);

            document.add(new Paragraph(" "));

            // Bill summary
            document.add(new Paragraph(
                    "Subtotal: Rs. " +
                    String.format("%.2f", subtotal),
                    normalFont
            ));

            document.add(new Paragraph(
                    "Tax: Rs. " +
                    String.format("%.2f", tax),
                    normalFont
            ));

            document.add(new Paragraph(
                    "Discount: Rs. " +
                    String.format("%.2f", discount),
                    normalFont
            ));

            Paragraph total = new Paragraph(
                    "GRAND TOTAL: Rs. " +
                    String.format("%.2f", grandTotal),
                    headingFont
            );

            total.setAlignment(Element.ALIGN_RIGHT);
            document.add(total);

            document.add(new Paragraph(" "));

            Paragraph footer = new Paragraph(
                    "Thank you for visiting DineFlow!",
                    headingFont
            );

            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();

            System.out.println(
                    "PDF invoice generated successfully: " + filePath
            );

        } catch (Exception e) {

            if (document.isOpen()) {
                document.close();
            }

            System.out.println("PDF generation failed!");
            e.printStackTrace();
        }
    }

    // Reusable table cell method
    private static void addCell(
            PdfPTable table,
            String text,
            Font font) {

        PdfPCell cell = new PdfPCell(
                new Phrase(text, font)
        );

        cell.setPadding(8);

        table.addCell(cell);
    }

}
