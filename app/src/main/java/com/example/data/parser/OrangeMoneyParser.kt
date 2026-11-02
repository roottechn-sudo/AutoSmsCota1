package com.example.data.parser

import com.example.data.model.OrangeMoneySmsType
import com.example.data.model.ParsedSmsResult
import java.util.regex.Pattern

/**
 * Parseur d'expressions régulières (RegEx) pour les notifications SMS d'Orange Money Burkina Faso.
 *
 * Supporte :
 * 1. Dépôt / Transfert reçu (Recette) : "Cher client, vous avez recu 10000.0 FCFA, Frais: FCFA, Taxe: FCFA de 3177363 KIENON MULTI SERVICES. Votre solde est de 15106.698 FCFA. ID Trans: CI261031.1040.8675069. Merci d'utiliser Orange Money."
 * 2. Transfert émis (Dépense) : "Cher client, vous avez transfere 1,780.00 FCFA, Frais: 0.0 FCFA, Taxe: FCFA au numero 54766084,Nonganaba Alexandre. Votre solde est de 5611.698 FCFA. ID Trans: PP261031.1017.2901274..."
 * 3. Paiement effectué (Dépense) : "Votre paiement de 500.00 FCFA, Frais: 4.3478 FCFA, Taxe: 0.6522 FCFA a Bebe jean-Baptiste Bougrouma a ete effectue avec succes. Votre solde est de : 5106.698 FCFA. Trans id: MP261031.1009.97358916."
 */
object OrangeMoneyParser {

    // Regex pour l'identifiant unique de transaction (ex: ID Trans: CI261031.1040.8675069 ou Trans id: MP261031.1009.97358916)
    private val REGEX_TRANSACTION_ID = Pattern.compile(
        "(?:ID\\s*Trans|Trans\\s*id|Trans\\s*ID|Id\\s*Trans|Ref)\\s*:\\s*([A-Za-z0-9.]+)",
        Pattern.CASE_INSENSITIVE
    )

    // Modèle 1: Transfert/Dépôt reçu (Recette)
    // "vous avez recu 10000.0 FCFA ... de 3177363 KIENON MULTI SERVICES"
    private val REGEX_RECETTE = Pattern.compile(
        "vous\\s+avez\\s+(?:recu|reçu)\\s+([0-9.,\\s]+)\\s*FCFA.*?de\\s+([^.]+?)(?:\\.\\s*Votre|\\.\\s*ID|\\.\\s*Merci)",
        Pattern.CASE_INSENSITIVE or Pattern.DOTALL
    )

    // Modèle 2: Transfert émis (Dépense)
    // "vous avez transfere 1,780.00 FCFA ... au numero 54766084,Nonganaba Alexandre"
    private val REGEX_TRANSFERT_EMIS = Pattern.compile(
        "vous\\s+avez\\s+(?:transfere|transféré)\\s+([0-9.,\\s]+)\\s*FCFA.*?au\\s+(?:numero|numéro)\\s+([^.]+?)(?:\\.\\s*Votre|\\.\\s*ID|\\.\\s*Merci)",
        Pattern.CASE_INSENSITIVE or Pattern.DOTALL
    )

    // Modèle 3: Paiement effectué
    // "Votre paiement de 500.00 FCFA ... a Bebe jean-Baptiste Bougrouma a ete effectue"
    private val REGEX_PAIEMENT = Pattern.compile(
        "Votre\\s+paiement\\s+de\\s+([0-9.,\\s]+)\\s*FCFA.*?(?:a|à)\\s+([^.]+?)\\s+a\\s+ete\\s+effectue",
        Pattern.CASE_INSENSITIVE or Pattern.DOTALL
    )

    // Extraction du solde disponible
    private val REGEX_SOLDE = Pattern.compile(
        "solde\\s+est\\s+de\\s*:?\\s*([0-9.,\\s]+)\\s*FCFA",
        Pattern.CASE_INSENSITIVE
    )

    // Extraction des frais
    private val REGEX_FRAIS = Pattern.compile(
        "Frais\\s*:\\s*([0-9.,]+)\\s*FCFA",
        Pattern.CASE_INSENSITIVE
    )

    // Extraction des taxes
    private val REGEX_TAXE = Pattern.compile(
        "Taxe\\s*:\\s*([0-9.,]+)\\s*FCFA",
        Pattern.CASE_INSENSITIVE
    )

