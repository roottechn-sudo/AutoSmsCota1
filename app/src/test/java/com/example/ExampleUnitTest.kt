package com.example

import com.example.data.model.OrangeMoneySmsType
import com.example.data.parser.OrangeMoneyParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrangeMoneyParserTest {

    @Test
    fun testModele1_RecetteDepotTransfertRecu() {
        val sms = "Cher client, vous avez recu 10000.0 FCFA, Frais: FCFA, Taxe: FCFA de 3177363 KIENON MULTI SERVICES. Votre solde est de 15106.698 FCFA. ID Trans: CI261031.1040.8675069. Merci d'utiliser Orange Money."
        val result = OrangeMoneyParser.parse(sms)

        assertTrue("Le SMS doit être valide", result.isValid)
        assertEquals(OrangeMoneySmsType.RECETTE_DEPOT_TRANSFERT, result.type)
        assertEquals(10000.0, result.amount, 0.01)
        assertEquals("CI261031.1040.8675069", result.transactionId)
        assertTrue("L'expéditeur doit être extrait", result.party.contains("3177363 KIENON MULTI SERVICES"))
        assertEquals(15106.698, result.balanceAfter ?: 0.0, 0.001)
    }

    @Test
    fun testModele2_TransfertEmis() {
        val sms = "Cher client, vous avez transfere 1,780.00 FCFA, Frais: 0.0 FCFA, Taxe: FCFA au numero 54766084,Nonganaba Alexandre. Votre solde est de 5611.698 FCFA. ID Trans: PP261031.1017.2901274..."
        val result = OrangeMoneyParser.parse(sms)

        assertTrue(result.isValid)
        assertEquals(OrangeMoneySmsType.DEPENSE_TRANSFERT_EMIS, result.type)
        assertEquals(1780.00, result.amount, 0.01)
        assertEquals("PP261031.1017.2901274", result.transactionId)
        assertTrue(result.party.contains("54766084,Nonganaba Alexandre"))
    }

    @Test
    fun testModele3_PaiementEffectue() {
        val sms = "Votre paiement de 500.00 FCFA, Frais: 4.3478 FCFA, Taxe: 0.6522 FCFA a Bebe jean-Baptiste Bougrouma a ete effectue avec succes. Votre solde est de : 5106.698 FCFA. Trans id: MP261031.1009.97358916."
        val result = OrangeMoneyParser.parse(sms)

        assertTrue(result.isValid)
        assertEquals(OrangeMoneySmsType.DEPENSE_PAIEMENT, result.type)
        assertEquals(500.00, result.amount, 0.01)
        assertEquals("MP261031.1009.97358916", result.transactionId)
        assertTrue(result.party.contains("Bebe jean-Baptiste Bougrouma"))
        assertEquals(4.3478, result.fees, 0.0001)
        assertEquals(0.6522, result.taxes, 0.0001)
    }
}
