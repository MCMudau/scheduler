package com.mphoYanga.scheduler.services;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.mphoYanga.scheduler.models.Client;
import com.mphoYanga.scheduler.models.Quotation;
import com.mphoYanga.scheduler.models.QuotationDocument;
import com.mphoYanga.scheduler.models.QuotationItem;
import com.mphoYanga.scheduler.repos.AdminRepository;
import com.mphoYanga.scheduler.repos.QuotationDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class QuotationPdfService {

    private static final Logger log = LoggerFactory.getLogger(QuotationPdfService.class);
    private final QuotationDocumentRepository documentRepository;
    private final AdminRepository             adminRepository;

    @Value("${quotation.pdf-dir:uploads/quotations}")
    private String pdfDir;

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final BaseColor C_BLUE    = new BaseColor(26,  95,  173);
    private static final BaseColor C_DARK    = new BaseColor(26,  34,  51);
    private static final BaseColor C_WHITE   = BaseColor.WHITE;
    private static final BaseColor C_MUTED   = new BaseColor(110, 110, 110);
    private static final BaseColor C_BORDER  = new BaseColor(180, 180, 180);
    private static final BaseColor C_ROW_ALT = new BaseColor(245, 245, 245);

    private static final float MARGIN   = 36f;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMMM d, yyyy");

    private static final String COMPANY_ADDRESS = "1867 Industry Site\nBeitbridge";

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────────────

    /** Preview — no admin info available yet. */
    public byte[] generateQuotationPdf(Quotation quotation) throws DocumentException {
        try {
            return buildPdfBytes(quotation, "Admin");
        } catch (IOException e) {
            throw new DocumentException("Failed to build PDF: " + e.getMessage());
        }
    }

    /** Confirm + save — looks up the real admin name from the ID. */
    public QuotationDocument generateAndSave(Quotation quotation, Long confirmedByAdminId)
            throws DocumentException, IOException {

        String adminName = adminRepository.findById(confirmedByAdminId)
                .map(a -> a.getName() + " " + a.getSurname())
                .orElse("Admin");

        byte[] pdfBytes = buildPdfBytes(quotation, adminName);
        Path dir = Paths.get(pdfDir);
        Files.createDirectories(dir);
        String fileName = "Quotation_" + quotation.getQuotationNumber() + ".pdf";
        Path   filePath = dir.resolve(fileName);
        Files.write(filePath, pdfBytes);

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

    public void markEmailSent(Long documentId) {
        documentRepository.findById(documentId).ifPresent(doc -> {
            doc.setEmailSent(true);
            documentRepository.save(doc);
        });
    }

    public byte[] loadPdfBytes(QuotationDocument doc) throws IOException {
        Path path = Paths.get(doc.getFilePath());
        if (!Files.exists(path)) throw new IOException("PDF not found on disk: " + path);
        return Files.readAllBytes(path);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BUILDER
    // ─────────────────────────────────────────────────────────────────────────

    private byte[] buildPdfBytes(Quotation quotation, String adminName)
            throws DocumentException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, MARGIN, MARGIN, MARGIN, MARGIN);
        PdfWriter.getInstance(document, out);
        document.open();

        addTopHeader(document, quotation);
        addBlueDivider(document);
        addBillToSection(document, quotation, adminName);
        addComments(document, quotation);
        addItemTable(document, quotation);
        addFooter(document);

        document.close();
        return out.toByteArray();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTIONS
    // ─────────────────────────────────────────────────────────────────────────

    private void addTopHeader(Document document, Quotation quotation)
            throws DocumentException, IOException {

        // ── Row 1: logo | "Quotation" ─────────────────────────────────────────
        PdfPTable row1 = new PdfPTable(2);
        row1.setWidthPercentage(100);
        row1.setWidths(new float[]{ 55f, 45f });
        row1.setSpacingAfter(4f);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setPadding(0f);
        try {
            ClassPathResource res = new ClassPathResource("static/Screenshot 2026-06-03 182310.png");
            byte[] imgBytes = res.getInputStream().readAllBytes();
            Image logo = Image.getInstance(imgBytes);
            logo.scaleToFit(200f, 65f);
            logoCell.addElement(logo);
        } catch (Exception e) {
            log.warn("Logo not found, using text fallback: {}", e.getMessage());
            logoCell.addElement(new Phrase("MPHO YANGA",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16f, C_BLUE)));
        }
        row1.addCell(logoCell);

        PdfPCell quotTitleCell = new PdfPCell(new Phrase("Quotation",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 34f, C_MUTED)));
        quotTitleCell.setBorder(Rectangle.NO_BORDER);
        quotTitleCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        quotTitleCell.setVerticalAlignment(Element.ALIGN_BOTTOM);
        row1.addCell(quotTitleCell);
        document.add(row1);

        // ── Row 2: "MPHO YAN GA" | "BP No." ──────────────────────────────────
        PdfPTable row2 = new PdfPTable(2);
        row2.setWidthPercentage(100);
        row2.setWidths(new float[]{ 55f, 45f });
        row2.setSpacingAfter(0f);

        Paragraph namePara = new Paragraph();
        namePara.add(new Chunk("MPHO ",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD,    22f, C_DARK)));
        namePara.add(new Chunk("YAN",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE,  22f, C_DARK)));
        namePara.add(new Chunk("GA",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22f, Font.UNDERLINE, C_DARK)));
        PdfPCell nameCell = new PdfPCell(namePara);
        nameCell.setBorder(Rectangle.NO_BORDER);
        nameCell.setPaddingBottom(0f);
        row2.addCell(nameCell);

        PdfPCell bpCell = new PdfPCell(new Phrase("BP No.0200269091",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, C_DARK)));
        bpCell.setBorder(Rectangle.NO_BORDER);
        bpCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        bpCell.setVerticalAlignment(Element.ALIGN_BOTTOM);
        row2.addCell(bpCell);
        document.add(row2);

        // ── Row 3: "CONSTRUCTION" + address | DATE / Quotation # / Customer ID ─
        String dateStr = quotation.getCreatedAt() != null
                ? quotation.getCreatedAt().format(DATE_FMT) : "—";
        String quotNum = quotation.getQuotationNumber() != null
                ? quotation.getQuotationNumber() : "—";
        String custId  = quotation.getClient() != null
                ? "C" + quotation.getClient().getId() : "—";

        PdfPTable row3 = new PdfPTable(2);
        row3.setWidthPercentage(100);
        row3.setWidths(new float[]{ 55f, 45f });
        row3.setSpacingAfter(6f);

        Paragraph leftPara = new Paragraph();
        leftPara.add(new Chunk("CONSTRUCTION\n",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13f, C_DARK)));
        leftPara.add(new Chunk(COMPANY_ADDRESS,
                FontFactory.getFont(FontFactory.HELVETICA, 8f, C_DARK)));
        PdfPCell leftCell = new PdfPCell(leftPara);
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPaddingTop(0f);
        row3.addCell(leftCell);

        Font lbl = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, C_DARK);
        Font val = FontFactory.getFont(FontFactory.HELVETICA,      8f, C_DARK);
        Paragraph metaPara = new Paragraph();
        metaPara.setAlignment(Element.ALIGN_RIGHT);
        metaPara.add(new Chunk("DATE  ",        lbl)); metaPara.add(new Chunk(dateStr + "\n", val));
        metaPara.add(new Chunk("Quotation #  ", lbl)); metaPara.add(new Chunk(quotNum + "\n",  val));
        metaPara.add(new Chunk("Customer ID  ", lbl)); metaPara.add(new Chunk(custId,           val));
        PdfPCell rightCell = new PdfPCell(metaPara);
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        row3.addCell(rightCell);
        document.add(row3);
    }

    private void addBlueDivider(Document document) throws DocumentException {
        PdfPTable divider = new PdfPTable(1);
        divider.setWidthPercentage(100);
        divider.setSpacingAfter(10f);
        PdfPCell cell = new PdfPCell(new Phrase(" "));
        cell.setBackgroundColor(C_BLUE);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setFixedHeight(3f);
        cell.setPadding(0f);
        divider.addCell(cell);
        document.add(divider);
    }

    private void addBillToSection(Document document, Quotation quotation, String adminName)
            throws DocumentException {
        Client client     = quotation.getClient();
        String clientName = client != null
                ? (client.getName() + " " + client.getSurname()).toUpperCase() : "—";
        String clientAddr = (client != null && client.getAddress() != null
                && !client.getAddress().isBlank())
                ? client.getAddress().toUpperCase() : "";
        String validUntil = quotation.getValidUntil() != null
                ? quotation.getValidUntil().format(DATE_FMT) : "N/A";

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{ 55f, 45f });
        table.setSpacingAfter(10f);

        Font sectionLabel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, C_DARK);
        Font bodyNormal   = FontFactory.getFont(FontFactory.HELVETICA,      9f,   C_DARK);
        Font bodyBold     = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f,   C_DARK);

        Paragraph billPara = new Paragraph();
        billPara.add(new Chunk("Bill To:\n",        sectionLabel));
        billPara.add(new Chunk(clientName + "\n",   bodyBold));
        if (!clientAddr.isEmpty())
            billPara.add(new Chunk(clientAddr,      bodyNormal));
        PdfPCell billCell = new PdfPCell(billPara);
        billCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(billCell);

        Font italicMuted = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8f, C_MUTED);
        Font valFont     = FontFactory.getFont(FontFactory.HELVETICA,         8f, C_DARK);
        Paragraph rightPara = new Paragraph();
        rightPara.setAlignment(Element.ALIGN_RIGHT);
        rightPara.add(new Chunk("Quotation valid until:  ", italicMuted));
        rightPara.add(new Chunk(validUntil + "\n",          valFont));
        rightPara.add(new Chunk("Prepared by:  ",           italicMuted));
        rightPara.add(new Chunk(adminName,                  valFont));
        PdfPCell rightCell = new PdfPCell(rightPara);
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(rightCell);

        document.add(table);
    }

    private void addComments(Document document, Quotation quotation) throws DocumentException {
        String desc  = quotation.getDescription();
        String title = quotation.getTitle();
        if ((desc == null || desc.isBlank()) && (title == null || title.isBlank())) return;

        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, C_DARK);
        Font bodyFont  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, C_DARK);

        Paragraph p = new Paragraph();
        p.setSpacingAfter(8f);
        p.add(new Chunk("Comments or special instructions:\n", labelFont));
        if (desc != null && !desc.isBlank())
            p.add(new Chunk(desc + "\n", bodyFont));
        if (title != null && !title.isBlank())
            p.add(new Chunk(title,       bodyFont));
        document.add(p);
    }

    private void addItemTable(Document document, Quotation quotation)
            throws DocumentException {
        List<QuotationItem> items    = quotation.getQuotationItems();
        String              currency = quotation.getCurrency() != null
                ? quotation.getCurrency() : "US$";

        // 3 columns: Description | Qty | Amount
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{ 65f, 10f, 25f });
        table.setSpacingAfter(10f);

        // ── Header ────────────────────────────────────────────────────────────
        Font hdrFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, C_WHITE);
        PdfPCell hDesc = headerCell("Description",        hdrFont, Element.ALIGN_LEFT);
        PdfPCell hQty  = headerCell("Qty",                hdrFont, Element.ALIGN_CENTER);
        PdfPCell hAmt  = headerCell("AMOUNT " + currency, hdrFont, Element.ALIGN_RIGHT);
        table.addCell(hDesc);
        table.addCell(hQty);
        table.addCell(hAmt);

        // ── Item rows ─────────────────────────────────────────────────────────
        Font rowFont  = FontFactory.getFont(FontFactory.HELVETICA,      9f,  C_DARK);
        Font totalFnt = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f, C_DARK);
        double grandTotal = 0;

        for (int i = 0; i < items.size(); i++) {
            QuotationItem item  = items.get(i);
            BaseColor     bg    = (i % 2 == 0) ? C_WHITE : C_ROW_ALT;
            double        price = item.getTotalPrice() != null ? item.getTotalPrice() : 0;
            grandTotal += price;

            String descTxt = item.getDescription() != null ? item.getDescription() : "—";
            String qtyTxt  = item.getQuantity() != null ? formatQty(item.getQuantity()) : "1";
            String amtTxt  = price == 0 ? "" : String.format("$ %.2f", price);

            table.addCell(borderedCell(new Phrase(descTxt, rowFont), bg, Element.ALIGN_LEFT));
            table.addCell(borderedCell(new Phrase(qtyTxt,  rowFont), bg, Element.ALIGN_CENTER));
            table.addCell(borderedCell(new Phrase(amtTxt,  rowFont), bg, Element.ALIGN_RIGHT));
        }

        // ── TAX row (always shown as Incl.) ──────────────────────────────────
        table.addCell(borderedCell(new Phrase("TAX",   rowFont), C_WHITE, Element.ALIGN_LEFT));
        table.addCell(borderedCell(new Phrase("",      rowFont), C_WHITE, Element.ALIGN_CENTER));
        table.addCell(borderedCell(new Phrase("Incl.", rowFont), C_WHITE, Element.ALIGN_RIGHT));

        // ── Total row (label spans 2 cols) ────────────────────────────────────
        PdfPCell totalLbl = borderedCell(new Phrase("TOTAL", totalFnt), C_WHITE, Element.ALIGN_RIGHT);
        totalLbl.setColspan(2);
        totalLbl.setBorderWidthTop(1f);
        PdfPCell totalAmt = borderedCell(
                new Phrase(String.format("$ %.2f", grandTotal), totalFnt), C_WHITE, Element.ALIGN_RIGHT);
        totalAmt.setBorderWidthTop(1f);
        table.addCell(totalLbl);
        table.addCell(totalAmt);

        document.add(table);
    }

    private void addFooter(Document document) throws DocumentException {
        Font bold   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, C_DARK);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA,      8f, C_DARK);

        Paragraph contact = new Paragraph();
        contact.setSpacingAfter(10f);
        contact.add(new Chunk(
            "If you have any questions concerning this quotation, contact Mpho Mudau ,\n" +
            "Phone Number 0712332083 / 0771527368,  E-mail  mphoyangainvestment@gmail.com", bold));
        document.add(contact);

        Paragraph payment = new Paragraph();
        payment.setSpacingAfter(14f);
        payment.add(new Chunk("Payment Methods\n",                bold));
        payment.add(new Chunk("We Accept Visa, Master Card, EcoCash.\n", normal));
        payment.add(new Chunk("Banking Details\n",                bold));
        payment.add(new Chunk("Mpho Yanga Investments\n",         normal));
        payment.add(new Chunk("Acc / ZB 4555470019200 ZiG\n",     normal));
        payment.add(new Chunk("4555470019405 US$",                normal));
        document.add(payment);

        PdfPTable thankYou = new PdfPTable(1);
        thankYou.setWidthPercentage(100);
        PdfPCell tyCell = new PdfPCell(new Phrase("THANK YOU FOR YOUR BUSINESS!",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f, C_WHITE)));
        tyCell.setBackgroundColor(C_DARK);
        tyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        tyCell.setPadding(12f);
        tyCell.setBorder(Rectangle.NO_BORDER);
        thankYou.addCell(tyCell);
        document.add(thankYou);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CELL HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private PdfPCell headerCell(String text, Font font, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBackgroundColor(C_DARK);
        c.setHorizontalAlignment(align);
        c.setPadding(7f);
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    private PdfPCell borderedCell(Phrase phrase, BaseColor bg, int align) {
        PdfPCell c = new PdfPCell(phrase);
        c.setBackgroundColor(bg);
        c.setHorizontalAlignment(align);
        c.setPadding(7f);
        c.setBorderColor(C_BORDER);
        c.setBorderWidthTop(0f);
        c.setBorderWidthLeft(0.5f);
        c.setBorderWidthRight(0.5f);
        c.setBorderWidthBottom(0.5f);
        return c;
    }

    private String formatQty(double qty) {
        return qty == Math.floor(qty) ? String.valueOf((long) qty) : String.valueOf(qty);
    }
}
