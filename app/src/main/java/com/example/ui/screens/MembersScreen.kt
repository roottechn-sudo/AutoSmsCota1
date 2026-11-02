package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MemberEntity
import com.example.data.model.UserRole
import com.example.security.DroitsService
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.OrangeMoneyAccent
import com.example.ui.theme.Terracotta
import com.example.ui.theme.TerracottaContainer
import com.example.ui.viewmodel.MainViewModel

@Composable
fun MembersScreen(
    viewModel: MainViewModel,
    onOpenAddMemberDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val members by viewModel.members.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf("ALL") }

    val currentRole = currentUser?.role ?: UserRole.MEMBRE
    val canManage = DroitsService.canManageMembers(currentRole)

    val filteredMembers = members.filter { member ->
        val matchesCategory = when (categoryFilter) {
            "ALL" -> true
            "ARREARS" -> member.soldeArrieres > 0
            "PENDING" -> !member.carteValide
            else -> member.categorie == categoryFilter
        }

        val matchesSearch = searchQuery.isBlank() ||
                member.nomComplet.contains(searchQuery, ignoreCase = true) ||
                member.telephone.contains(searchQuery, ignoreCase = true) ||
                (member.email?.contains(searchQuery, ignoreCase = true) == true) ||
                member.categorie.contains(searchQuery, ignoreCase = true)

        matchesCategory && matchesSearch
    }

    Box(modifier = modifier.fillMaxSize().testTag("members_screen")) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Gestion des Membres RAVA",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${members.size} adhérents enregistrés • Catégories, coordonnées et arriérés",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Recherche
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_members_input"),
                    placeholder = { Text("Rechercher par nom, téléphone, catégorie...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Effacer")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Filtres de Catégories
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = categoryFilter == "ALL",
                        onClick = { categoryFilter = "ALL" },
                        label = { Text("Tous (${members.size})") }
                    )
                    FilterChip(
                        selected = categoryFilter == "ARREARS",
                        onClick = { categoryFilter = "ARREARS" },
                        label = { Text("Arriérés (${members.count { it.soldeArrieres > 0 }})") }
                    )
                    FilterChip(
                        selected = categoryFilter == "PENDING",
                        onClick = { categoryFilter = "PENDING" },
                        label = { Text("En attente (${members.count { !it.carteValide }})") }
                    )
                }
            }

            if (filteredMembers.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Aucun membre trouvé",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Essayez un autre mot-clé ou modifiez les filtres de recherche.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredMembers) { member ->
                    MemberCardItemEnriched(
                        member = member,
                        isCurrentUser = member.uid == currentUser?.uid,
                        canManage = canManage,
                        formattedArrears = viewModel.formatFcfa(member.soldeArrieres),
                        onClick = {
                            viewModel.selectMemberForDetail(member)
                        },
                        onRoleChange = { newRole ->
                            viewModel.updateMemberRole(member.uid, newRole)
                        },
                        onToggleCardStatus = {
                            viewModel.toggleCardValidation(member.uid, member.carteValide)
                        },
                        onSecurityBlocked = {
                            viewModel.showSecurityBlock(
                                DroitsService.getDeniedMessage(currentRole, "modifier les droits d'un membre")
                            )
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // FAB Ajouter un membre
        FloatingActionButton(
            onClick = {
                if (canManage) {
                    onOpenAddMemberDialog()
                } else {
                    viewModel.showSecurityBlock(
                        DroitsService.getDeniedMessage(currentRole, "ajouter un membre")
                    )
                }
            },
            containerColor = if (canManage) EmeraldPrimary else Color.Gray,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_member_fab")
        ) {
            Icon(
                imageVector = if (canManage) Icons.Default.PersonAdd else Icons.Default.Lock,
                contentDescription = "Ajouter un membre"
            )
        }
    }
}

@Composable
fun MemberCardItemEnriched(
    member: MemberEntity,
    isCurrentUser: Boolean,
    canManage: Boolean,
    formattedArrears: String,
    onClick: () -> Unit,
    onRoleChange: (UserRole) -> Unit,
    onToggleCardStatus: () -> Unit,
    onSecurityBlocked: () -> Unit
) {
    var showRoleMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("member_card_${member.uid}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(member.role.badgeColorHex)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = member.nom.take(1),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = member.nomComplet,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            if (isCurrentUser) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = EmeraldContainer
                                ) {
                                    Text(
                                        text = "Vous",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = EmeraldPrimary,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${member.telephone} • ${member.categorie}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Menu de rôle
                Box {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(member.role.badgeColorHex).copy(alpha = 0.15f),
                        modifier = Modifier.clickable {
                            if (canManage) showRoleMenu = true else onSecurityBlocked()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = member.role.labelFr,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(member.role.badgeColorHex),
                                    fontSize = 11.sp
                                )
                            )
                            if (canManage) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Modifier rôle",
                                    tint = Color(member.role.badgeColorHex),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = showRoleMenu,
                        onDismissRequest = { showRoleMenu = false }
                    ) {
                        UserRole.entries.forEach { role ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color(role.badgeColorHex))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(role.labelFr)
                                        if (role == member.role) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = EmeraldPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onRoleChange(role)
                                    showRoleMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Statut de carte & Solde d'arriérés
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (member.carteValide) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (member.carteValide) EmeraldPrimary else Terracotta,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (member.carteValide) "Carte Adhérent Active" else "Compte en attente",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (member.carteValide) EmeraldPrimary else Terracotta,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    )
                }

                if (canManage) {
                    Switch(
                        checked = member.carteValide,
                        onCheckedChange = { onToggleCardStatus() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = EmeraldPrimary,
                            checkedTrackColor = EmeraldContainer
                        ),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            if (member.soldeArrieres > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = TerracottaContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Arriérés (${member.joursRetardEstimes}j de retard) :",
                            style = MaterialTheme.typography.labelSmall,
                            color = Terracotta
                        )
                        Text(
                            text = formattedArrears,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Terracotta
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Voir profil & historique complet →",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}
