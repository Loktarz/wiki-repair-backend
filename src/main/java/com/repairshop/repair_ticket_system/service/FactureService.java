package com.repairshop.repair_ticket_system.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.repairshop.repair_ticket_system.entity.Devis;
import com.repairshop.repair_ticket_system.entity.DevisLigne;
import com.repairshop.repair_ticket_system.entity.Ticket;
import com.repairshop.repair_ticket_system.entity.TicketStatus;
import com.repairshop.repair_ticket_system.repository.DevisRepository;
import com.repairshop.repair_ticket_system.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Generates the final invoice (Facture WIKI) for a ticket.
 *
 * Pricing rules from the procedure:
 *   - LIVRE_CLIENT          → full devis amount (pieces + main d'œuvre + TVA 19%)
 *   - DEVIS_REFUSE          → diagnostic fee only (20 DT)
 *   - REPARATION_IMPOSSIBLE → diagnostic fee only (20 DT)
 *   - any other status      → not yet billable (rejected by the controller)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FactureService {

    private final TicketRepository ticketRepository;
    private final DevisRepository  devisRepository;

    private static final double DIAGNOSTIC_FEE_HT  = 20.0;
    private static final double TVA_RATE           = 0.19;
    private static final DateTimeFormatter FMT     = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Color WIKI_GREEN_DARK  = new Color(29, 107, 46);
    private static final Color WIKI_GREEN_LIGHT = new Color(58, 181, 74);
    private static final Color GRAY_LABEL       = new Color(110, 110, 110);
    private static final Color GRAY_LIGHT       = new Color(245, 245, 245);
    private static final Color GRAY_BORDER      = new Color(220, 220, 220);

    public byte[] generate(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));

        TicketStatus s = ticket.getStatus();
        boolean billableFull       = s == TicketStatus.LIVRE_CLIENT;
        boolean billableDiagnostic = s == TicketStatus.DEVIS_REFUSE || s == TicketStatus.REPARATION_IMPOSSIBLE;
        if (!billableFull && !billableDiagnostic) {
            throw new RuntimeException("Ticket non facturable au statut actuel : " + s);
        }

        double piecesHT;
        double mainOeuvreHT;
        boolean usingDevis;
        Devis devis = null;

        if (billableFull) {
            // Use the devis as-is
            Optional<Devis> latest = devisRepository.findTopByTicketIdOrderByCreatedAtDesc(ticketId);
            if (latest.isEmpty()) {
                throw new RuntimeException("Aucun devis associé à ce ticket — impossible de facturer LIVRE_CLIENT.");
            }
            devis = latest.get();
            piecesHT     = devis.getTotalPiecesHT() != null ? devis.getTotalPiecesHT() : 0.0;
            mainOeuvreHT = devis.getMainDoeuvre()   != null ? devis.getMainDoeuvre()   : 0.0;
            usingDevis = true;
        } else {
            // Diagnostic fee only
            piecesHT = 0.0;
            mainOeuvreHT = DIAGNOSTIC_FEE_HT;
            usingDevis = false;
        }
        double tva    = (piecesHT + mainOeuvreHT) * TVA_RATE;
        double totalTTC = piecesHT + mainOeuvreHT + tva;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            addHeader(doc, ticket);
            addTitle(doc, ticket, s);
            addClientInfo(doc, ticket);
            addLineItems(doc, devis, mainOeuvreHT, usingDevis, s);
            addTotals(doc, piecesHT, mainOeuvreHT, tva, totalTTC);
            addFooter(doc);

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating Facture for ticket {}: {}", ticketId, e.getMessage(), e);
            throw new RuntimeException("Erreur génération facture: " + e.getMessage());
        }
    }

    // ─── Sections ─────────────────────────────────────────────────────────────

    private void addHeader(Document doc, Ticket ticket) throws DocumentException {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{1f, 1f});

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        Paragraph brand = new Paragraph();
        brand.add(new Chunk("WIKI ", new Font(Font.HELVETICA, 22, Font.BOLD, WIKI_GREEN_LIGHT)));
        brand.add(new Chunk("Repair", new Font(Font.HELVETICA, 22, Font.BOLD, WIKI_GREEN_DARK)));
        logoCell.addElement(brand);
        logoCell.addElement(new Paragraph("HIGH TECH PROVIDER",
                new Font(Font.HELVETICA, 8, Font.NORMAL, GRAY_LABEL)));
        header.addCell(logoCell);

        PdfPCell metaCell = new PdfPCell();
        metaCell.setBorder(Rectangle.NO_BORDER);
        metaCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph p1 = new Paragraph("Date : " + LocalDateTime.now().format(FMT),
                new Font(Font.HELVETICA, 9, Font.NORMAL, GRAY_LABEL));
        p1.setAlignment(Element.ALIGN_RIGHT);
        metaCell.addElement(p1);
        Paragraph p2 = new Paragraph("Ticket : " + ticket.getTicketNumber(),
                new Font(Font.HELVETICA, 9, Font.NORMAL, GRAY_LABEL));
        p2.setAlignment(Element.ALIGN_RIGHT);
        metaCell.addElement(p2);
        header.addCell(metaCell);
        doc.add(header);
        doc.add(new Paragraph(" "));
    }

    private void addTitle(Document doc, Ticket ticket, TicketStatus s) throws DocumentException {
        PdfPTable title = new PdfPTable(1);
        title.setWidthPercentage(100);
        PdfPCell tCell = new PdfPCell(new Phrase("FACTURE",
                new Font(Font.HELVETICA, 18, Font.BOLD, Color.WHITE)));
        tCell.setBackgroundColor(WIKI_GREEN_DARK);
        tCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        tCell.setPadding(10);
        tCell.setBorder(Rectangle.NO_BORDER);
        title.addCell(tCell);
        doc.add(title);

        Paragraph p = new Paragraph("Motif de facturation : " + reasonLabel(s),
                new Font(Font.HELVETICA, 9, Font.ITALIC, GRAY_LABEL));
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingBefore(4);
        p.setSpacingAfter(8);
        doc.add(p);
    }

    private void addClientInfo(Document doc, Ticket ticket) throws DocumentException {
        sectionHeader(doc, "Informations Client");
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1f, 2f});
        row(t, "Nom / Raison Sociale", n(ticket.getClientName()));
        row(t, "Adresse",               n(ticket.getClientAddress()));
        row(t, "Téléphone",             n(ticket.getClientPhone()));
        row(t, "E-mail",                n(ticket.getClientEmail()));
        row(t, "Type de client",
                ticket.getClientType() != null && ticket.getClientType().name().equals("ENTREPRISE")
                        ? "Entreprise" : "Particulier");
        doc.add(t);
        doc.add(new Paragraph(" "));
    }

    private void addLineItems(Document doc, Devis devis, double mainOeuvreHT,
                              boolean usingDevis, TicketStatus s) throws DocumentException {
        sectionHeader(doc, "Détail");

        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{4f, 1f, 1.5f, 1.5f});
        Font th = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
        addHeaderCell(t, "Description", th);
        addHeaderCell(t, "Qté",         th);
        addHeaderCell(t, "P.U. HT",     th);
        addHeaderCell(t, "Total HT",    th);

        if (usingDevis && devis != null) {
            for (DevisLigne l : devis.getLignes()) {
                int qty = l.getQuantite() != null ? l.getQuantite() : 0;
                double pu = l.getPrixUnitaire() != null ? l.getPrixUnitaire() : 0.0;
                line(t, n(l.getDescription()), String.valueOf(qty),
                        money(pu), money(qty * pu));
            }
            // Main d'œuvre line
            line(t, "Main d'œuvre", "1", money(mainOeuvreHT), money(mainOeuvreHT));
        } else {
            // Diagnostic-only invoice
            line(t, "Frais de diagnostic — " + reasonLabel(s),
                    "1", money(mainOeuvreHT), money(mainOeuvreHT));
        }
        doc.add(t);
        doc.add(new Paragraph(" "));
    }

    private void addTotals(Document doc, double piecesHT, double mainOeuvreHT,
                           double tva, double totalTTC) throws DocumentException {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(50);
        t.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.setWidths(new float[]{1.5f, 1f});

        kv(t, "Total pièces HT", money(piecesHT));
        kv(t, "Main d'œuvre HT", money(mainOeuvreHT));
        kv(t, "TVA (19%)",       money(tva));

        Font totalFont = new Font(Font.HELVETICA, 11, Font.BOLD, Color.WHITE);
        PdfPCell c1 = new PdfPCell(new Phrase("TOTAL TTC", totalFont));
        c1.setBackgroundColor(WIKI_GREEN_DARK);
        c1.setPadding(7);
        c1.setBorder(Rectangle.NO_BORDER);
        PdfPCell c2 = new PdfPCell(new Phrase(money(totalTTC) + " TND", totalFont));
        c2.setBackgroundColor(WIKI_GREEN_DARK);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c2.setPadding(7);
        c2.setBorder(Rectangle.NO_BORDER);
        t.addCell(c1);
        t.addCell(c2);
        doc.add(t);
    }

    private void addFooter(Document doc) throws DocumentException {
        Paragraph p = new Paragraph();
        p.setSpacingBefore(20);
        p.setAlignment(Element.ALIGN_CENTER);
        Font small = new Font(Font.HELVETICA, 8, Font.NORMAL, GRAY_LABEL);
        p.add(new Chunk("wiki.tn — Service Après-Vente   |   ", small));
        p.add(new Chunk("Tunis, Tunisie   |   ", small));
        p.add(new Chunk("Tél : +216 22414444   |   ", small));
        p.add(new Chunk("info@wiki.tn   |   ", small));
        p.add(new Chunk("www.wiki.tn", small));
        doc.add(p);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void sectionHeader(Document doc, String title) throws DocumentException {
        PdfPTable hb = new PdfPTable(1);
        hb.setWidthPercentage(100);
        PdfPCell c = new PdfPCell(new Phrase(title,
                new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE)));
        c.setBackgroundColor(WIKI_GREEN_LIGHT);
        c.setPadding(5);
        c.setBorder(Rectangle.NO_BORDER);
        hb.addCell(c);
        doc.add(hb);
    }

    private void row(PdfPTable t, String label, String value) {
        Font lf = new Font(Font.HELVETICA, 9, Font.NORMAL, GRAY_LABEL);
        Font vf = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
        PdfPCell c1 = new PdfPCell(new Phrase(label, lf));
        c1.setBorder(Rectangle.BOTTOM); c1.setBorderColor(GRAY_BORDER); c1.setPadding(5);
        PdfPCell c2 = new PdfPCell(new Phrase(value, vf));
        c2.setBorder(Rectangle.BOTTOM); c2.setBorderColor(GRAY_BORDER); c2.setPadding(5);
        t.addCell(c1); t.addCell(c2);
    }

    private void addHeaderCell(PdfPTable t, String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBackgroundColor(WIKI_GREEN_LIGHT);
        c.setPadding(6);
        c.setBorder(Rectangle.NO_BORDER);
        t.addCell(c);
    }

    private void line(PdfPTable t, String desc, String qty, String pu, String total) {
        Font f = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
        PdfPCell c1 = new PdfPCell(new Phrase(desc, f)); c1.setPadding(5); c1.setBorder(Rectangle.BOTTOM); c1.setBorderColor(GRAY_BORDER);
        PdfPCell c2 = new PdfPCell(new Phrase(qty,  f)); c2.setPadding(5); c2.setHorizontalAlignment(Element.ALIGN_CENTER); c2.setBorder(Rectangle.BOTTOM); c2.setBorderColor(GRAY_BORDER);
        PdfPCell c3 = new PdfPCell(new Phrase(pu,   f)); c3.setPadding(5); c3.setHorizontalAlignment(Element.ALIGN_RIGHT);  c3.setBorder(Rectangle.BOTTOM); c3.setBorderColor(GRAY_BORDER);
        PdfPCell c4 = new PdfPCell(new Phrase(total,f)); c4.setPadding(5); c4.setHorizontalAlignment(Element.ALIGN_RIGHT);  c4.setBorder(Rectangle.BOTTOM); c4.setBorderColor(GRAY_BORDER);
        t.addCell(c1); t.addCell(c2); t.addCell(c3); t.addCell(c4);
    }

    private void kv(PdfPTable t, String label, String value) {
        Font lf = new Font(Font.HELVETICA, 9, Font.NORMAL, GRAY_LABEL);
        Font vf = new Font(Font.HELVETICA, 9, Font.BOLD, Color.DARK_GRAY);
        PdfPCell c1 = new PdfPCell(new Phrase(label, lf));
        c1.setPadding(5); c1.setBorder(Rectangle.NO_BORDER);
        PdfPCell c2 = new PdfPCell(new Phrase(value + " TND", vf));
        c2.setPadding(5); c2.setHorizontalAlignment(Element.ALIGN_RIGHT); c2.setBorder(Rectangle.NO_BORDER);
        t.addCell(c1); t.addCell(c2);
    }

    private String n(String s) { return (s == null || s.isBlank()) ? "—" : s; }
    private String money(double d) { return String.format("%.2f", d); }
    private String reasonLabel(TicketStatus s) {
        return switch (s) {
            case LIVRE_CLIENT          -> "Réparation effectuée et livrée";
            case DEVIS_REFUSE          -> "Devis refusé — diagnostic uniquement";
            case REPARATION_IMPOSSIBLE -> "Réparation impossible — diagnostic uniquement";
            default                    -> "—";
        };
    }
}
