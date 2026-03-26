package com.mphoYanga.scheduler.services;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.mphoYanga.scheduler.models.Quotation;
import com.mphoYanga.scheduler.models.QuotationDocument;
import com.mphoYanga.scheduler.models.QuotationItem;
import com.mphoYanga.scheduler.repos.QuotationDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Generates a professional PDF quotation document using iText 5,
 * saves it to the local filesystem, and tracks it in the database.
 *
 * ── HOW TO CHANGE THE LAYOUT ──────────────────────────────────────────────
 *  All visual constants are at the top of this class.
 *  Each PDF section is a private helper — rearrange or extend freely:
 *
 *   Column widths   → TABLE_COL_WIDTHS  (must sum to 100)
 *   Add a column    → extend addItemTable()
 *   Colours         → COLOUR_* constants
 *   Terms text      → TERMS_TEXT constant
 *   Header design   → HeaderFooterEvent.drawHeader()
 *   New section     → add a method, call it inside buildPdfBytes()
 *   Storage folder  → quotation.pdf-dir in application.properties
 * ─────────────────────────────────────────────────────────────────────────
 */
@Service
@RequiredArgsConstructor
public class QuotationPdfService {

    private final QuotationDocumentRepository documentRepository;

    /**
     * Root directory for saved PDFs.
     * Set in application.properties:
     *   quotation.pdf-dir=C:/uploads/quotations
     */
    @Value("${quotation.pdf-dir:uploads/quotations}")
    private String pdfDir;

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final BaseColor COLOUR_NAVY         = new BaseColor(13,  27,  42);
    private static final BaseColor COLOUR_BLUE         = new BaseColor(26,  95,  173);
    private static final BaseColor COLOUR_BLUE_LIGHT   = new BaseColor(238, 243, 252);
    private static final BaseColor COLOUR_ORANGE       = new BaseColor(232, 118, 42);
    private static final BaseColor COLOUR_GREEN        = new BaseColor(60,  181, 74);
    private static final BaseColor COLOUR_WHITE        = BaseColor.WHITE;
    private static final BaseColor COLOUR_TEXT         = new BaseColor(26,  36,  51);
    private static final BaseColor COLOUR_MUTED        = new BaseColor(124, 139, 161);
    private static final BaseColor COLOUR_BORDER       = new BaseColor(221, 227, 240);
    private static final BaseColor COLOUR_ROW_ALT      = new BaseColor(248, 250, 253);
    private static final BaseColor COLOUR_TERMS_BG     = new BaseColor(240, 246, 255);
    private static final BaseColor COLOUR_TERMS_BORDER = new BaseColor(191, 219, 254);
    private static final BaseColor COLOUR_FOOTER_BG    = new BaseColor(248, 249, 250);

    // ── Table column widths (must sum to 100) ─────────────────────────────────
    private static final float[] TABLE_COL_WIDTHS = { 25f, 55f, 20f };

    // ── Page margins and header height (points) ───────────────────────────────
    private static final float MARGIN        = 50f;
    private static final float HEADER_HEIGHT = 90f;

    // ── Terms & Conditions ────────────────────────────────────────────────────
    private static final String TERMS_TEXT =
            "50% deposit required upon acceptance. Balance due upon project completion. " +
                    "This quotation is valid for 30 days from the date of issue. " +
                    "All prices are in Zimbabwe Dollars (ZWL) and include applicable taxes. " +
                    "Work will commence within 5 business days of deposit receipt.";

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMMM yyyy");

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generate PDF bytes only — used for the admin preview endpoint
     * before the quotation is confirmed.
     */
    public byte[] generateQuotationPdf(Quotation quotation) throws DocumentException {
        return buildPdfBytes(quotation);
    }

