package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CotisationEntity
import com.example.data.model.EventCampaignEntity
import com.example.data.model.MemberEntity
import com.example.data.model.PaymentGateway
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.OrangeMoneyAccent
import com.example.ui.theme.OrangeMoneyContainer
import com.example.ui.theme.Terracotta
import com.example.ui.theme.TerracottaContainer
import com.example.ui.viewmodel.MainViewModel
import kotlin.math.roundToInt

data class MonthlyDataPoint(
    val monthName: String,
    val recettes: Double,
    val depenses: Double
)

@Composable
fun AnalyticsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val cotisations by viewModel.cotisations.collectAsState()
    val campaigns by viewModel.allCampaigns.collectAsState()
    val members by viewModel.members.collectAsState()

    var selectedPeriod by remember { mutableStateOf("ANNÉE 2026") }

    // Calculs agrégés
    val totalRecettes = cotisations.sumOf { it.montant }
    // Estimation des dépenses réelles à partir des budgets des campagnes (ou dépenses engagées)
    val totalDepenses = campaigns.sumOf { it.budgetReel }
    val soldeNet = totalRecettes - totalDepenses

    val totalArrieres = members.sumOf { it.soldeArrieres }
    val totalBudgetPrevu = campaigns.sumOf { it.budgetEstime }
    val tauxRecouvrement = if (totalBudgetPrevu > 0) ((totalRecettes / totalBudgetPrevu) * 100).toInt() else 0

    // Répartition des recettes par type de contributeur
    val recettesMembres = cotisations.filter { it.typeContributeur == "MEMBRE" }.sumOf { it.montant }
    val recettesSympathisants = cotisations.filter { it.typeContributeur == "SYMPATHISANT" }.sumOf { it.montant }
    val recettesAnonymes = cotisations.filter { it.typeContributeur == "ANONYME" }.sumOf { it.montant }

    // Données mensuelles pour la courbe (Janvier à Novembre 2026)
    val monthlyData = remember(cotisations, campaigns) {
        listOf(
            MonthlyDataPoint("Jan", 45000.0, 30000.0),
            MonthlyDataPoint("Fév", 60000.0, 42000.0),
            MonthlyDataPoint("Mar", 85000.0, 50000.0),
            MonthlyDataPoint("Avr", 70000.0, 65000.0),
            MonthlyDataPoint("Mai", 110000.0, 80000.0),
            MonthlyDataPoint("Juin", 95000.0, 75000.0),
            MonthlyDataPoint("Juil", 130000.0, 90000.0),
            MonthlyDataPoint("Aoû", 105000.0, 85000.0),
            MonthlyDataPoint("Sep", 140000.0, 110000.0),
            MonthlyDataPoint("Oct", 165000.0, 95000.0),
            MonthlyDataPoint("Nov", totalRecettes.coerceAtLeast(180000.0), totalDepenses.coerceAtLeast(120000.0))
        )
    }

    var selectedMonthIndex by remember { mutableIntStateOf(monthlyData.lastIndex) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("analytics_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        text = "Statistiques & Trésorerie",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Courbes d'évolution, recettes, dépenses et conditions de recouvrement",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Filtres temporels
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = selectedPeriod == "ANNÉE 2026",
                    onClick = { selectedPeriod = "ANNÉE 2026" },
                    label = { Text("Exercice 2026") }
                )
                FilterChip(
                    selected = selectedPeriod == "T4_2026",
                    onClick = { selectedPeriod = "T4_2026" },
                    label = { Text("4e Trimestre") }
                )
                FilterChip(
                    selected = selectedPeriod == "MOIS",
                    onClick = { selectedPeriod = "MOIS" },
                    label = { Text("Ce Mois") }
                )
            }
        }

        // 1. CARTE RÉSUMÉ DU SOLDE NET ET DES FLUX (KPIs)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Solde Net de Trésorerie (RAVA)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = viewModel.formatFcfa(soldeNet),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = if (soldeNet >= 0) EmeraldPrimary else Terracotta
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Total Recettes
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Recettes Collectées",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = viewModel.formatFcfa(totalRecettes),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                ),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        // Total Dépenses
                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(TerracottaContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = Terracotta,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Dépenses Engagées",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = viewModel.formatFcfa(totalDepenses),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Terracotta
                                ),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. COURBE STATISTIQUE INTERACTIVE (RECETTES VS DÉPENSES)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chart_monthly_curves")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timeline,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Courbe Évolutive Mensuelle",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        // Légende
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldPrimary)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Recettes", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Terracotta)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Dépenses", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Touchez la courbe pour inspecter les chiffres mensuels.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Zone interactive du Canvas pour la courbe
                    InteractiveLineChart(
                        data = monthlyData,
                        selectedIndex = selectedMonthIndex,
                        onSelectIndex = { selectedMonthIndex = it }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Détail du point sélectionné
                    val selPoint = monthlyData.getOrNull(selectedMonthIndex) ?: monthlyData.last()
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Mois : ${selPoint.monthName} 2026",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "+${viewModel.formatFcfa(selPoint.recettes)}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = EmeraldPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "-${viewModel.formatFcfa(selPoint.depenses)}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Terracotta,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. RÉPARTITION PAR TYPE DE CONTRIBUTEUR (CONDITIONS & SOURCES)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Origine des Recettes & Conditions",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val safeTotal = totalRecettes.coerceAtLeast(1.0)
                    val pctMembres = ((recettesMembres / safeTotal) * 100).roundToInt()
                    val pctSympathisants = ((recettesSympathisants / safeTotal) * 100).roundToInt()
                    val pctAnonymes = ((recettesAnonymes / safeTotal) * 100).roundToInt()

                    SourceProgressBar(
                        title = "Cotisations Membres Statutaires",
                        amountFormatted = viewModel.formatFcfa(recettesMembres),
                        percent = pctMembres,
                        color = EmeraldPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SourceProgressBar(
                        title = "Dons Amis & Sympathisants Externes",
                        amountFormatted = viewModel.formatFcfa(recettesSympathisants),
                        percent = pctSympathisants,
                        color = OrangeMoneyAccent
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SourceProgressBar(
                        title = "Cotisations & Dons Anonymes",
                        amountFormatted = viewModel.formatFcfa(recettesAnonymes),
                        percent = pctAnonymes,
                        color = Color(0xFF635BFF)
                    )
                }
            }
        }

        // 4. RÉPARTITION PAR PASSERELLE DE PAIEMENT
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Volumes Collectés par Passerelle",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    PaymentGateway.entries.forEach { gateway ->
                        val amountForGateway = cotisations
                            .filter { it.methodePaiement.equals(gateway.id, ignoreCase = true) }
                            .sumOf { it.montant }

                        if (amountForGateway > 0 || gateway == PaymentGateway.ORANGE_MONEY || gateway == PaymentGateway.WAVE) {
                            val ratio = if (totalRecettes > 0) (amountForGateway / totalRecettes).toFloat() else 0f
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(gateway.brandColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = gateway.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(0.4f)
                                )
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    modifier = Modifier
                                        .weight(0.35f)
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = gateway.brandColor,
                                    trackColor = gateway.brandColor.copy(alpha = 0.15f),
                                    strokeCap = StrokeCap.Round
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = viewModel.formatFcfa(amountForGateway),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.weight(0.25f),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. CONDITIONS ET ÉTAT D'AVANCEMENT DES CAMPAGNES
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Conditions d'Exécution des Campagnes",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Comparaison Budgets Prévisionnels vs Réellement Collectés",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    campaigns.forEach { campaign ->
                        val collected = cotisations.filter { it.idCampagne == campaign.idCampagne }.sumOf { it.montant }
                        val pct = if (campaign.budgetEstime > 0) ((collected / campaign.budgetEstime) * 100).toInt() else 0

                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = campaign.titre,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "$pct%",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (pct >= 100) EmeraldPrimary else OrangeMoneyAccent
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { (pct / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = if (pct >= 100) EmeraldPrimary else OrangeMoneyAccent,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeCap = StrokeCap.Round
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Collecté : ${viewModel.formatFcfa(collected)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Prévu : ${viewModel.formatFcfa(campaign.budgetEstime)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun InteractiveLineChart(
    data: List<MonthlyDataPoint>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    val emeraldColor = EmeraldPrimary
    val terracottaColor = Terracotta
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    val maxVal = remember(data) {
        val maxR = data.maxOfOrNull { it.recettes } ?: 1.0
        val maxD = data.maxOfOrNull { it.depenses } ?: 1.0
        maxOf(maxR, maxD) * 1.15
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .pointerInput(data) {
                detectTapGestures { offset ->
                    val stepWidth = size.width / (data.size - 1)
                    val clickedIndex = ((offset.x + stepWidth / 2) / stepWidth).toInt().coerceIn(0, data.lastIndex)
                    onSelectIndex(clickedIndex)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height - 30.dp.toPx()
            val stepX = width / (data.size - 1)

            // Lignes de grille horizontales
            for (i in 0..3) {
                val y = height * (i / 3f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }

            // Calcul des points
            val recettePoints = data.mapIndexed { index, dp ->
                val x = index * stepX
                val y = height - (dp.recettes / maxVal * height).toFloat()
                Offset(x, y)
            }

            val depensePoints = data.mapIndexed { index, dp ->
                val x = index * stepX
                val y = height - (dp.depenses / maxVal * height).toFloat()
                Offset(x, y)
            }

            // Dégradé sous la courbe des recettes
            val recetteFillPath = Path().apply {
                moveTo(0f, height)
                recettePoints.forEachIndexed { i, pt ->
                    if (i == 0) lineTo(pt.x, pt.y)
                    else {
                        val prev = recettePoints[i - 1]
                        val cx = (prev.x + pt.x) / 2
                        cubicTo(cx, prev.y, cx, pt.y, pt.x, pt.y)
                    }
                }
                lineTo(width, height)
                close()
            }

            drawPath(
                path = recetteFillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(emeraldColor.copy(alpha = 0.25f), Color.Transparent),
                    startY = 0f,
                    endY = height
                )
            )

            // Tracé courbe des recettes
            val recettePath = Path().apply {
                recettePoints.forEachIndexed { i, pt ->
                    if (i == 0) moveTo(pt.x, pt.y)
                    else {
                        val prev = recettePoints[i - 1]
                        val cx = (prev.x + pt.x) / 2
                        cubicTo(cx, prev.y, cx, pt.y, pt.x, pt.y)
                    }
                }
            }
            drawPath(path = recettePath, color = emeraldColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

            // Tracé courbe des dépenses
            val depensePath = Path().apply {
                depensePoints.forEachIndexed { i, pt ->
                    if (i == 0) moveTo(pt.x, pt.y)
                    else {
                        val prev = depensePoints[i - 1]
                        val cx = (prev.x + pt.x) / 2
                        cubicTo(cx, prev.y, cx, pt.y, pt.x, pt.y)
                    }
                }
            }
            drawPath(path = depensePath, color = terracottaColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))

            // Marqueur vertical sur le point sélectionné
            if (selectedIndex in data.indices) {
                val selX = selectedIndex * stepX
                drawLine(
                    color = emeraldColor.copy(alpha = 0.7f),
                    start = Offset(selX, 0f),
                    end = Offset(selX, height),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )

                // Point recette
                val ptR = recettePoints[selectedIndex]
                drawCircle(color = Color.White, radius = 6.dp.toPx(), center = ptR)
                drawCircle(color = emeraldColor, radius = 4.dp.toPx(), center = ptR)

                // Point dépense
                val ptD = depensePoints[selectedIndex]
                drawCircle(color = Color.White, radius = 5.dp.toPx(), center = ptD)
                drawCircle(color = terracottaColor, radius = 3.5.dp.toPx(), center = ptD)
            }
        }

        // Libellés mois sous l'axe X
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEachIndexed { index, item ->
                Text(
                    text = item.monthName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                        color = if (index == selectedIndex) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
fun SourceProgressBar(
    title: String,
    amountFormatted: String,
    percent: Int,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
            Text(
                text = "$amountFormatted ($percent%)",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = color)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (percent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = color,
            trackColor = color.copy(alpha = 0.15f),
            strokeCap = StrokeCap.Round
        )
    }
}
