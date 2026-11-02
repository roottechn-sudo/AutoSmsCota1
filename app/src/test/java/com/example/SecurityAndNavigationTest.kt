package com.example

import com.example.data.model.CotisationEntity
import com.example.data.model.MemberEntity
import com.example.data.model.StatutCotisation
import com.example.data.model.UserRole
import com.example.security.DroitsService
import com.example.ui.navigation.AppRoute
import com.example.ui.navigation.NavigationResult
import com.example.ui.navigation.RouteGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityAndNavigationTest {

    private fun createMember(role: UserRole): MemberEntity {
        return MemberEntity(
            uid = "test_${role.name.lowercase()}",
            nom = "Test",
            prenom = "User",
            telephone = "+22670000000",
            role = role,
            carteValide = true,
            soldeArrieres = 0.0
        )
    }

    @Test
    fun testCotisationEntityStatusAndSecurityDefaults() {
        val pendingCotisation = CotisationEntity(
            idTransaction = "TX_TEST_001",
            idCampagne = "CAMP_TEST",
            membreUid = "mbr_001",
            montant = 15000.0,
            statut = StatutCotisation.EN_ATTENTE.code
        )

        assertTrue("La transaction doit être en attente", pendingCotisation.isEnAttente)
        assertFalse("La transaction ne doit pas être validée", pendingCotisation.isValidee)
        assertEquals(StatutCotisation.EN_ATTENTE, pendingCotisation.statutEnum)
        assertEquals("TX_TEST_001", pendingCotisation.id)
    }

    @Test
    fun testDroitsServiceRoleRestrictions() {
        // Membre simple
        assertFalse(DroitsService.canRecordCotisation(UserRole.MEMBRE))
        assertFalse(DroitsService.canScanSms(UserRole.MEMBRE))
        assertFalse(DroitsService.canViewFinancialReports(UserRole.MEMBRE))
        assertFalse(DroitsService.canCreateCampaign(UserRole.MEMBRE))

        // Trésorier
        assertTrue(DroitsService.canRecordCotisation(UserRole.TRESORIER))
        assertTrue(DroitsService.canScanSms(UserRole.TRESORIER))
        assertTrue(DroitsService.canViewFinancialReports(UserRole.TRESORIER))

        // Secrétaire
        assertTrue(DroitsService.canCreateCampaign(UserRole.SECRETAIRE))
        assertFalse(DroitsService.canScanSms(UserRole.SECRETAIRE))

        // Administrateur (Privilèges complets)
        assertTrue(DroitsService.canRecordCotisation(UserRole.ADMINISTRATEUR))
        assertTrue(DroitsService.canScanSms(UserRole.ADMINISTRATEUR))
        assertTrue(DroitsService.canViewFinancialReports(UserRole.ADMINISTRATEUR))
        assertTrue(DroitsService.canCreateCampaign(UserRole.ADMINISTRATEUR))
    }

    @Test
    fun testRouteGuardInterception_MembreDeniedFromAnalytics() {
        val member = createMember(UserRole.MEMBRE)
        val result = RouteGuard.evaluateNavigation(AppRoute.Analytics, member)

        assertTrue("L'accès à Analytics doit être refusé pour un membre", result is NavigationResult.Denied)
        val denied = result as NavigationResult.Denied
        assertEquals("Trésorier ou Administrateur", denied.requiredRole)
    }

    @Test
    fun testRouteGuardInterception_TresorierAllowedForAnalytics() {
        val tresorier = createMember(UserRole.TRESORIER)
        val result = RouteGuard.evaluateNavigation(AppRoute.Analytics, tresorier)

        assertTrue("L'accès à Analytics doit être autorisé pour le trésorier", result is NavigationResult.Allowed)
    }

    @Test
    fun testRouteGuardInterception_PublicRoutesAllowedForAll() {
        val member = createMember(UserRole.MEMBRE)

        assertTrue(RouteGuard.evaluateNavigation(AppRoute.Dashboard, member) is NavigationResult.Allowed)
        assertTrue(RouteGuard.evaluateNavigation(AppRoute.Cotisations, member) is NavigationResult.Allowed)
        assertTrue(RouteGuard.evaluateNavigation(AppRoute.Campaigns, member) is NavigationResult.Allowed)
        assertTrue(RouteGuard.evaluateNavigation(AppRoute.Members, member) is NavigationResult.Allowed)
    }
}