    /**
     * Generate the PDF, save it to disk, persist a {@link QuotationDocument}
     * record, and return it.  Called when the admin confirms the quotation.
     *
     * @param quotation          fully-loaded entity (client + items eager-loaded)
     * @param confirmedByAdminId the admin's session ID
     */
    public QuotationDocument generateAndSave(Quotation quotation, Long confirmedByAdminId)
            throws DocumentException, IOException {

        // 1. Build bytes
        byte[] pdfBytes = buildPdfBytes(quotation);

        // 2. Ensure storage directory exists
        Path dir = Paths.get(pdfDir);
        Files.createDirectories(dir);

        // 3. Write file using NIO (reliable under Tomcat's temp-dir restrictions)
        String fileName = "Quotation_" + quotation.getQuotationNumber() + ".pdf";
        Path   filePath = dir.resolve(fileName);
        Files.write(filePath, pdfBytes);

        // 4. Upsert the QuotationDocument record (re-confirm regenerates the file)
        Optional<QuotationDocument> existing =
                documentRepository.findByQuotation_QuotationId(quotation.getQuotationId());

        QuotationDocument doc = existing.orElse(new QuotationDocument());
        doc.setQuotation(quotation);
        doc.setConfirmedByAdminId(confirmedByAdminId);
        doc.setClientId(quotation.getClient().getId());
        doc.setFilePath(filePath.toString().replace("\\", "/"));
        doc.setFileName(fileName);
        doc.setFileSizeBytes((long) pdfBytes.length);
        doc.setEmailSent(false);

        return documentRepository.save(doc);
    }

    /**
     * Mark the confirmation email as successfully sent.
     * Called by the service layer after the email has been dispatched.
     */
    public void markEmailSent(Long documentId) {
        documentRepository.findById(documentId).ifPresent(doc -> {
            doc.setEmailSent(true);
            documentRepository.save(doc);
        });
    }

    /**
     * Read the saved PDF bytes from disk for a given {@link QuotationDocument}.
     * Used by download endpoints so the file is never regenerated unnecessarily.
     */
    public byte[] loadPdfBytes(QuotationDocument doc) throws IOException {
        Path path = Paths.get(doc.getFilePath());
        if (!Files.exists(path)) {
            throw new IOException("PDF not found on disk: " + path);
        }
        return Files.readAllBytes(path);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PDF BUILDER
    // ─────────────────────────────────────────────────────────────────────────

    private byte[] buildPdfBytes(Quotation quotation) throws DocumentException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(
                PageSize.A4, MARGIN, MARGIN, HEADER_HEIGHT + 20f, 60f);
        PdfWriter writer = PdfWriter.getInstance(document, out);
        writer.setPageEvent(new HeaderFooterEvent(quotation));
        document.open();
        addMetaSection(document, quotation);
        addItemTable(document, quotation);
        addTermsBox(document);
        document.close();
        return out.toByteArray();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION BUILDERS
    // ─────────────────────────────────────────────────────────────────────────

    private void addMetaSection(Document document, Quotation quotation)
            throws DocumentException {
        String clientName  = quotation.getClient() != null
                ? quotation.getClient().getName() + " " + quotation.getClient().getSurname()
                : "—";
        String clientEmail = (quotation.getClient() != null
                && quotation.getClient().getEmail() != null)
                ? quotation.getClient().getEmail() : "";
        String dateIssued  = quotation.getCreatedAt()  != null
                ? quotation.getCreatedAt().format(DATE_FMT)  : "—";
        String validUntil  = quotation.getValidUntil() != null
                ? quotation.getValidUntil().format(DATE_FMT) : "N/A";

        PdfPTable meta = new PdfPTable(2);
        meta.setWidthPercentage(100);
        meta.setSpacingAfter(20f);

        meta.addCell(metaBox("BILLED TO",
                clientName + (clientEmail.isEmpty() ? "" : "\n" + clientEmail)));
        meta.addCell(metaBox("PROJECT",
                (quotation.getTitle() != null ? quotation.getTitle() : "Construction Project")
                        + (quotation.getDescription() != null && !quotation.getDescription().isBlank()
                        ? "\n" + truncate(quotation.getDescription(), 80) : "")));
        meta.addCell(metaBox("DATE ISSUED", dateIssued));
        meta.addCell(metaBox("VALID UNTIL", validUntil));

        document.add(meta);
    }

    private void addItemTable(Document document, Quotation quotation)
            throws DocumentException {
        List<QuotationItem> items = quotation.getQuotationItems();

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(TABLE_COL_WIDTHS);
        table.setSpacingAfter(16f);

        table.addCell(tableHeader("ITEM"));
        table.addCell(tableHeader("DESCRIPTION"));
        table.addCell(tableHeaderRight("PRICE (ZWL)"));

        double grandTotal = 0;
        for (int i = 0; i < items.size(); i++) {
            QuotationItem item     = items.get(i);
            BaseColor     rowBg    = (i % 2 == 0) ? COLOUR_WHITE : COLOUR_ROW_ALT;
            double        rowTotal = item.getTotalPrice() != null ? item.getTotalPrice() : 0;
            grandTotal += rowTotal;

            table.addCell(tableCell(
                    item.getServiceType() != null ? item.getServiceType() : "Item " + (i + 1),
                    rowBg, Element.ALIGN_LEFT, true));
            table.addCell(tableCell(
                    item.getDescription() != null ? item.getDescription() : "—",
                    rowBg, Element.ALIGN_LEFT, false));
            table.addCell(tableCell(
                    String.format("ZWL %.2f", rowTotal),
                    rowBg, Element.ALIGN_RIGHT, true));
        }

        // Grand total footer
        PdfPCell totalLabel = new PdfPCell(new Phrase("GRAND TOTAL",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, COLOUR_BLUE)));
        totalLabel.setColspan(2);
        totalLabel.setBackgroundColor(COLOUR_BLUE_LIGHT);
        totalLabel.setPadding(10f);
        totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalLabel.setBorderColor(COLOUR_BORDER);
        totalLabel.setBorderWidthTop(1.5f);
        table.addCell(totalLabel);

        PdfPCell totalAmount = new PdfPCell(new Phrase(
                String.format("ZWL %.2f", grandTotal),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, COLOUR_BLUE)));
        totalAmount.setBackgroundColor(COLOUR_BLUE_LIGHT);
        totalAmount.setPadding(10f);
        totalAmount.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalAmount.setBorderColor(COLOUR_BORDER);
        totalAmount.setBorderWidthTop(1.5f);
        table.addCell(totalAmount);