    fun parse(smsBody: String, timestamp: Long = System.currentTimeMillis()): ParsedSmsResult {
        val cleanBody = smsBody.trim().replace("\r", " ")

        // 1. Extraire l'identifiant de transaction
        val transMatcher = REGEX_TRANSACTION_ID.matcher(cleanBody)
        val transId = if (transMatcher.find()) {
            transMatcher.group(1)?.trim()?.trimEnd('.') ?: ""
        } else {
            ""
        }

        // 2. Extraire les éléments auxiliaires (Solde, Frais, Taxe)
        val solde = extractNumber(REGEX_SOLDE, cleanBody)
        val frais = extractNumber(REGEX_FRAIS, cleanBody) ?: 0.0
        val taxe = extractNumber(REGEX_TAXE, cleanBody) ?: 0.0

        // 3. Tester modèle 1 : Recette reçue
        val recetteMatcher = REGEX_RECETTE.matcher(cleanBody)
        if (recetteMatcher.find()) {
            val montant = parseAmountString(recetteMatcher.group(1))
            val expediteur = recetteMatcher.group(2)?.trim() ?: "Orange Money Inconnu"
            val finalTransId = transId.ifEmpty { "REC_${System.currentTimeMillis()}" }

            return ParsedSmsResult(
                isValid = true,
                type = OrangeMoneySmsType.RECETTE_DEPOT_TRANSFERT,
                transactionId = finalTransId,
                amount = montant,
                party = expediteur,
                fees = frais,
                taxes = taxe,
                balanceAfter = solde,
                rawBody = cleanBody,
                timestamp = timestamp
            )
        }

        // 4. Tester modèle 2 : Transfert émis
        val transfertMatcher = REGEX_TRANSFERT_EMIS.matcher(cleanBody)
        if (transfertMatcher.find()) {
            val montant = parseAmountString(transfertMatcher.group(1))
            val destinataire = transfertMatcher.group(2)?.trim() ?: "Bénéficiaire Inconnu"
            val finalTransId = transId.ifEmpty { "TR_${System.currentTimeMillis()}" }

            return ParsedSmsResult(
                isValid = true,
                type = OrangeMoneySmsType.DEPENSE_TRANSFERT_EMIS,
                transactionId = finalTransId,
                amount = montant,
                party = destinataire,
                fees = frais,
                taxes = taxe,
                balanceAfter = solde,
                rawBody = cleanBody,
                timestamp = timestamp
            )
        }

        // 5. Tester modèle 3 : Paiement effectué
        val paiementMatcher = REGEX_PAIEMENT.matcher(cleanBody)
        if (paiementMatcher.find()) {
            val montant = parseAmountString(paiementMatcher.group(1))
            val marchand = paiementMatcher.group(2)?.trim() ?: "Marchand Inconnu"
            val finalTransId = transId.ifEmpty { "PM_${System.currentTimeMillis()}" }

            return ParsedSmsResult(
                isValid = true,
                type = OrangeMoneySmsType.DEPENSE_PAIEMENT,
                transactionId = finalTransId,
                amount = montant,
                party = marchand,
                fees = frais,
                taxes = taxe,
                balanceAfter = solde,
                rawBody = cleanBody,
                timestamp = timestamp
            )
        }

        // Si aucun modèle strict ne correspond, tenter extraction générique si présence de Orange Money ou FCFA + ID Trans
        if (transId.isNotEmpty() && cleanBody.contains("FCFA", ignoreCase = true)) {
            val genericAmount = extractGenericAmount(cleanBody)
            if (genericAmount > 0.0) {
                return ParsedSmsResult(
                    isValid = true,
                    type = if (cleanBody.contains("recu", ignoreCase = true) || cleanBody.contains("reçu", ignoreCase = true))
                        OrangeMoneySmsType.RECETTE_DEPOT_TRANSFERT else OrangeMoneySmsType.INCONNU,
                    transactionId = transId,
                    amount = genericAmount,
                    party = "Orange Money",
                    fees = frais,
                    taxes = taxe,
                    balanceAfter = solde,
                    rawBody = cleanBody,
                    timestamp = timestamp
                )
            }
        }

        return ParsedSmsResult(
            isValid = false,
            type = OrangeMoneySmsType.INCONNU,
            transactionId = transId,
            amount = 0.0,
            party = "",
            fees = 0.0,
            taxes = 0.0,
            balanceAfter = null,
            rawBody = cleanBody,
            timestamp = timestamp
        )
    }

    private fun extractNumber(pattern: Pattern, text: String): Double? {
        val matcher = pattern.matcher(text)
        return if (matcher.find()) {
            parseAmountString(matcher.group(1))
        } else null
    }

    private fun extractGenericAmount(text: String): Double {
        val pattern = Pattern.compile("([0-9.,\\s]+)\\s*FCFA", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        return if (matcher.find()) {
            parseAmountString(matcher.group(1))
        } else 0.0
    }

    fun parseAmountString(raw: String?): Double {
        if (raw == null) return 0.0
        // Enlève espaces, remplace virgules par point si c'est séparateur décimal, ou enlève virgule de milliers
        val clean = raw.trim().replace(" ", "").replace("\u00A0", "")
        return try {
            if (clean.contains(",") && clean.contains(".")) {
                // Format américain 1,780.00 -> 1780.00
                clean.replace(",", "").toDouble()
            } else if (clean.contains(",") && !clean.contains(".")) {
                // Format français 1780,00 -> 1780.00
                clean.replace(",", ".").toDouble()
            } else {
                clean.toDouble()
            }
        } catch (e: Exception) {
            0.0
        }
    }
}
