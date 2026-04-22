package com.repairshop.repair_ticket_system.service;

import com.repairshop.repair_ticket_system.entity.Ticket;
import com.repairshop.repair_ticket_system.entity.TicketStatus;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    // ─── Status Change Notification ────────────────────────────────────────────

    @Async
    public void sendStatusChangeEmail(Ticket ticket, TicketStatus newStatus) {
        if (ticket.getClientEmail() == null || ticket.getClientEmail().isBlank()) return;

        try {
            String subject = buildSubject(newStatus, ticket.getTicketNumber());
            String body    = buildStatusHtml(ticket, newStatus);
            send(ticket.getClientEmail(), subject, body);
            log.info("Status email sent to {} for ticket {}", ticket.getClientEmail(), ticket.getTicketNumber());
        } catch (Exception e) {
            log.error("Failed to send status email for ticket {}: {}", ticket.getTicketNumber(), e.getMessage());
        }
    }

    // ─── Devis Created Notification ────────────────────────────────────────────

    @Async
    public void sendDevisEmail(Ticket ticket, Double totalHT, Double tva, Double montantTotal) {
        if (ticket.getClientEmail() == null || ticket.getClientEmail().isBlank()) return;

        try {
            String subject = "Wiki Repair — Devis disponible pour votre ticket " + ticket.getTicketNumber();
            String body    = buildDevisHtml(ticket, totalHT, tva, montantTotal);
            send(ticket.getClientEmail(), subject, body);
            log.info("Devis email sent to {} for ticket {}", ticket.getClientEmail(), ticket.getTicketNumber());
        } catch (Exception e) {
            log.error("Failed to send devis email for ticket {}: {}", ticket.getTicketNumber(), e.getMessage());
        }
    }

    // ─── Internal send ────────────────────────────────────────────────────────

    private void send(String to, String subject, String htmlBody) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromAddress);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        mailSender.send(message);
    }

    // ─── Subject builder ──────────────────────────────────────────────────────

    private String buildSubject(TicketStatus status, String ticketNumber) {
        return switch (status) {
            case DEPOSE_MAGASIN            -> "Wiki Repair — Dépôt confirmé · " + ticketNumber;
            case DIAGNOSTIC_EN_ATTENTE     -> "Wiki Repair — En attente de diagnostic · " + ticketNumber;
            case EN_DIAGNOSTIC             -> "Wiki Repair — Diagnostic en cours · " + ticketNumber;
            case DIAGNOSTIC_TERMINE        -> "Wiki Repair — Diagnostic terminé · " + ticketNumber;
            case DEVIS_EN_ATTENTE          -> "Wiki Repair — Devis en préparation · " + ticketNumber;
            case DEVIS_ENVOYE_CLIENT       -> "Wiki Repair — Votre devis est prêt · " + ticketNumber;
            case DEVIS_ACCEPTE             -> "Wiki Repair — Devis accepté · " + ticketNumber;
            case DEVIS_REFUSE              -> "Wiki Repair — Devis refusé · " + ticketNumber;
            case TENTATIVE_REPARATION      -> "Wiki Repair — Tentative de réparation · " + ticketNumber;
            case ATTENTE_PIECE             -> "Wiki Repair — En attente d'une pièce · " + ticketNumber;
            case PIECE_RECUE               -> "Wiki Repair — Pièce reçue · " + ticketNumber;
            case EN_REPARATION             -> "Wiki Repair — Réparation en cours · " + ticketNumber;
            case REPARATION_TERMINEE       -> "Wiki Repair — Réparation terminée · " + ticketNumber;
            case PRET_RETRAIT              -> "Wiki Repair — Votre appareil est prêt ! · " + ticketNumber;
            case LIVRE_CLIENT              -> "Wiki Repair — Appareil remis · " + ticketNumber;
            case REPARATION_IMPOSSIBLE     -> "Wiki Repair — Réparation impossible · " + ticketNumber;
            default                        -> "Wiki Repair — Mise à jour de votre ticket · " + ticketNumber;
        };
    }

    // ─── Status HTML email ────────────────────────────────────────────────────

    private String buildStatusHtml(Ticket ticket, TicketStatus status) {
        String statusLabel   = translateStatus(status);
        String statusColor   = statusColor(status);
        String message       = statusMessage(status, ticket);

        return """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;background:#f5f5f5;padding:30px">
              <div style="background:#1d6b2e;border-radius:10px 10px 0 0;padding:24px;text-align:center">
                <span style="background:#3ab54a;color:white;font-weight:900;font-size:18px;padding:6px 12px;border-radius:4px 0 0 4px">WIKI</span><span style="background:#1d6b2e;color:white;font-weight:900;font-size:18px;padding:6px 12px;border:2px solid #3ab54a;border-radius:0 4px 4px 0">Repair</span>
              </div>
              <div style="background:white;padding:32px;border-radius:0 0 10px 10px;border:1px solid #e0e0e0">
                <p style="color:#555;font-size:15px">Bonjour <strong>%s</strong>,</p>
                <p style="color:#555;font-size:15px">%s</p>

                <div style="background:#f9f9f9;border-left:4px solid %s;border-radius:6px;padding:16px;margin:24px 0">
                  <p style="margin:0;font-size:13px;color:#888;text-transform:uppercase;letter-spacing:1px">Statut actuel</p>
                  <p style="margin:6px 0 0;font-size:18px;font-weight:bold;color:%s">%s</p>
                </div>

                <table style="width:100%%;border-collapse:collapse;font-size:14px;margin:16px 0">
                  <tr><td style="padding:8px 0;color:#888;width:40%%">Numéro de ticket</td><td style="font-weight:bold;color:#1a1a1a">%s</td></tr>
                  <tr><td style="padding:8px 0;color:#888">Appareil</td><td style="color:#1a1a1a">%s %s</td></tr>
                  %s
                </table>

                <p style="font-size:13px;color:#888;margin-top:24px">Pour toute question, contactez notre équipe.<br>
                <strong style="color:#1d6b2e">Wiki Repair</strong></p>
              </div>
            </div>
            """.formatted(
                ticket.getClientName(),
                message,
                statusColor, statusColor, statusLabel,
                ticket.getTicketNumber(),
                ticket.getBrand() != null ? ticket.getBrand() : "",
                ticket.getDesignation() != null ? ticket.getDesignation() : ticket.getProductType(),
                ticket.getDiagnosticNotes() != null && !ticket.getDiagnosticNotes().isBlank()
                    ? "<tr><td style=\"padding:8px 0;color:#888\">Notes</td><td style=\"color:#1a1a1a\">" + ticket.getDiagnosticNotes() + "</td></tr>"
                    : ""
        );
    }

    // ─── Devis HTML email ─────────────────────────────────────────────────────

    private String buildDevisHtml(Ticket ticket, Double totalHT, Double tva, Double montantTotal) {
        return """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;background:#f5f5f5;padding:30px">
              <div style="background:#1d6b2e;border-radius:10px 10px 0 0;padding:24px;text-align:center">
                <span style="background:#3ab54a;color:white;font-weight:900;font-size:18px;padding:6px 12px;border-radius:4px 0 0 4px">WIKI</span><span style="background:#1d6b2e;color:white;font-weight:900;font-size:18px;padding:6px 12px;border:2px solid #3ab54a;border-radius:0 4px 4px 0">Repair</span>
              </div>
              <div style="background:white;padding:32px;border-radius:0 0 10px 10px;border:1px solid #e0e0e0">
                <p style="color:#555;font-size:15px">Bonjour <strong>%s</strong>,</p>
                <p style="color:#555;font-size:15px">
                  Un <strong>devis</strong> a été établi pour votre appareil (<strong>%s</strong>).
                  Veuillez en prendre connaissance et nous informer de votre décision.
                </p>

                <div style="background:#f9fafb;border:1px solid #e0e0e0;border-radius:8px;padding:20px;margin:24px 0">
                  <p style="margin:0 0 12px;font-size:13px;color:#888;text-transform:uppercase;letter-spacing:1px">Récapitulatif du devis</p>
                  <table style="width:100%%;font-size:14px;border-collapse:collapse">
                    <tr><td style="padding:6px 0;color:#555">Total pièces HT</td><td style="text-align:right;font-weight:bold">%.2f TND</td></tr>
                    <tr><td style="padding:6px 0;color:#555">TVA (19%%)</td><td style="text-align:right;font-weight:bold">%.2f TND</td></tr>
                    <tr style="border-top:2px solid #e0e0e0">
                      <td style="padding:10px 0 0;font-size:16px;font-weight:bold;color:#1d6b2e">Montant total TTC</td>
                      <td style="padding:10px 0 0;text-align:right;font-size:16px;font-weight:bold;color:#1d6b2e">%.2f TND</td>
                    </tr>
                  </table>
                </div>

                <p style="color:#555;font-size:14px">
                  Numéro de ticket : <strong>%s</strong><br>
                  Veuillez contacter notre équipe ou vous présenter en magasin pour accepter ou refuser ce devis.
                </p>

                <p style="font-size:13px;color:#888;margin-top:24px">
                <strong style="color:#1d6b2e">Wiki Repair</strong></p>
              </div>
            </div>
            """.formatted(
                ticket.getClientName(),
                ticket.getBrand() != null ? ticket.getBrand() + " " + ticket.getProductType() : ticket.getProductType(),
                totalHT != null ? totalHT : 0.0,
                tva != null ? tva : 0.0,
                montantTotal != null ? montantTotal : 0.0,
                ticket.getTicketNumber()
        );
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String translateStatus(TicketStatus s) {
        return switch (s) {
            case EN_ATTENTE_DEPOT          -> "En attente de dépôt";
            case DEPOSE_MAGASIN            -> "Déposé en magasin";
            case FICHE_REPARATION_IMPRIMEE -> "Fiche imprimée";
            case DIAGNOSTIC_EN_ATTENTE     -> "Diagnostic en attente";
            case EN_DIAGNOSTIC             -> "En diagnostic";
            case DIAGNOSTIC_TERMINE        -> "Diagnostic terminé";
            case DEVIS_EN_ATTENTE          -> "Devis en attente";
            case DEVIS_ENVOYE_CLIENT       -> "Devis envoyé";
            case DEVIS_ACCEPTE             -> "Devis accepté";
            case DEVIS_REFUSE              -> "Devis refusé";
            case TENTATIVE_REPARATION      -> "Tentative de réparation";
            case ATTENTE_PIECE             -> "Attente de pièce";
            case PIECE_RECUE               -> "Pièce reçue";
            case EN_REPARATION             -> "En réparation";
            case REPARATION_TERMINEE       -> "Réparation terminée";
            case PRET_RETRAIT              -> "Prêt pour retrait";
            case LIVRE_CLIENT              -> "Livré au client";
            case REPARATION_IMPOSSIBLE     -> "Réparation impossible";
        };
    }

    private String statusColor(TicketStatus s) {
        return switch (s) {
            case PRET_RETRAIT, REPARATION_TERMINEE, LIVRE_CLIENT, DEVIS_ACCEPTE -> "#1d6b2e";
            case REPARATION_IMPOSSIBLE, DEVIS_REFUSE                             -> "#c0392b";
            case DEVIS_ENVOYE_CLIENT, DEVIS_EN_ATTENTE                          -> "#e67e22";
            default                                                               -> "#2980b9";
        };
    }

    private String statusMessage(TicketStatus status, Ticket ticket) {
        return switch (status) {
            case DEPOSE_MAGASIN        -> "Votre appareil a bien été déposé dans notre magasin. Nous allons prendre en charge votre demande dans les meilleurs délais.";
            case EN_DIAGNOSTIC         -> "Le diagnostic de votre appareil a débuté. Nos techniciens analysent le problème.";
            case DIAGNOSTIC_TERMINE    -> "Le diagnostic de votre appareil est terminé. Un devis vous sera communiqué prochainement.";
            case DEVIS_ENVOYE_CLIENT   -> "Un devis a été établi pour la réparation de votre appareil. Veuillez le consulter et nous informer de votre décision.";
            case DEVIS_ACCEPTE         -> "Votre accord pour le devis a bien été enregistré. La réparation va démarrer prochainement.";
            case DEVIS_REFUSE          -> "Vous avez refusé le devis. Votre appareil sera mis à votre disposition pour récupération.";
            case EN_REPARATION         -> "La réparation de votre appareil est en cours.";
            case REPARATION_TERMINEE   -> "La réparation de votre appareil est terminée avec succès !";
            case PRET_RETRAIT          -> "🎉 Bonne nouvelle ! Votre appareil est prêt et disponible pour être récupéré en magasin.";
            case REPARATION_IMPOSSIBLE -> "Après diagnostic, nos techniciens n'ont pas pu réparer votre appareil. Veuillez nous contacter pour convenir des prochaines étapes.";
            case LIVRE_CLIENT          -> "Votre appareil vous a été remis. Merci de votre confiance !";
            case ATTENTE_PIECE         -> "Nous sommes en attente d'une pièce pour la réparation de votre appareil.";
            case PIECE_RECUE           -> "La pièce nécessaire à la réparation de votre appareil a été reçue. La réparation va reprendre.";
            default                    -> "Le statut de votre ticket a été mis à jour.";
        };
    }
}