        document.add(table);
    }

    private void addTermsBox(Document document) throws DocumentException {
        PdfPTable terms = new PdfPTable(1);
        terms.setWidthPercentage(100);
        terms.setSpacingAfter(10f);

        Paragraph content = new Paragraph();
        content.add(new Chunk("TERMS & CONDITIONS\n",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, COLOUR_BLUE)));
        content.add(new Chunk(TERMS_TEXT,
                FontFactory.getFont(FontFactory.HELVETICA, 8, COLOUR_TEXT)));

        PdfPCell cell = new PdfPCell(content);
        cell.setBackgroundColor(COLOUR_TERMS_BG);
        cell.setBorderColor(COLOUR_TERMS_BORDER);
        cell.setBorderWidth(1f);
        cell.setPadding(12f);
        terms.addCell(cell);
        document.add(terms);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CELL FACTORIES
    // ─────────────────────────────────────────────────────────────────────────

    private PdfPCell metaBox(String label, String value) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + "\n",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, COLOUR_MUTED)));
        p.add(new Chunk(value,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f, COLOUR_TEXT)));
        PdfPCell cell = new PdfPCell(p);
        cell.setBackgroundColor(COLOUR_ROW_ALT);
        cell.setBorderColor(COLOUR_BORDER);
        cell.setPadding(10f);
        return cell;
    }

    private PdfPCell tableHeader(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, COLOUR_WHITE)));
        cell.setBackgroundColor(COLOUR_BLUE);
        cell.setPadding(8f);
        cell.setBorderColor(COLOUR_BLUE);
        return cell;
    }

    private PdfPCell tableHeaderRight(String text) {
        PdfPCell cell = tableHeader(text);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }

    private PdfPCell tableCell(String text, BaseColor bg, int align, boolean bold) {
        Font f = bold
                ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, COLOUR_TEXT)
                : FontFactory.getFont(FontFactory.HELVETICA,      9, COLOUR_MUTED);
        PdfPCell cell = new PdfPCell(new Phrase(text, f));
        cell.setBackgroundColor(bg);
        cell.setPadding(7f);
        cell.setHorizontalAlignment(align);
        cell.setBorderColor(COLOUR_BORDER);
        return cell;
    }

    private String truncate(String s, int max) {
        return (s == null || s.length() <= max) ? (s != null ? s : "—") : s.substring(0, max) + "…";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PAGE EVENT  — header band + footer on every page
    // ─────────────────────────────────────────────────────────────────────────

    private static class HeaderFooterEvent extends PdfPageEventHelper {

        private final Quotation quotation;

        HeaderFooterEvent(Quotation quotation) {
            this.quotation = quotation;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb   = writer.getDirectContent();
            Rectangle      page = document.getPageSize();
            drawHeader(cb, page);
            drawFooter(cb, page);
        }

        private void drawHeader(PdfContentByte cb, Rectangle page) {
            float w   = page.getWidth();
            float top = page.getTop();

            cb.saveState();
            cb.setColorFill(COLOUR_NAVY);
            cb.rectangle(0, top - HEADER_HEIGHT, w, HEADER_HEIGHT);
            cb.fill();
            cb.restoreState();

            float iconY = top - 52f;
            drawRect(cb, MARGIN,       iconY, 22f, 22f, COLOUR_BLUE);
            drawRect(cb, MARGIN + 27f, iconY, 22f, 22f, COLOUR_ORANGE);
            drawRect(cb, MARGIN + 54f, iconY, 22f, 22f, COLOUR_GREEN);

            cb.saveState();
            try {
                BaseFont bold   = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);
                BaseFont normal = BaseFont.createFont(BaseFont.HELVETICA,      BaseFont.CP1252, false);

                cb.setColorFill(COLOUR_WHITE);
                cb.beginText();
                cb.setFontAndSize(bold, 14);
                cb.setTextMatrix(MARGIN + 84f, top - 44f);
                cb.showText("MPHO YANGA CONSTRUCTION");
                cb.endText();

                cb.setColorFill(new BaseColor(180, 190, 210));
                cb.beginText();
                cb.setFontAndSize(normal, 7.5f);
                cb.setTextMatrix(MARGIN + 84f, top - 56f);
                cb.showText("BP No. 0200269091  ·  Zimbabwe  ·  admin@mphoyanga.co.zw");
                cb.endText();

                cb.setColorFill(COLOUR_BLUE_LIGHT);
                cb.beginText();
                cb.setFontAndSize(bold, 22f);
                cb.setTextMatrix(w - MARGIN - 130f, top - 42f);
                cb.showText("QUOTATION");
                cb.endText();

                if (quotation.getQuotationNumber() != null) {
                    cb.setColorFill(new BaseColor(180, 190, 210));
                    cb.beginText();
                    cb.setFontAndSize(normal, 8f);
                    cb.setTextMatrix(w - MARGIN - 130f, top - 55f);
                    cb.showText(quotation.getQuotationNumber());
                    cb.endText();
                }
            } catch (Exception ignored) {}
            cb.restoreState();
        }

        private void drawFooter(PdfContentByte cb, Rectangle page) {
            float w  = page.getWidth();
            float fh = 30f;
            cb.saveState();
            cb.setColorFill(COLOUR_FOOTER_BG);
            cb.rectangle(0, 0, w, fh);
            cb.fill();
            cb.setColorStroke(COLOUR_BORDER);
            cb.setLineWidth(0.5f);
            cb.moveTo(MARGIN, fh);
            cb.lineTo(w - MARGIN, fh);
            cb.stroke();
            try {
                cb.setColorFill(COLOUR_MUTED);
                cb.beginText();
                cb.setFontAndSize(
                        BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, false), 7.5f);
                cb.showTextAligned(PdfContentByte.ALIGN_CENTER,
                        "© " + java.time.Year.now().getValue()
                                + " Mpho Yanga Construction · BP No. 0200269091 · Zimbabwe",
                        w / 2f, 10f, 0);
                cb.endText();
            } catch (Exception ignored) {}
            cb.restoreState();
        }

        private void drawRect(PdfContentByte cb, float x, float y,
                              float w, float h, BaseColor colour) {
            cb.saveState();
            cb.setColorFill(colour);
            cb.roundRectangle(x, y, w, h, 3f);
            cb.fill();
            cb.restoreState();
        }
    }
}