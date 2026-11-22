/*
 * Zynx Launcher - mod & modpack hub
 * Based on Zalith Launcher 2 (GPL-3.0)
 */

package com.movtery.zalithlauncher.ui.screens.content

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel

@Composable
fun ZynxModHubScreen(
    backScreenViewModel: ScreenBackStackViewModel
) {
    BaseScreen(
        screenKey = NormalNavKey.ZynxModHub,
        currentKey = backScreenViewModel.mainScreen.currentKey
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.zynx_mod_hub_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.zynx_mod_hub_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val go: (TitledNavKey) -> Unit = { target ->
                backScreenViewModel.navigateToDownload(target)
            }

            HubCard(
                icon = R.drawable.ic_package_2_outlined,
                title = R.string.download_category_modpack,
                desc = R.string.zynx_hub_modpack_desc,
                onClick = { go(backScreenViewModel.downloadModPackScreen) }
            )
            HubCard(
                icon = R.drawable.ic_extension_outlined,
                title = R.string.download_category_mod,
                desc = R.string.zynx_hub_mod_desc,
                onClick = { go(backScreenViewModel.downloadModScreen) }
            )
            HubCard(
                icon = R.drawable.ic_favorite_outlined,
                title = R.string.download_category_favorites,
                desc = R.string.zynx_hub_fav_desc,
                onClick = { go(backScreenViewModel.downloadFavoritesScreen) }
            )
            HubCard(
                icon = R.drawable.ic_card,
                title = R.string.download_category_by_id,
                desc = R.string.zynx_hub_id_desc,
                onClick = { go(NormalNavKey.SearchId) }
            )
        }
    }
}

@Composable
private fun HubCard(
    @DrawableRes icon: Int,
    @StringRes title: Int,
    @StringRes desc: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                modifier = Modifier.size(32.dp),
                painter = painterResource(icon),
                contentDescription = null
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(desc),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
