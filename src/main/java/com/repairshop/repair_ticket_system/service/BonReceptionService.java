package com.repairshop.repair_ticket_system.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.repairshop.repair_ticket_system.entity.Ticket;
import com.repairshop.repair_ticket_system.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Generates the official "Bon de Réception" PDF when a device is dropped off.
 * Layout matches the procedure document: header with logo, client info, machine info,
 * panne, accessories, état, observations, conditions générales, signatures, footer.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BonReceptionService {

    private final TicketRepository ticketRepository;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Wiki Repair brand colors
    private static final Color WIKI_GREEN_DARK  = new Color(29, 107, 46);   // #1d6b2e
    private static final Color WIKI_GREEN_LIGHT = new Color(58, 181, 74);   // #3ab54a
    private static final Color GRAY_LABEL       = new Color(110, 110, 110);
    private static final Color GRAY_LIGHT       = new Color(245, 245, 245);
    private static final Color GRAY_BORDER      = new Color(220, 220, 220);

    /**
     * Generates the PDF bytes for the Bon de Réception. Also assigns and persists
     * a unique 6-digit bonNumber on the ticket (if not already set).
     */
    public byte[] generate(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));

        // Assign a unique bon number on first generation
        if (ticket.getBonNumber() == null || ticket.getBonNumber().isBlank()) {
            ticket.setBonNumber(generateUniqueBonNumber());
            ticket.setBonGeneratedAt(LocalDateTime.now());
            ticketRepository.save(ticket);
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            addHeader(doc, ticket);
            addTitleAndNumbers(doc, ticket);
            addClientInfo(doc, ticket);
            addMachineInfo(doc, ticket);
            addPanneAndAccessories(doc, ticket);
            addEtatAndObservations(doc, ticket);
            addConditionsGenerales(doc);
            addSignatures(doc);
            addFooter(doc);

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating Bon de Réception for ticket {}: {}", ticketId, e.getMessage(), e);
            throw new RuntimeException("Erreur génération PDF: " + e.getMessage());
        }
    }

    // ─── Header (logo + brand) ────────────────────────────────────────────────

    private void addHeader(Document doc, Ticket ticket) throws DocumentException {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{1f, 1f});

        // Left: brand
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Paragraph brand = new Paragraph();
        brand.add(new Chunk("WIKI ", new Font(Font.HELVETICA, 22, Font.BOLD, WIKI_GREEN_LIGHT)));
        brand.add(new Chunk("Repair", new Font(Font.HELVETICA, 22, Font.BOLD, WIKI_GREEN_DARK)));
        logoCell.addElement(brand);

        Paragraph tagline = new Paragraph("HIGH TECH PROVIDER",
                new Font(Font.HELVETICA, 8, Font.NORMAL, GRAY_LABEL));
        logoCell.addElement(tagline);
        header.addCell(logoCell);

        // Right: meta box
        PdfPCell metaCell = new PdfPCell();
        metaCell.setBorder(Rectangle.NO_BORDER);
        metaCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Paragraph dateP = new Paragraph("Date d'édition : " + LocalDateTime.now().format(FMT),
                new Font(Font.HELVETICA, 9, Font.NORMAL, GRAY_LABEL));
        dateP.setAlignment(Element.ALIGN_RIGHT);
        metaCell.addElement(dateP);

        Paragraph codeClient = new Paragraph(
                "Code Client : " + (ticket.getClientPhone() != null ? ticket.getClientPhone() : "—"),
                new Font(Font.HELVETICA, 9, Font.NORMAL, GRAY_LABEL));
        codeClient.setAlignment(Element.ALIGN_RIGHT);
        metaCell.addElement(codeClient);

        header.addCell(metaCell);
        doc.add(header);

        doc.add(new Paragraph(" "));
    }

    // ─── Title bar with bon number ────────────────────────────────────────────

    private void addTitleAndNumbers(Document doc, Ticket ticket) throws DocumentException {
        // Big green title bar
        PdfPTable title = new PdfPTable(1);
        title.setWidthPercentage(100);

        PdfPCell tCell = new PdfPCell(new Phrase("BON DE RÉCEPTION",
                new Font(Font.HELVETICA, 18, Font.BOLD, Color.WHITE)));
        tCell.setBackgroundColor(WIKI_GREEN_DARK);
        tCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        tCell.setPadding(10);
        tCell.setBorder(Rectangle.NO_BORDER);
        title.addCell(tCell);
        doc.add(title);

        // Sub-bar with N° and ticket number
        PdfPTable subBar = new PdfPTable(2);
        subBar.setWidthPercentage(100);
        subBar.setSpacingBefore(4);

        PdfPCell n1 = numberCell("N° " + ticket.getBonNumber(), WIKI_GREEN_LIGHT);
        PdfPCell n2 = numberCell("Ticket : " + ticket.getTicketNumber(), Color.DARK_GRAY);
        subBar.addCell(n1);
        subBar.addCell(n2);
        doc.add(subBar);

        doc.add(new Paragraph(" "));
    }

    private PdfPCell numberCell(String text, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(text, new Font(Font.HELVETICA, 11, Font.BOLD, Color.WHITE)));
        c.setBackgroundColor(bg);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPadding(7);
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    // ─── Client info ──────────────────────────────────────────────────────────

    private void addClientInfo(Document doc, Ticket ticket) throws DocumentException {
        addSectionHeader(doc, "Informations Client");
        PdfPTable t = twoColTable();
        addRow(t, "Nom / Raison Sociale", nullSafe(ticket.getClientName()));
        addRow(t, "Adresse",               nullSafe(ticket.getClientAddress()));
        addRow(t, "Téléphone",             nullSafe(ticket.getClientPhone()));
        addRow(t, "E-mail",                nullSafe(ticket.getClientEmail()));
        addRow(t, "Société",               nullSafe(ticket.getClientCompany()));
        addRow(t, "Type de client",
                ticket.getClientType() != null && ticket.getClientType().name().equals("ENTREPRISE")
                        ? "Entreprise" : "Particulier");
        addRow(t, "Service demandé",       nullSafe(ticket.getServiceType()));
        addRow(t, "N° Ticket",             ticket.getTicketNumber());
        doc.add(t);
        doc.add(new Paragraph(" "));
    }

    // ─── Machine info ─────────────────────────────────────────────────────────

    private void addMachineInfo(Document doc, Ticket ticket) throws DocumentException {
        addSectionHeader(doc, "Informations Machine");
        PdfPTable t = twoColTable();
        addRow(t, "Famille",     nullSafe(ticket.getProductFamily()));
        addRow(t, "Type",        nullSafe(ticket.getProductType()));
        addRow(t, "Marque",      nullSafe(ticket.getBrand()));
        addRow(t, "Désignation", nullSafe(ticket.getDesignation()));
        addRow(t, "Référence",   nullSafe(ticket.getReference()));
        addRow(t, "N° Série",    nullSafe(ticket.getSerialNumber()));
        addRow(t, "Statut Pièces",
                Boolean.TRUE.equals(ticket.getWarrantyPieces()) ? "Garantie" : "Hors Garantie");
        addRow(t, "Main d'œuvre",
                Boolean.TRUE.equals(ticket.getWarrantyLabor())  ? "Garantie" : "Hors Garantie");
        doc.add(t);
        doc.add(new Paragraph(" "));
    }

    // ─── Panne + accessoires ──────────────────────────────────────────────────

    private void addPanneAndAccessories(Document doc, Ticket ticket) throws DocumentException {
        addSectionHeader(doc, "Description de la Panne");
        addBox(doc, nullSafe(ticket.getProblemDescription()));
        doc.add(new Paragraph(" "));

        addSectionHeader(doc, "Accessoires de la Machine");
        addBox(doc, nullSafe(ticket.getAccessories()));
        doc.add(new Paragraph(" "));
    }

    // ─── État + observations ──────────────────────────────────────────────────

    private void addEtatAndObservations(Document doc, Ticket ticket) throws DocumentException {
        addSectionHeader(doc, "État de la Machine");
        addBox(doc, nullSafe(ticket.getMachineState()));
        doc.add(new Paragraph(" "));

        addSectionHeader(doc, "Observations Client");
        addBox(doc, nullSafe(ticket.getDiagnosticNotes()));
        doc.add(new Paragraph(" "));
    }

    // ─── Conditions générales ─────────────────────────────────────────────────

    private void addConditionsGenerales(Document doc) throws DocumentException {
        addSectionHeader(doc, "Conditions Générales de Réparation");

        Font small = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.DARK_GRAY);
        String[] conditions = {
            "• Les délais de diagnostic varient entre 24 heures et 72 heures. Si la pièce est disponible, " +
                "la réparation sera effectuée lors du diagnostic. Si la pièce n'est pas disponible, le client sera contacté.",
            "• Pour les appels sous garantie, le délai d'attente est estimé entre 15 et 45 jours, en fonction " +
                "de la disponibilité chez le fournisseur et des réglementations en vigueur.",
            "• Pour les appels facturables, vous pouvez vous référer au devis.",
            "• Si ces délais ne sont pas respectés ou en cas de comportement inapproprié de la part de vos vis-à-vis, " +
                "veuillez adresser votre réclamation directement à l'adresse suivante : wiki.repair@wiki.tn",
            "• Le matériel non récupéré dans un délai de 3 mois après notification sera considéré comme abandonné."
        };

        PdfPTable box = new PdfPTable(1);
        box.setWidthPercentage(100);
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(GRAY_LIGHT);
        c.setBorderColor(GRAY_BORDER);
        c.setPadding(10);

        for (String line : conditions) {
            Paragraph p = new Paragraph(line, small);
            p.setSpacingAfter(4);
            c.addElement(p);
        }
        box.addCell(c);
        doc.add(box);
        doc.add(new Paragraph(" "));
    }

    // ─── Signatures ───────────────────────────────────────────────────────────

    private void addSignatures(Document doc) throws DocumentException {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setSpacingBefore(10);

        Font label = new Font(Font.HELVETICA, 9, Font.BOLD, GRAY_LABEL);
        Font note  = new Font(Font.HELVETICA, 8, Font.ITALIC, GRAY_LABEL);

        PdfPCell c1 = new PdfPCell();
        c1.setBorder(Rectangle.BOX);
        c1.setBorderColor(GRAY_BORDER);
        c1.setPadding(10);
        c1.setMinimumHeight(70);
        c1.addElement(new Paragraph("Signature du Magasin", label));
        c1.addElement(new Paragraph(" ", note));
        t.addCell(c1);

        PdfPCell c2 = new PdfPCell();
        c2.setBorder(Rectangle.BOX);
        c2.setBorderColor(GRAY_BORDER);
        c2.setPadding(10);
        c2.setMinimumHeight(70);
        c2.addElement(new Paragraph("Signature du Client", label));
        c2.addElement(new Paragraph("(Bon pour accord sur les conditions générales)", note));
        t.addCell(c2);

        doc.add(t);
    }

    // ─── Footer ───────────────────────────────────────────────────────────────

    private void addFooter(Document doc) throws DocumentException {
        Paragraph spacer = new Paragraph(" ");
        spacer.setSpacingBefore(12);
        doc.add(spacer);

        Font footerFont = new Font(Font.HELVETICA, 8, Font.NORMAL, GRAY_LABEL);
        Paragraph footer = new Paragraph();
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.add(new Chunk("wiki.tn — Service Après-Vente   |   ", footerFont));
        footer.add(new Chunk("Tunis, Tunisie   |   ", footerFont));
        footer.add(new Chunk("Tél : +216 22414444   |   ", footerFont));
        footer.add(new Chunk("info@wiki.tn   |   ", footerFont));
        footer.add(new Chunk("www.wiki.tn", footerFont));
        doc.add(footer);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void addSectionHeader(Document doc, String title) throws DocumentException {
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

    private PdfPTable twoColTable() throws DocumentException {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1f, 2f});
        return t;
    }

    private void addRow(PdfPTable t, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label,
                new Font(Font.HELVETICA, 9, Font.NORMAL, GRAY_LABEL)));
        labelCell.setBorder(Rectangle.BOTTOM);
        labelCell.setBorderColor(GRAY_BORDER);
        labelCell.setPadding(5);

        PdfPCell valueCell = new PdfPCell(new Phrase(value,
                new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY)));
        valueCell.setBorder(Rectangle.BOTTOM);
        valueCell.setBorderColor(GRAY_BORDER);
        valueCell.setPadding(5);

        t.addCell(labelCell);
        t.addCell(valueCell);
    }

    private void addBox(Document doc, String text) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        PdfPCell c = new PdfPCell(new Phrase(text,
                new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY)));
        c.setBackgroundColor(GRAY_LIGHT);
        c.setBorderColor(GRAY_BORDER);
        c.setPadding(10);
        c.setMinimumHeight(35);
        t.addCell(c);
        doc.add(t);
    }

    private String nullSafe(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    /**
     * Generates a unique 6-digit bon number (e.g. 708941). Retries on collision.
     */
    private String generateUniqueBonNumber() {
        for (int i = 0; i < 50; i++) {
            String candidate = String.valueOf(100000 + RANDOM.nextInt(900000));
            if (ticketRepository.findAll().stream()
                    .noneMatch(t -> candidate.equals(t.getBonNumber()))) {
                return candidate;
            }
        }
        // Fallback: timestamp-based
        return String.valueOf(System.currentTimeMillis() % 1_000_000L);
    }
}
